package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdBrief
import com.example.data.model.AiSettings
import com.example.data.model.MotionProject
import com.example.data.model.ProjectStatus
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProjectSortOrder(val label: String) {
    RECENT("Plus récents"),
    NAME("Nom A–Z")
}

data class DashboardUiState(
    val projects: List<MotionProject> = emptyList(),
    val allProjectsCount: Int = 0,
    val validatedCount: Int = 0,
    val inProgressCount: Int = 0,
    val searchQuery: String = "",
    val statusFilter: ProjectStatus? = null,
    val sortOrder: ProjectSortOrder = ProjectSortOrder.RECENT,
    val aiSettings: AiSettings = AiSettings(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<ProjectStatus?>(null)
    private val sortOrder = MutableStateFlow(ProjectSortOrder.RECENT)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allProjects,
        repository.aiSettings,
        searchQuery,
        statusFilter,
        sortOrder
    ) { allProjects, settings, query, filter, sort ->
        val filtered = allProjects.filter { project ->
            val matchesQuery = query.isBlank() ||
                project.title.contains(query, ignoreCase = true) ||
                project.brief.productName.contains(query, ignoreCase = true) ||
                project.brief.sector.contains(query, ignoreCase = true) ||
                project.brief.platform.contains(query, ignoreCase = true)
            val matchesStatus = filter == null || project.status == filter
            matchesQuery && matchesStatus
        }
        val sorted = when (sort) {
            ProjectSortOrder.RECENT -> filtered.sortedByDescending { it.updatedAt }
            ProjectSortOrder.NAME -> filtered.sortedBy { it.title.lowercase() }
        }
        DashboardUiState(
            projects = sorted,
            allProjectsCount = allProjects.size,
            validatedCount = allProjects.count { it.status == ProjectStatus.VALIDATED },
            inProgressCount = allProjects.count { it.status == ProjectStatus.IN_PROGRESS },
            searchQuery = query,
            statusFilter = filter,
            sortOrder = sort,
            aiSettings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DashboardUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterSelected(status: ProjectStatus?) {
        statusFilter.value = status
    }

    fun onToggleSortOrder() {
        sortOrder.value = if (sortOrder.value == ProjectSortOrder.RECENT) {
            ProjectSortOrder.NAME
        } else {
            ProjectSortOrder.RECENT
        }
    }

    fun createNewProject(
        title: String,
        brief: AdBrief = AdBrief(),
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val created = repository.createProject(title, brief)
            onCreated(created.id)
        }
    }

    fun duplicateProject(project: MotionProject) {
        viewModelScope.launch {
            repository.duplicateProject(project)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
