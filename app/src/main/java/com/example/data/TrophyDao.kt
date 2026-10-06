package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrophyDao {

    @Query("SELECT * FROM trophy_catches ORDER BY createdAt DESC")
    fun getAllCatches(): Flow<List<TrophyCatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatch(trophyCatch: TrophyCatch): Long

    @Delete
    suspend fun deleteCatch(trophyCatch: TrophyCatch)

    @Query("DELETE FROM trophy_catches")
    suspend fun clearAll()
}
