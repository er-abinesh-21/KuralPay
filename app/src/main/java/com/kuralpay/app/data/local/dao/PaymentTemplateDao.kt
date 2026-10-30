package com.kuralpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuralpay.app.data.local.entity.PaymentTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: PaymentTemplateEntity): Long

    @Query("SELECT * FROM payment_templates WHERE appPackage = :appPackage ORDER BY confidenceScore DESC")
    fun getTemplatesForApp(appPackage: String): Flow<List<PaymentTemplateEntity>>

    @Query("SELECT * FROM payment_templates ORDER BY confidenceScore DESC")
    fun getAllTemplates(): Flow<List<PaymentTemplateEntity>>

    @Query("UPDATE payment_templates SET confidenceScore = :score WHERE id = :id")
    suspend fun updateConfidence(id: Long, score: Float)

    @Query("DELETE FROM payment_templates WHERE id = :id")
    suspend fun delete(id: Long)
}
