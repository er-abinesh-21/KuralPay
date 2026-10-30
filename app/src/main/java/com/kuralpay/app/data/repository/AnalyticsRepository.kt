package com.kuralpay.app.data.repository

import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.domain.model.PaymentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    data class DailyStats(
        val total: Double = 0.0,
        val count: Int = 0,
        val average: Double = 0.0,
        val maxAmount: Double = 0.0,
        val sourceDistribution: List<Pair<PaymentSource, Int>> = emptyList()
    )

    private fun startOfDay(dayOffset: Int = 0): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfDay(dayOffset: Int = 0): Long = startOfDay(dayOffset) + 86400000

    fun getTodayStats(): Flow<DailyStats> = combine(
        transactionDao.getTodayTotal(startOfDay(), endOfDay()),
        transactionDao.getTodayCount(startOfDay(), endOfDay()),
        transactionDao.getAverageForDay(startOfDay(), endOfDay()),
        transactionDao.getMaxAmountForDay(startOfDay(), endOfDay()),
        transactionDao.getSourceDistribution(startOfDay(), endOfDay())
    ) { total, count, avg, max, sourceList ->
        DailyStats(
            total = total,
            count = count,
            average = avg,
            maxAmount = max,
            sourceDistribution = sourceList.map { (source, count) ->
                try { PaymentSource.valueOf(source) } catch (_: Exception) { PaymentSource.UNKNOWN } to count
            }
        )
    }

    fun getStatsForDay(dayOffset: Int): Flow<DailyStats> = combine(
        transactionDao.getTodayTotal(startOfDay(dayOffset), endOfDay(dayOffset)),
        transactionDao.getTodayCount(startOfDay(dayOffset), endOfDay(dayOffset)),
        transactionDao.getAverageForDay(startOfDay(dayOffset), endOfDay(dayOffset)),
        transactionDao.getMaxAmountForDay(startOfDay(dayOffset), endOfDay(dayOffset)),
        transactionDao.getSourceDistribution(startOfDay(dayOffset), endOfDay(dayOffset))
    ) { total, count, avg, max, sourceList ->
        DailyStats(
            total = total,
            count = count,
            average = avg,
            maxAmount = max,
            sourceDistribution = sourceList.map { (source, count) ->
                try { PaymentSource.valueOf(source) } catch (_: Exception) { PaymentSource.UNKNOWN } to count
            }
        )
    }
}
