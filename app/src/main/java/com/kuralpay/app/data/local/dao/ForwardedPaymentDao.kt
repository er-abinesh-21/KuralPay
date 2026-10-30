package com.kuralpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuralpay.app.data.local.entity.ForwardedPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardedPaymentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: ForwardedPaymentEntity)

    @Query("SELECT * FROM forwarded_payments ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ForwardedPaymentEntity>>

    @Query("DELETE FROM forwarded_payments WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
