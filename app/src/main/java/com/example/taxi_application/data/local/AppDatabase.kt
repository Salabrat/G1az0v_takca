package com.example.taxi_application.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taxi_application.data.model.FavoriteAddressEntity
import com.example.taxi_application.data.model.RideHistoryEntity

@Database(
    entities = [RideHistoryEntity::class, FavoriteAddressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rideHistoryDao(): RideHistoryDao
    abstract fun favoriteAddressDao(): FavoriteAddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taxi_glazov_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
