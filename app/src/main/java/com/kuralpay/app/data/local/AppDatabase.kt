package com.kuralpay.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.dao.PaymentTemplateDao
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.local.entity.AnnouncementQueueEntity
import com.kuralpay.app.data.local.entity.PaymentTemplateEntity
import com.kuralpay.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        PaymentTemplateEntity::class,
        AnnouncementQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentTemplateDao(): PaymentTemplateDao
    abstract fun announcementQueueDao(): AnnouncementQueueDao
}
