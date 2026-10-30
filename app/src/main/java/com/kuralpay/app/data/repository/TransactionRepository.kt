package com.kuralpay.app.data.repository

import com.kuralpay.app.data.local.dao.SourceCount
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.local.entity.TransactionEntity
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    fun getLastTransaction(): Flow<Transaction?> =
        transactionDao.getLastTransaction().map { it?.toDomain() }

    fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForDay(startOfDay, endOfDay).map { entities -> entities.map { it.toDomain() } }

    fun getTodayTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        transactionDao.getTodayTotal(startOfDay, endOfDay)

    fun getTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        transactionDao.getTodayCount(startOfDay, endOfDay)

    fun getSourceDistribution(startOfDay: Long, endOfDay: Long): Flow<List<Pair<PaymentSource, Int>>> =
        transactionDao.getSourceDistribution(startOfDay, endOfDay).map { list ->
            list.map { (sourceApp, count) ->
                val source = try { PaymentSource.valueOf(sourceApp) } catch (_: Exception) { PaymentSource.UNKNOWN }
                source to count
            }
        }

    fun getMaxAmountForDay(startOfDay: Long, endOfDay: Long): Flow<Double> =
        transactionDao.getMaxAmountForDay(startOfDay, endOfDay)

    fun getAverageForDay(startOfDay: Long, endOfDay: Long): Flow<Double> =
        transactionDao.getAverageForDay(startOfDay, endOfDay)

    fun getTransactionsBySender(senderName: String, start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBySender(senderName, start, end).map { entities -> entities.map { it.toDomain() } }

    fun getTransactionsBySource(source: PaymentSource, startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBySource(source.name, startOfDay, endOfDay).map { entities -> entities.map { it.toDomain() } }

    suspend fun recordTransaction(transaction: Transaction): Long =
        transactionDao.insert(TransactionEntity.fromDomain(transaction))
}
