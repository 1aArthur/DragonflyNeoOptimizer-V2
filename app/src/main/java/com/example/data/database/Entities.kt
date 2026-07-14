package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tweaks")
data class TweakConfig(
    @PrimaryKey val key: String,
    val name: String,
    val category: String, // e.g., "Performance", "Battery", "Network", "UI"
    val description: String,
    val value: String,
    val originalValue: String?,
    val isApplied: Boolean,
    val lastAppliedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "optimizer_logs")
data class OptimizerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionName: String,
    val status: String, // "SUCCESS", "FAILURE", "REVERTED"
    val details: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_metrics")
data class SystemMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ramUsagePercent: Float,
    val cpuUsagePercent: Float,
    val batteryTempCelsius: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_recommendations")
data class AIRecommendation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val tweakKey: String,
    val suggestedValue: String,
    val confidence: Float, // 0.0 to 1.0
    val isApplied: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
