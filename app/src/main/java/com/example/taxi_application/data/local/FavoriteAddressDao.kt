package com.example.taxi_application.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.taxi_application.data.model.FavoriteAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAddressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: FavoriteAddressEntity)

    @Query("SELECT * FROM favorite_addresses ORDER BY label ASC")
    fun getAll(): Flow<List<FavoriteAddressEntity>>

    @Delete
    suspend fun delete(address: FavoriteAddressEntity)

    @Query("DELETE FROM favorite_addresses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM favorite_addresses")
    suspend fun getCount(): Int
}
