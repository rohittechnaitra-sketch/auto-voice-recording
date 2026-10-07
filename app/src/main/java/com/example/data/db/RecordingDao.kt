package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE emailStatus = :status ORDER BY timestamp DESC")
    fun getRecordingsByStatus(status: String): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingItem?

    @Query("SELECT * FROM recordings WHERE emailStatus != 'SENT' ORDER BY timestamp DESC")
    suspend fun getPendingOrFailedRecordings(): List<RecordingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingItem): Long

    @Update
    suspend fun update(recording: RecordingItem)

    @Delete
    suspend fun delete(recording: RecordingItem)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recordings")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE emailStatus = 'SENT'")
    fun getSentCount(): Flow<Int>
}
