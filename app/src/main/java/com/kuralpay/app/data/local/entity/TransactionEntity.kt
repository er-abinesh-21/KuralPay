package com.kuralpay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.domain.model.Transaction

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val sender: String,
    val sourceApp: String,
    val timestamp: Long,
    val rawNotification: String
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        sender = sender,
        sourceApp = try { PaymentSource.valueOf(sourceApp) } catch (_: Exception) { PaymentSource.UNKNOWN },
        timestamp = timestamp,
        rawNotification = rawNotification
    )

    companion object {
        fun fromDomain(t: Transaction): TransactionEntity = TransactionEntity(
            id = t.id,
            amount = t.amount,
            sender = t.sender,
            sourceApp = t.sourceApp.name,
            timestamp = t.timestamp,
            rawNotification = t.rawNotification
        )
    }
}
