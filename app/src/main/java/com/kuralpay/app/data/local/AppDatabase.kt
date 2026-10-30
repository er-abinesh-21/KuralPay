package com.kuralpay.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.dao.ForwardedPaymentDao
import com.kuralpay.app.data.local.dao.PaymentTemplateDao
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.local.entity.AnnouncementQueueEntity
import com.kuralpay.app.data.local.entity.ForwardedPaymentEntity
import com.kuralpay.app.data.local.entity.PaymentTemplateEntity
import com.kuralpay.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        PaymentTemplateEntity::class,
        AnnouncementQueueEntity::class,
        ForwardedPaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentTemplateDao(): PaymentTemplateDao
    abstract fun announcementQueueDao(): AnnouncementQueueDao
    abstract fun forwardedPaymentDao(): ForwardedPaymentDao

    companion object {
        /** Adds the forwarded-payments table. Existing tables and their data are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `forwarded_payments` (" +
                        "`id` TEXT NOT NULL, `fromName` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                        "`payer` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
