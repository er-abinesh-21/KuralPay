package com.kuralpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kuralpay.app.data.local.entity.AnnouncementQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementQueueDao {

    @Insert
    suspend fun insert(announcement: AnnouncementQueueEntity): Long

    @Query("SELECT * FROM announcement_queue WHERE isReplayed = 0 ORDER BY timestamp ASC")
    fun getPendingAnnouncements(): Flow<List<AnnouncementQueueEntity>>

    @Query("SELECT * FROM announcement_queue ORDER BY timestamp DESC LIMIT 1")
    fun getLastAnnouncement(): Flow<AnnouncementQueueEntity?>

    @Query("UPDATE announcement_queue SET isReplayed = 1 WHERE id = :id")
    suspend fun markAsReplayed(id: Long)

    @Query("DELETE FROM announcement_queue WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM announcement_queue")
    suspend fun deleteAll()

    @Query("DELETE FROM announcement_queue WHERE isReplayed = 1")
    suspend fun clearReplayed()

    @Query("SELECT COUNT(*) FROM announcement_queue WHERE isReplayed = 0")
    fun getPendingCount(): Flow<Int>
}
