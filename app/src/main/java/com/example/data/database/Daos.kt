package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TweakDao {
    @Query("SELECT * FROM tweaks ORDER BY category, name")
    fun getAllTweaks(): Flow<List<TweakConfig>>

    @Query("SELECT * FROM tweaks WHERE `key` = :key LIMIT 1")
    suspend fun getTweakByKey(key: String): TweakConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTweak(tweak: TweakConfig)

    @Update
    suspend fun updateTweak(tweak: TweakConfig)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM optimizer_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<OptimizerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OptimizerLog)

    @Query("DELETE FROM optimizer_logs")
    suspend fun clearLogs()
}

@Dao
interface MetricDao {
    @Query("SELECT * FROM system_metrics ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMetrics(): Flow<List<SystemMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: SystemMetric)
}

@Dao
interface AIRecommendationDao {
    @Query("SELECT * FROM ai_recommendations ORDER BY timestamp DESC")
    fun getRecommendations(): Flow<List<AIRecommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<AIRecommendation>)

    @Query("UPDATE ai_recommendations SET isApplied = :isApplied WHERE id = :id")
    suspend fun updateAppliedStatus(id: Long, isApplied: Boolean)

    @Query("DELETE FROM ai_recommendations")
    suspend fun clearRecommendations()
}
