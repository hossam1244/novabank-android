package com.novabank.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novabank.app.core.data.BankingRepository
import com.novabank.app.core.network.CardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repository: BankingRepository,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardDto>>(emptyList())
    val cards: StateFlow<List<CardDto>> = _cards

    init {
        viewModelScope.launch {
            runCatching { repository.cards() }.onSuccess { _cards.value = it }
        }
    }
}

@Composable
fun CardsScreen(viewModel: CardsViewModel = hiltViewModel()) {
    val cards by viewModel.cards.collectAsState()
    if (cards.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No cards issued yet", Modifier.padding(24.dp))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(cards, key = { it.id }) { card ->
            CardFace(card)
        }
    }
}

/** A card face — masked number only; the full PAN exists nowhere in the app. */
@Composable
fun CardFace(card: CardDto) {
    val frozen = card.status == "frozen"
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                if (frozen) Color(0xFF6E747F) else Color(0xFF1A2B4A),
                RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("NovaBank Virtual", color = Color.White.copy(alpha = 0.7f))
            Icon(
                if (frozen) Icons.Default.AcUnit else Icons.Default.CreditCard,
                contentDescription = card.status,
                tint = Color.White.copy(alpha = 0.7f),
            )
        }
        Text(
            "•••• •••• •••• ${card.last4}",
            color = Color.White,
            fontSize = 20.sp,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            "%02d/%d".format(card.expiryMonth, card.expiryYear),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
