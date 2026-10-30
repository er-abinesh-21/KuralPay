package com.kuralpay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuralpay.app.domain.model.Announcement
import com.kuralpay.app.domain.model.PaymentSource

@Entity(tableName = "announcement_queue")
data class AnnouncementQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val sender: String,
    val sourceApp: String,
    val timestamp: Long,
    val isReplayed: Boolean = false
) {
    fun toDomain(): Announcement = Announcement(
        id = id,
        amount = amount,
        sender = sender,
        sourceApp = try { PaymentSource.valueOf(sourceApp) } catch (_: Exception) { PaymentSource.UNKNOWN },
        timestamp = timestamp,
        isReplayed = isReplayed
    )

    companion object {
        fun fromDomain(a: Announcement): AnnouncementQueueEntity = AnnouncementQueueEntity(
            id = a.id,
            amount = a.amount,
            sender = a.sender,
            sourceApp = a.sourceApp.name,
            timestamp = a.timestamp,
            isReplayed = a.isReplayed
        )
    }
}
