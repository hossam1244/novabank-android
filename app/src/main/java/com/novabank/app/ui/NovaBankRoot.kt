package com.novabank.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novabank.app.core.data.BankingRepository
import com.novabank.app.core.network.MeResponse
import com.novabank.app.ui.screens.CardsScreen
import com.novabank.app.ui.screens.DashboardScreen
import com.novabank.app.ui.screens.LoginScreen
import com.novabank.app.ui.screens.PaymentsScreen
import com.novabank.app.ui.theme.NovaBankTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Root state machine: restoring session → login → app. */
sealed interface AppStart {
    data object Restoring : AppStart
    data class NeedLogin(val error: String = "") : AppStart
    data class Ready(val me: MeResponse) : AppStart
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: BankingRepository,
    private val tokenStore: com.novabank.app.core.storage.TokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow<AppStart>(AppStart.Restoring)
    val state: StateFlow<AppStart> = _state

    init {
        viewModelScope.launch {
            if (tokenStore.accessToken() == null) {
                _state.value = AppStart.NeedLogin()
                return@launch
            }
            try {
                _state.value = AppStart.Ready(repository.me())
            } catch (_: Exception) {
                _state.value = AppStart.NeedLogin()
            }
        }
    }

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val me = repository.login(email, password)
                _state.value = AppStart.Ready(me)
                onResult(null)
            } catch (error: Exception) {
                onResult(error.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        repository.logout()
        _state.value = AppStart.NeedLogin()
    }
}

@Composable
fun NovaBankRoot(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    NovaBankTheme {
        when (val current = state) {
            AppStart.Restoring -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is AppStart.NeedLogin -> LoginScreen(
                error = current.error,
                onLogin = { email, password, cb -> viewModel.login(email, password, cb) },
            )
            is AppStart.Ready -> MainNav(
                me = current.me,
                onLogout = viewModel::logout,
            )
        }
    }
}

@Composable
private fun MainNav(me: MeResponse, onLogout: () -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(me, onLogout) }
        composable("payments") { PaymentsScreen() }
        composable("cards") { CardsScreen() }
    }
}
