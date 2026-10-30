package com.kuralpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuralpay.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1")
    fun getLastTransaction(): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTodayTotal(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT sourceApp, COUNT(*) as count FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay GROUP BY sourceApp ORDER BY count DESC")
    fun getSourceDistribution(startOfDay: Long, endOfDay: Long): Flow<List<SourceCount>>

    @Query("SELECT COALESCE(MAX(amount), 0) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getMaxAmountForDay(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(AVG(amount), 0) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getAverageForDay(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT * FROM transactions WHERE sender LIKE '%' || :senderName || '%' AND timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsBySender(senderName: String, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay AND sourceApp = :source ORDER BY timestamp DESC")
    fun getTransactionsBySource(source: String, startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>
}

data class SourceCount(
    val sourceApp: String,
    val count: Int
)
