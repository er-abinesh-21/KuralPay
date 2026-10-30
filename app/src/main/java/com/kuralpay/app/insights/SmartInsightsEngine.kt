package com.kuralpay.app.insights

import com.kuralpay.app.data.repository.AnalyticsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import com.kuralpay.app.domain.model.Insight
import com.kuralpay.app.domain.model.InsightType
import com.kuralpay.app.domain.model.PaymentSource
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartInsightsEngine @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend fun generateInsights(): List<Insight> {
        val insights = mutableListOf<Insight>()
        val today = analyticsRepository.getStatsForDay(0).first()
        val yesterday = analyticsRepository.getStatsForDay(-1).first()

        if (today.total > 0 && yesterday.total > 0) {
            val change = ((today.total - yesterday.total) / yesterday.total * 100).toInt()
            val direction = if (change >= 0) "higher" else "lower"
            insights.add(
                Insight(
                    text = "Today's income is ${kotlin.math.abs(change)}% $direction than yesterday.",
                    type = InsightType.DAILY_TREND,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        val topSource = today.sourceDistribution.maxByOrNull { it.second }
        if (topSource != null && topSource.first != PaymentSource.UNKNOWN) {
            insights.add(
                Insight(
                    text = "Most payments today came through ${topSource.first.displayName}.",
                    type = InsightType.SOURCE_PERFORMANCE,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        val peakHour = findPeakHour()
        if (peakHour != null) {
            insights.add(
                Insight(
                    text = "Peak payment hour was ${peakHour.first} to ${peakHour.second}.",
                    type = InsightType.PEAK_HOUR,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        if (today.average > 0 && yesterday.average > 0) {
            val avgChange = ((today.average - yesterday.average) / yesterday.average * 100).toInt()
            if (avgChange != 0) {
                insights.add(
                    Insight(
                        text = "Average transaction value ${if (avgChange > 0) "increased" else "decreased"} by ${kotlin.math.abs(avgChange)}%.",
                        type = InsightType.GROWTH_INDICATOR,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        val weekAgo = analyticsRepository.getStatsForDay(-7).first()
        if (today.total > weekAgo.total) {
            insights.add(
                Insight(
                    text = "Collection trend is improving this week.",
                    type = InsightType.WEEKLY_TREND,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        return insights
    }

    private suspend fun findPeakHour(): Pair<String, String>? {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay()
        val transactions = transactionRepository.getTransactionsForDay(startOfDay, startOfDay + 86400000).first()

        val hourlyCounts = mutableMapOf<Int, Int>()
        transactions.forEach { t ->
            val cal = Calendar.getInstance().apply { timeInMillis = t.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourlyCounts[hour] = (hourlyCounts[hour] ?: 0) + 1
        }

        val peakHour = hourlyCounts.maxByOrNull { it.value } ?: return null
        val endHour = (peakHour.key + 1) % 24
        val fmt = { h: Int -> String.format("%d %s", if (h % 12 == 0) 12 else h % 12, if (h < 12) "AM" else "PM") }
        return fmt(peakHour.key) to fmt(endHour)
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
