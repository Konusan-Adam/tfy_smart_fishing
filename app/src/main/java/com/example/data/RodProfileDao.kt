package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RodProfileDao {

    @Query("SELECT * FROM rod_profiles ORDER BY id ASC")
    fun getAllRodProfiles(): Flow<List<RodProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRodProfile(profile: RodProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<RodProfile>)

    @Delete
    suspend fun deleteRodProfile(profile: RodProfile)

    @Query("DELETE FROM rod_profiles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM rod_profiles")
    suspend fun getCount(): Int
}
