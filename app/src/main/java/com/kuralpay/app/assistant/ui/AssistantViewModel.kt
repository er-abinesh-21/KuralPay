package com.kuralpay.app.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.assistant.QueryProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.nanoTime()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val queryProcessor: QueryProcessor
) : ViewModel() {

    val suggestions = listOf(
        "How much did I receive today?",
        "Who paid last?",
        "Which app is used most?",
        "Biggest payment today?",
        "How many payments today?"
    )

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "Vanakkam! Ask me about today's payments. I can tell you totals, the last payer, your busiest app and more.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    fun processQuery(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _messages.update { it + ChatMessage(text = q, isUser = true) }
            _thinking.value = true
            delay(350)
            val response = queryProcessor.processQuery(q)
            _thinking.value = false
            _messages.update { it + ChatMessage(text = response.text, isUser = false) }
        }
    }
}
