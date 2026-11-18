package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiOutcome
import com.example.data.model.AiSettings
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data class Testing(val modelId: String) : ConnectionTestState
    data class Success(val message: String, val modelId: String, val latencyMs: Long) : ConnectionTestState
    data class Error(val message: String, val modelId: String) : ConnectionTestState
}

data class AiServicesUiState(
    val settings: AiSettings = AiSettings(),
    val testState: ConnectionTestState = ConnectionTestState.Idle
)

class AiServicesViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)

    val uiState: StateFlow<AiServicesUiState> = combine(
        repository.aiSettings,
        testState
    ) { settings, test ->
        AiServicesUiState(
            settings = settings,
            testState = test
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AiServicesUiState()
    )

    fun selectPrimaryModel(modelId: String) {
        viewModelScope.launch {
            repository.setPrimaryModel(modelId)
            if (uiState.value.settings.fallbackModelId == modelId) {
                repository.setFallbackModel(null)
            }
        }
    }

    fun selectFallbackModel(modelId: String?) {
        viewModelScope.launch {
            repository.setFallbackModel(modelId)
        }
    }

    fun setEconomicMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEconomicMode(enabled)
        }
    }

    fun testConnection(modelId: String = uiState.value.settings.primaryModelId) {
        viewModelScope.launch {
            testState.value = ConnectionTestState.Testing(modelId)
            when (val result = repository.testGeminiConnection(modelId)) {
                is AiOutcome.Success -> {
                    testState.value = ConnectionTestState.Success(
                        message = result.data,
                        modelId = result.modelUsed,
                        latencyMs = result.latencyMs
                    )
                }
                is AiOutcome.Failure -> {
                    testState.value = ConnectionTestState.Error(
                        message = result.message,
                        modelId = result.modelAttempted
                    )
                }
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiServicesViewModel(repository) as T
        }
    }
}
