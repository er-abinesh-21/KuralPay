package com.kuralpay.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.data.repository.AnalyticsRepository
import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import com.kuralpay.app.domain.model.Insight
import com.kuralpay.app.domain.model.Transaction
import com.kuralpay.app.emergency.ReplayManager
import com.kuralpay.app.insights.SmartInsightsEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val todayTotal: Double = 0.0,
    val todayCount: Int = 0,
    val average: Double = 0.0,
    val maxAmount: Double = 0.0,
    val lastTransaction: Transaction? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val missedCount: Int = 0,
    val replayCount: Int = 1,
    val loaded: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val insightsEngine: SmartInsightsEngine,
    private val replayManager: ReplayManager,
    announcementRepository: AnnouncementRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                analyticsRepository.getTodayStats(),
                transactionRepository.getLastTransaction(),
                transactionRepository.getAllTransactions()
            ) { stats, last, all ->
                Triple(stats, last, all)
            }.collect { (stats, last, all) ->
                _state.update {
                    it.copy(
                        todayTotal = stats.total,
                        todayCount = stats.count,
                        average = stats.average,
                        maxAmount = stats.maxAmount,
                        lastTransaction = last,
                        recentTransactions = all.take(8),
                        loaded = true
                    )
                }
            }
        }

        viewModelScope.launch {
            announcementRepository.getPendingCount().collect { n -> _state.update { it.copy(missedCount = n) } }
        }

        viewModelScope.launch {
            settingsRepository.replayCount.collect { n -> _state.update { it.copy(replayCount = n) } }
        }

        viewModelScope.launch {
            val insights = insightsEngine.generateInsights()
            _state.update { it.copy(insights = insights) }
        }
    }

    fun replayLast() = replayManager.replayLast()

    fun replayMissed() = replayManager.replayPending()

    /** The tile plays missed payments when there are any, otherwise the last N from Settings. */
    fun replayTile() {
        if (_state.value.missedCount > 0) replayManager.replayPending() else replayManager.replayLast()
    }

    fun replayPayment(tx: Transaction) = replayManager.replayPayment(tx.amount, tx.sender, tx.sourceApp)
}
