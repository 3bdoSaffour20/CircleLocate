package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LocationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationRecordDao {
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<LocationRecord>>

    @Query("SELECT * FROM location_records WHERE (:userId = '' OR userId = :userId) ORDER BY timestamp DESC")
    fun getRecordsForUser(userId: String): Flow<List<LocationRecord>>

    @Query("SELECT * FROM location_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): LocationRecord?

    @Query("SELECT * FROM location_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRecordByRemoteId(remoteId: String): LocationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LocationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<LocationRecord>)

    @Delete
    suspend fun deleteRecord(record: LocationRecord)

    @Query("DELETE FROM location_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM location_records WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM location_records")
    suspend fun clearAll()

    @Query("SELECT * FROM location_records WHERE (name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%') AND (:userId = '' OR userId = :userId) ORDER BY timestamp DESC")
    fun searchRecords(query: String, userId: String): Flow<List<LocationRecord>>
}
