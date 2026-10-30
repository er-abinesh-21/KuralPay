package com.kuralpay.app.di

import android.content.Context
import androidx.room.Room
import com.kuralpay.app.data.local.AppDatabase
import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.dao.ForwardedPaymentDao
import com.kuralpay.app.data.local.dao.PaymentTemplateDao
import com.kuralpay.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kuralpay_db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun providePaymentTemplateDao(db: AppDatabase): PaymentTemplateDao = db.paymentTemplateDao()

    @Provides
    fun provideAnnouncementQueueDao(db: AppDatabase): AnnouncementQueueDao = db.announcementQueueDao()

    @Provides
    fun provideForwardedPaymentDao(db: AppDatabase): ForwardedPaymentDao = db.forwardedPaymentDao()
}
