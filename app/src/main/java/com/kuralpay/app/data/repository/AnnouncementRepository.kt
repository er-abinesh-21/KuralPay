package com.kuralpay.app.data.repository

import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.entity.AnnouncementQueueEntity
import com.kuralpay.app.domain.model.Announcement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepository @Inject constructor(
    private val announcementQueueDao: AnnouncementQueueDao
) {
    fun getPendingAnnouncements(): Flow<List<Announcement>> =
        announcementQueueDao.getPendingAnnouncements().map { entities -> entities.map { it.toDomain() } }

    fun getLastAnnouncement(): Flow<Announcement?> =
        announcementQueueDao.getLastAnnouncement().map { it?.toDomain() }

    fun getPendingCount(): Flow<Int> =
        announcementQueueDao.getPendingCount()

    suspend fun enqueue(announcement: Announcement): Long =
        announcementQueueDao.insert(AnnouncementQueueEntity.fromDomain(announcement))

    suspend fun markAsReplayed(id: Long) =
        announcementQueueDao.markAsReplayed(id)

    suspend fun clearReplayed() =
        announcementQueueDao.clearReplayed()
}
