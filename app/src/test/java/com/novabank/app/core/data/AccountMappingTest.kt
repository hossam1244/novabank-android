package com.novabank.app.core.data

import com.novabank.app.core.network.BankAccountDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountMappingTest {

    @Test
    fun `dto to cache entity round-trips the fields that matter`() {
        val dto = BankAccountDto(
            id = "a1",
            name = "Main EUR",
            iban = "NV01NVBKTEST",
            currency = "EUR",
            balance = "5000.00",
        )
        val entity = AccountEntity(dto.id, dto.name, dto.iban, dto.currency, dto.balance)
        val restored = BankAccountDto(
            entity.id, entity.name, entity.iban, entity.currency, entity.balance,
        )

        assertEquals(dto, restored)
        assertEquals("5000.00", entity.balance)
    }
}
