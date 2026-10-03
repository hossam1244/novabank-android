package com.novabank.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novabank.app.core.data.BankingRepository
import com.novabank.app.core.network.CreatePaymentRequest
import com.novabank.app.core.network.PaymentDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repository: BankingRepository,
) : ViewModel() {

    private val _payments = MutableStateFlow<List<PaymentDto>>(emptyList())
    val payments: StateFlow<List<PaymentDto>> = _payments

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { repository.payments() }.onSuccess { _payments.value = it }
        }
    }

    /** Pays from the first active account — the demo's single-funded account. */
    fun pay(amount: String, beneficiary: String, iban: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val source = repository.accounts().firstOrNull { it.isActive }
                    ?: error("No active account to pay from")
                val payment = repository.pay(
                    CreatePaymentRequest(
                        sourceAccountId = source.id,
                        amount = amount,
                        beneficiaryName = beneficiary,
                        beneficiaryIban = iban,
                    ),
                )
                onDone("Payment ${payment.status}")
                load()
            } catch (error: Exception) {
                onDone(error.message ?: "Payment failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel = hiltViewModel()) {
    val payments by viewModel.payments.collectAsState()
    var sheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { sheet = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("New payment") }
        LazyColumn {
            items(payments, key = { it.id }) { payment ->
                PaymentRow(payment)
            }
        }
    }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            NewPaymentSheet(
                onSubmit = { amount, beneficiary, iban, onDone ->
                    viewModel.pay(amount, beneficiary, iban, onDone)
                },
                onDismiss = { sheet = false },
            )
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentDto) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (payment.status == "settled") {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Schedule
                },
                contentDescription = payment.status,
                tint = when (payment.status) {
                    "settled" -> Color(0xFF2E7D32)
                    "failed" -> MaterialTheme.colorScheme.error
                    else -> Color(0xFFEF6C00)
                },
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(payment.beneficiaryName.ifEmpty { "Deposit" })
                Text(
                    payment.reference.ifEmpty { payment.createdAt },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text("${payment.amount} ${payment.currency}")
    }
}

@Composable
private fun NewPaymentSheet(
    onSubmit: (String, String, String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var beneficiary by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.padding(20.dp)) {
        Text("New payment", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = amount, onValueChange = { amount = it },
            label = { Text("Amount") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = beneficiary, onValueChange = { beneficiary = it },
            label = { Text("Beneficiary") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = iban, onValueChange = { iban = it },
            label = { Text("IBAN") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (message.isNotEmpty()) {
            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            enabled = !busy && amount.toDoubleOrNull() != null && beneficiary.isNotBlank() && iban.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = {
                busy = true
                onSubmit(amount.trim(), beneficiary.trim(), iban.trim()) { result ->
                    if (result == null || result.startsWith("Payment")) {
                        onDismiss()
                    } else {
                        message = result ?: "Payment failed"
                        busy = false
                    }
                }
            },
        ) { Text(if (busy) "Sending…" else "Pay") }
    }
}
