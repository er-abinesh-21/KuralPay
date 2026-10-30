package com.kuralpay.app.assistant

import com.kuralpay.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    data class QueryResponse(val text: String, val amount: Double? = null)

    suspend fun processQuery(query: String): QueryResponse {
        val lower = query.lowercase()
        val startOfDay = getStartOfDay()
        val endOfDay = startOfDay + 86400000

        return when {
            lower.contains("how much") && (lower.contains("today") || lower.contains("receive")) -> {
                val totalValue = transactionRepository.getTodayTotal(startOfDay, endOfDay).first()
                val countValue = transactionRepository.getTodayCount(startOfDay, endOfDay).first()
                QueryResponse("You have received ₹${String.format("%,.0f", totalValue)} from $countValue transactions today.", totalValue)
            }
            lower.contains("last payment") || (lower.contains("who") && lower.contains("last")) -> {
                val last = transactionRepository.getLastTransaction().first()
                last?.let {
                    QueryResponse("Last payment of ₹${String.format("%,.0f", it.amount)} from ${it.sender} via ${it.sourceApp.displayName}.")
                } ?: QueryResponse("No payments recorded yet.")
            }
            lower.contains("most") && (lower.contains("used") || lower.contains("payment")) -> {
                val dist = transactionRepository.getSourceDistribution(startOfDay, endOfDay).first()
                val top = dist.maxByOrNull { it.second }
                top?.let {
                    QueryResponse("${it.first.displayName} is the most used payment app.")
                } ?: QueryResponse("No payment data available.")
            }
            lower.contains("biggest") || lower.contains("largest") || lower.contains("highest") -> {
                val maxValue = transactionRepository.getMaxAmountForDay(startOfDay, endOfDay).first()
                QueryResponse("Your biggest payment today was ₹${String.format("%,.0f", maxValue)}.")
            }
            lower.contains("how many") || lower.contains("count") -> {
                val countValue = transactionRepository.getTodayCount(startOfDay, endOfDay).first()
                QueryResponse("You have received $countValue payments today.")
            }
            else -> QueryResponse("I can help with: today's total, last payment, most used app, biggest payment, and transaction count.")
        }
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
