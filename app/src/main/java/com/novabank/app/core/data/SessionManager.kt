package com.novabank.app.core.data

import com.novabank.app.core.network.BankAccountDto
import com.novabank.app.core.network.CardDto
import com.novabank.app.core.network.CreatePaymentRequest
import com.novabank.app.core.network.LoginRequest
import com.novabank.app.core.network.MeResponse
import com.novabank.app.core.network.NovaBankApi
import com.novabank.app.core.network.PaymentDto
import com.novabank.app.core.storage.TokenStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionEvent {
    data object LoggedOut : SessionEvent
}

/** Session + org context, plus the API calls the UI drives. */
@Singleton
class BankingRepository @Inject constructor(
    private val api: NovaBankApi,
    private val tokenStore: TokenStore,
) {

    var session: MeResponse? = null
        private set

    val orgId: String?
        get() = session?.memberships?.firstOrNull()?.organization?.id

    suspend fun login(email: String, password: String): MeResponse {
        val tokens = api.login(LoginRequest(email, password))
        tokenStore.save(tokens.access, tokens.refresh)
        return me()
    }

    suspend fun me(): MeResponse {
        val me = api.me()
        session = me
        return me
    }

    fun logout() {
        session = null
        tokenStore.clear()
    }

    suspend fun accounts(): List<BankAccountDto> =
        api.accounts(requireOrg()).results

    suspend fun payments(): List<PaymentDto> = api.payments(requireOrg()).results

    /**
     * One idempotency key per submission attempt: transport-level retries
     * replay the same payment server-side; a new submit gets a new key.
     */
    suspend fun pay(request: CreatePaymentRequest): PaymentDto =
        api.createPayment(requireOrg(), UUID.randomUUID().toString(), request)

    suspend fun cards(): List<CardDto> = api.cards(requireOrg()).results

    private fun requireOrg(): String =
        orgId ?: error("No organization in session — call login/me first")
}
