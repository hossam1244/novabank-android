package com.novabank.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class TokenPair(
    @SerialName("access") val access: String,
    @SerialName("refresh") val refresh: String? = null,
)

@Serializable
data class RefreshRequest(val refresh: String)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
) {
    val displayName: String get() = "$firstName $lastName".trim().ifEmpty { email }
}

@Serializable
data class MembershipDto(
    val organization: OrganizationDto,
    val role: String,
)

@Serializable
data class OrganizationDto(val id: String, val name: String)

@Serializable
data class MeResponse(val user: UserDto, val memberships: List<MembershipDto> = emptyList())

@Serializable
data class BankAccountDto(
    val id: String,
    val name: String,
    val iban: String,
    val currency: String,
    val balance: String,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class Paginated<T>(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList(),
)

@Serializable
data class PaymentDto(
    val id: String,
    val kind: String = "payment",
    val amount: String,
    val currency: String,
    @SerialName("beneficiary_name") val beneficiaryName: String = "",
    val reference: String = "",
    val status: String,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CreatePaymentRequest(
    @SerialName("source_account") val sourceAccountId: String,
    val amount: String,
    @SerialName("beneficiary_name") val beneficiaryName: String,
    @SerialName("beneficiary_iban") val beneficiaryIban: String,
    val reference: String = "",
)

@Serializable
data class CardDto(
    val id: String,
    @SerialName("last4") val last4: String,
    @SerialName("expiry_month") val expiryMonth: Int,
    @SerialName("expiry_year") val expiryYear: Int,
    val status: String,
    @SerialName("account_iban") val accountIban: String = "",
)

/** The API's uniform error envelope. */
@Serializable
data class ApiErrorEnvelope(val error: ApiErrorBody)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

class ApiException(
    val statusCode: Int,
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap(),
) : Exception(message)
