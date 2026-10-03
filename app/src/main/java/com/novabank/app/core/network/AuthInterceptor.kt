package com.novabank.app.core.network

import com.novabank.app.core.storage.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Attaches the Bearer token and performs a single, synchronized refresh on 401.
 *
 * Rotating refresh tokens mean a second concurrent refresh would fail and
 * kill the session — so all 401s wait on one lock while the first thread
 * performs the refresh, then replay their requests with the new token.
 */
class AuthInterceptor(
    private val tokenStore: TokenStore,
    private val baseUrl: String,
) : Interceptor {

    private val refreshLock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().withBearer()
        val response = chain.proceed(request)

        if (response.code != 401 || request.isAuthEndpoint()) {
            return response
        }
        if (request.tag(RETRIED::class.java) != null) {
            return response // one retry per request — no loops
        }

        val refreshed = refreshLock.withLock {
            // Another thread may have refreshed while we waited for the lock.
            val current = tokenStore.accessToken()
            if (current != null && current != request.header("Authorization")?.removePrefix("Bearer ")) {
                true
            } else {
                performRefresh()
            }
        }
        if (!refreshed) {
            tokenStore.clear()
            return response
        }

        response.close()
        val retried = chain.call().request().newBuilder()
            .tag(RETRIED::class.java, RETRIED)
            .build()
            .withBearer()
        return chain.proceed(retried)
    }

    private fun Request.withBearer(): Request {
        val token = tokenStore.accessToken() ?: return this
        return newBuilder().header("Authorization", "Bearer $token").build()
    }

    private fun Request.isAuthEndpoint(): Boolean = url.encodedPath.contains("/auth/token")

    /** POSTs the refresh endpoint on a bare client (no interceptor loop). */
    private fun performRefresh(): Boolean = runBlocking {
        val refresh = tokenStore.refreshToken() ?: return@runBlocking false
        val body = Json.encodeToString(RefreshPayload.serializer(), RefreshPayload(refresh))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/auth/token/refresh/")
            .post(body)
            .build()
        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runBlocking false
                val parsed = Json.decodeFromString(RefreshResponse.serializer(), response.body?.string().orEmpty())
                if (parsed.access.isEmpty()) return@runBlocking false
                tokenStore.save(parsed.access, parsed.refresh ?: refresh)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    @Serializable
    private data class RefreshPayload(val refresh: String)

    @Serializable
    private data class RefreshResponse(val access: String = "", val refresh: String? = null)

    private object RETRIED
}
