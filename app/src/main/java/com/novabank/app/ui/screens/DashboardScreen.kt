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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novabank.app.core.data.AccountDao
import com.novabank.app.core.data.BankingRepository
import com.novabank.app.core.network.BankAccountDto
import com.novabank.app.core.network.MeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BankingRepository,
    private val accountDao: AccountDao,
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<BankAccountDto>>(emptyList())
    val accounts: StateFlow<List<BankAccountDto>> = _accounts

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        refresh()
    }

    /** Network-first with the Room cache as instant fallback. */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _accounts.value = accountDao.all().map { it.toDto() }
            try {
                val fresh = repository.accounts()
                _accounts.value = fresh
                accountDao.replaceAll(fresh.map { it.toEntity() })
            } catch (_: Exception) {
                // Cached rows stay on screen — offline-tolerant dashboard.
            }
            _loading.value = false
        }
    }

    private fun com.novabank.app.core.data.AccountEntity.toDto() = BankAccountDto(
        id = id, name = name, iban = iban, currency = currency, balance = balance,
    )

    private fun BankAccountDto.toEntity() = com.novabank.app.core.data.AccountEntity(
        id = id, name = name, iban = iban, currency = currency, balance = balance,
    )
}

@Composable
fun DashboardScreen(me: MeResponse, onLogout: () -> Unit, onTab: (Int) -> Unit = {}) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val accounts by viewModel.accounts.collectAsState()
    val loading by viewModel.loading.collectAsState()

    DashboardContent(
        title = "Welcome, ${me.user.displayName}",
        accounts = accounts,
        loading = loading,
        onRefresh = viewModel::refresh,
        onLogout = onLogout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    title: String,
    accounts: List<BankAccountDto>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && accounts.isEmpty() ->
                Row(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }
            accounts.isEmpty() ->
                Row(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalArrangement = Arrangement.Center,
                ) { Text("No accounts yet") }
            else ->
                LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                    items(accounts, key = { it.id }) { account ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = 16.dp, vertical = 6.dp,
                            ),
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        account.iban,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    "${account.balance} ${account.currency}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
        }
    }
}
