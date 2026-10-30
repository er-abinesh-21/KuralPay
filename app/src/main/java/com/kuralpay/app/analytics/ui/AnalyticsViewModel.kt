package com.kuralpay.app.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.data.repository.AnalyticsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** Shop hours shown on the hourly chart: 8 AM to 10 PM. */
val ChartHours = 8..21

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dayOffset = MutableStateFlow(0)
    val dayOffset: StateFlow<Int> = _dayOffset.asStateFlow()

    val stats: StateFlow<AnalyticsRepository.DailyStats> = _dayOffset
        .flatMapLatest { analyticsRepository.getStatsForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsRepository.DailyStats())

    val hourly: StateFlow<List<Int>> = _dayOffset
        .flatMapLatest { offset ->
            val start = startOfDay(offset)
            transactionRepository.getTransactionsForDay(start, start + 86_400_000L).map { txs ->
                val counts = IntArray(24)
                val cal = Calendar.getInstance()
                txs.forEach { cal.timeInMillis = it.timestamp; counts[cal.get(Calendar.HOUR_OF_DAY)]++ }
                ChartHours.map { counts[it] }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartHours.map { 0 })

    fun selectDay(offset: Int) {
        _dayOffset.value = offset
    }

    private fun startOfDay(offset: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, offset)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
