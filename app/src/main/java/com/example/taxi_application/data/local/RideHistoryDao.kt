package com.example.taxi_application.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taxi_application.data.model.RideHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ride: RideHistoryEntity)

    @Query("SELECT * FROM ride_history ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideHistoryEntity>>

    @Query("SELECT * FROM ride_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRides(limit: Int = 20): Flow<List<RideHistoryEntity>>

    @Query("SELECT * FROM ride_history WHERE id = :id")
    suspend fun getRideById(id: String): RideHistoryEntity?

    @Query("DELETE FROM ride_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM ride_history")
    suspend fun getTotalCount(): Int
}
