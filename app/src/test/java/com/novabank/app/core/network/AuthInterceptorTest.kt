package com.novabank.app.core.network

import com.novabank.app.core.FakeTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeTokenStore
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokens = FakeTokenStore()
        tokens.save("stale-access", "valid-refresh")
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokens, server.url("/").toString()))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `attaches the bearer token`() {
        server.enqueue(MockResponse().setBody("{}"))

        client.newCall(Request.Builder().url(server.url("/api/v1/me/")).build())
            .execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer stale-access", recorded.getHeader("Authorization"))
    }

    @Test
    fun `401 refreshes the token and replays the request`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{}}"""))
        server.enqueue(
            MockResponse().setBody("""{"access":"new-access","refresh":"new-refresh"}""")
        )
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        val response = client
            .newCall(Request.Builder().url(server.url("/api/v1/accounts/")).build())
            .execute()

        assertEquals(200, response.code)
        assertEquals("new-access", tokens.accessToken())
        assertEquals("new-refresh", tokens.refreshToken())

        // 401'd call → refresh POST → replay with the fresh token.
        server.takeRequest()
        server.takeRequest()
        val replayed = server.takeRequest()
        assertEquals("Bearer new-access", replayed.getHeader("Authorization"))
        assertEquals("/api/v1/accounts/", replayed.path)
    }

    @Test
    fun `failed refresh clears the session`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val response = client
            .newCall(Request.Builder().url(server.url("/api/v1/accounts/")).build())
            .execute()

        assertEquals(401, response.code)
        assertNull(tokens.accessToken())
        assertNull(tokens.refreshToken())
    }

    @Test
    fun `login endpoint 401 never triggers a refresh`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        client.newCall(Request.Builder().url(server.url("/api/v1/auth/token/")).build())
            .execute()

        assertEquals(1, server.requestCount) // no refresh POST followed
    }

    @Test
    fun `retried request does not loop on second 401`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setBody("""{"access":"new-access"}"""))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val response = client
            .newCall(Request.Builder().url(server.url("/api/v1/accounts/")).build())
            .execute()

        assertEquals(401, response.code)
        assertEquals(3, server.requestCount) // original + refresh + one replay, then stop
    }
}
