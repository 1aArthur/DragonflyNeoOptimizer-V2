package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AIRecommendation
import com.example.data.database.OptimizerLog
import com.example.data.database.SystemMetric
import com.example.data.database.TweakConfig
import com.example.data.executor.PrivilegeMode
import com.example.data.repository.OptimizerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AIState {
    object Idle : AIState()
    object Loading : AIState()
    data class Success(val message: String) : AIState()
    data class Error(val error: String) : AIState()
}

class OptimizerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OptimizerRepository(application)

    val tweaks: StateFlow<List<TweakConfig>> = repository.allTweaks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<OptimizerLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metrics: StateFlow<List<SystemMetric>> = repository.recentMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendations: StateFlow<List<AIRecommendation>> = repository.recommendations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTab = MutableStateFlow("dashboard")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _privilegeMode = MutableStateFlow(PrivilegeMode.LIMITED)
    val privilegeMode: StateFlow<PrivilegeMode> = _privilegeMode.asStateFlow()

    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    private val _isRefreshingMetrics = MutableStateFlow(false)
    val isRefreshingMetrics: StateFlow<Boolean> = _isRefreshingMetrics.asStateFlow()

    private var metricsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultTweaks()
            detectPermissions()
            startMetricsMonitoring()
        }
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun detectPermissions() {
        viewModelScope.launch {
            val mode = repository.detectHighestPrivilegeMode()
            _privilegeMode.value = mode
            _isShizukuRunning.value = repository.isShizukuInstalledAndRunning()
            _hasShizukuPermission.value = repository.hasShizukuPermission()
        }
    }

    fun requestShizukuPermission() {
        try {
            if (repository.isShizukuInstalledAndRunning()) {
                rikka.shizuku.Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) {
            // Log or handle gracefully
        }
    }

    private fun startMetricsMonitoring() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            while (true) {
                _isRefreshingMetrics.value = true
                repository.recordMetrics()
                _isRefreshingMetrics.value = false
                delay(3000) // update every 3 seconds for active visual charts
            }
        }
    }

    fun applyTweak(key: String) {
        viewModelScope.launch {
            repository.applyTweak(key, _privilegeMode.value)
        }
    }

    fun revertTweak(key: String) {
        viewModelScope.launch {
            repository.revertTweak(key, _privilegeMode.value)
        }
    }

    fun runFullAIAnalysis() {
        viewModelScope.launch {
            _aiState.value = AIState.Loading
            val statusMessage = repository.generateAIRecommendations()
            if (statusMessage.contains("sucesso", ignoreCase = true)) {
                _aiState.value = AIState.Success(statusMessage)
            } else {
                _aiState.value = AIState.Error(statusMessage)
            }
        }
    }

    fun applyAIRecommendation(rec: AIRecommendation) {
        viewModelScope.launch {
            repository.applyRecommendation(rec, _privilegeMode.value)
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        metricsJob?.cancel()
    }
}
