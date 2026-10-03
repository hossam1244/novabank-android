package com.novabank.app.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface NovaBankApi {

    @POST("api/v1/auth/token/")
    suspend fun login(@Body body: LoginRequest): TokenPair

    @GET("api/v1/me/")
    suspend fun me(): MeResponse

    @GET("api/v1/orgs/{orgId}/accounts/")
    suspend fun accounts(@Path("orgId") orgId: String): Paginated<BankAccountDto>

    @GET("api/v1/orgs/{orgId}/payments/")
    suspend fun payments(@Path("orgId") orgId: String): Paginated<PaymentDto>

    /** The Idempotency-Key header makes transport retries safe. */
    @POST("api/v1/orgs/{orgId}/payments/")
    suspend fun createPayment(
        @Path("orgId") orgId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePaymentRequest,
    ): PaymentDto

    @GET("api/v1/orgs/{orgId}/cards/")
    suspend fun cards(@Path("orgId") orgId: String): Paginated<CardDto>
}
