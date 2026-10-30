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
import com.kuralpay.app.data.local.dao.ForwardedPaymentDao
import com.kuralpay.app.forwarding.ForwardedPayment
import javax.inject.Inject

/** One line in Recent payments: a payment this phone received, or one a linked user forwarded. */
sealed interface RecentEntry {
    val timestamp: Long
    data class Own(val tx: Transaction) : RecentEntry { override val timestamp get() = tx.timestamp }
    data class Forwarded(val payment: ForwardedPayment) : RecentEntry { override val timestamp get() = payment.timestamp }
}

data class DashboardState(
    val todayTotal: Double = 0.0,
    val todayCount: Int = 0,
    val average: Double = 0.0,
    val maxAmount: Double = 0.0,
    val lastTransaction: Transaction? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
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
    settingsRepository: SettingsRepository,
    forwardedPaymentDao: ForwardedPaymentDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                analyticsRepository.getTodayStats(),
                transactionRepository.getLastTransaction(),
                transactionRepository.getAllTransactions(),
                forwardedPaymentDao.getRecent(RECENT_LIMIT)
            ) { stats, last, all, forwarded ->
                val recent = (all.take(RECENT_LIMIT).map { RecentEntry.Own(it) } +
                    forwarded.map { RecentEntry.Forwarded(it.toDomain()) })
                    .sortedByDescending { it.timestamp }
                    .take(RECENT_LIMIT)
                Triple(stats, last, all to recent)
            }.collect { (stats, last, lists) ->
                val (all, recent) = lists
                _state.update {
                    it.copy(
                        todayTotal = stats.total,
                        todayCount = stats.count,
                        average = stats.average,
                        maxAmount = stats.maxAmount,
                        lastTransaction = last,
                        recentTransactions = all.take(RECENT_LIMIT),
                        recent = recent,
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

    private companion object {
        const val RECENT_LIMIT = 8
    }
}
