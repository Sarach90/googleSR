package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdBrief
import com.example.data.model.MotionProject
import com.example.data.model.ProjectStatus
import com.example.ui.components.ActiveModelBadge
import com.example.ui.components.StudioEmptyState
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer
import com.example.ui.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (ProjectStatus?) -> Unit,
    onToggleSortOrder: () -> Unit,
    onCreateProject: (title: String, brief: AdBrief) -> Unit,
    onOpenProject: (String) -> Unit,
    onDuplicateProject: (MotionProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenAiServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectPendingDelete by remember { mutableStateOf<MotionProject?>(null) }

    Scaffold(
        containerColor = StudioObsidian,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = KineticCyan,
                contentColor = StudioObsidian,
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Créer un nouveau projet publicitaire"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    StudioHeroCard(
                        uiState = uiState,
                        onOpenAiServices = onOpenAiServices,
                        onNewProjectClick = { showCreateDialog = true }
                    )
                }

                item {
                    SearchAndFilterSection(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        selectedFilter = uiState.statusFilter,
                        onStatusFilterChange = onStatusFilterChange,
                        sortLabel = uiState.sortOrder.label,
                        onToggleSortOrder = onToggleSortOrder
                    )
                }

                if (uiState.projects.isEmpty() && !uiState.isLoading) {
                    item {
                        StudioEmptyState(
                            title = if (uiState.allProjectsCount == 0) {
                                "Votre studio est prêt"
                            } else {
                                "Aucun projet ne correspond"
                            },
                            subtitle = if (uiState.allProjectsCount == 0) {
                                "Créez votre première campagne publicitaire : du brief commercial au storyboard seconde par seconde avec Gemini."
                            } else {
                                "Modifiez votre recherche ou réinitialisez les filtres de statut."
                            },
                            actionLabel = if (uiState.allProjectsCount == 0) {
                                "Démarrer une publicité"
                            } else {
                                "Réinitialiser les filtres"
                            },
                            onActionClick = {
                                if (uiState.allProjectsCount == 0) {
                                    showCreateDialog = true
                                } else {
                                    onSearchQueryChange("")
                                    onStatusFilterChange(null)
                                }
                            }
                        )
                    }
                } else {
                    items(uiState.projects, key = { it.id }) { project ->
                        ProjectSummaryCard(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onDuplicate = { onDuplicateProject(project) },
                            onRequestDelete = { projectPendingDelete = project }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NewProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, productName, duration, platform ->
                showCreateDialog = false
                onCreateProject(
                    title,
                    AdBrief(
                        productName = productName,
                        durationSeconds = duration,
                        platform = platform
                    )
                )
            }
        )
    }

    projectPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            title = { Text("Supprimer ce projet ?") },
            text = {
                Text("Le projet « ${target.title} » ainsi que son historique de versions seront supprimés définitivement.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(target.id)
                        projectPendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun StudioHeroCard(
    uiState: DashboardUiState,
    onOpenAiServices: () -> Unit,
    onNewProjectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StudioSurface)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_studio_hero),
            contentDescription = "Bannière Motion Studio AI",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StudioObsidian.copy(alpha = 0.55f),
                            StudioObsidian.copy(alpha = 0.92f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = null,
                        tint = KineticCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MOTION STUDIO AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                ActiveModelBadge(
                    aiSettings = uiState.aiSettings,
                    onClick = onOpenAiServices
                )
            }

            Text(
                text = "Concevez vos publicités du brief commercial au storyboard seconde par seconde.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricPill(
                    label = "Projets",
                    value = "${uiState.allProjectsCount}",
                    accent = KineticCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Validés",
                    value = "${uiState.validatedCount}",
                    accent = ValidatedEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Appels IA",
                    value = "${uiState.aiSettings.sessionCallCount}",
                    accent = DirectorAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onNewProjectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("hero_new_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouveau projet publicitaire", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurfaceElevated.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: ProjectStatus?,
    onStatusFilterChange: (ProjectStatus?) -> Unit,
    sortLabel: String,
    onToggleSortOrder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher un projet, produit, secteur…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rechercher"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_search_input")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Tous") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KineticCyanContainer,
                            selectedLabelColor = KineticCyan
                        ),
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                items(ProjectStatus.entries) { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { onStatusFilterChange(status) },
                        label = { Text(status.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KineticCyanContainer,
                            selectedLabelColor = KineticCyan
                        ),
                        modifier = Modifier.testTag("filter_${status.name.lowercase()}")
                    )
                }
            }
            TextButton(
                onClick = onToggleSortOrder,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sort_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Changer l'ordre de tri",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(sortLabel)
            }
        }
    }
}

@Composable
private fun ProjectSummaryCard(
    project: MotionProject,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.FRENCH) }
    val statusColor = when (project.status) {
        ProjectStatus.VALIDATED -> ValidatedEmerald
        ProjectStatus.IN_PROGRESS -> KineticCyan
        ProjectStatus.DRAFT -> DirectorAmber
    }
    val statusBg = when (project.status) {
        ProjectStatus.VALIDATED -> ValidatedEmeraldContainer
        ProjectStatus.IN_PROGRESS -> KineticCyanContainer
        ProjectStatus.DRAFT -> StudioSurfaceElevated
    }

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (project.brief.productName.isNotBlank()) {
                                append(project.brief.productName)
                                append(" • ")
                            }
                            append("${project.brief.durationSeconds}s • ${project.brief.platform}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = project.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Étape active : ${project.currentStep.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${project.validatedStepsCount}/4 validées",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                LinearProgressIndicator(
                    progress = { project.validatedStepsCount / 4f },
                    color = statusColor,
                    trackColor = StudioSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Modifié : ${dateFormatter.format(Date(project.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("duplicate_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Dupliquer le projet",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer le projet",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onOpen,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ouvrir")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, productName: String, duration: Int, platform: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(30) }
    var selectedPlatform by remember { mutableStateOf("TikTok / Reels (9:16)") }

    val durations = listOf(15, 30, 45, 60)
    val platforms = listOf(
        "TikTok / Reels (9:16)",
        "YouTube Shorts (9:16)",
        "YouTube Ads (16:9)",
        "Instagram Feed (4:5)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau projet publicitaire") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nom du projet") },
                    placeholder = { Text("Ex: Campagne lancement montre Pulse") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nom du produit / marque") },
                    placeholder = { Text("Ex: Pulse Watch Pro") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_product_input")
                )
                Text("Durée cible", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durations.forEach { d ->
                        FilterChip(
                            selected = selectedDuration == d,
                            onClick = { selectedDuration = d },
                            label = { Text("${d}s") }
                        )
                    }
                }
                Text("Plateforme", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(platforms) { p ->
                        FilterChip(
                            selected = selectedPlatform == p,
                            onClick = { selectedPlatform = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedTitle = title.ifBlank { productName.ifBlank { "Nouvelle publicité" } }
                    onCreate(resolvedTitle, productName, selectedDuration, selectedPlatform)
                },
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
