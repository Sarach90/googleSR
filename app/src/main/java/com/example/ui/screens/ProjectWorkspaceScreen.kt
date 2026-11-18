package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectVersion
import com.example.data.model.WorkflowStep
import com.example.ui.components.ActiveModelBadge
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.EconomicConfirmationDialog
import com.example.ui.components.WorkflowStepperBar
import com.example.ui.screens.tabs.BriefStudioTab
import com.example.ui.screens.tabs.ConceptStudioTab
import com.example.ui.screens.tabs.ScriptStudioTab
import com.example.ui.screens.tabs.StoryboardStudioTab
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.PendingAiOperation
import com.example.ui.viewmodel.ProjectWorkspaceViewModel
import com.example.ui.viewmodel.WorkspaceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    uiState: WorkspaceUiState,
    viewModel: ProjectWorkspaceViewModel,
    onNavigateBack: () -> Unit,
    onOpenAiServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project = uiState.project
    val isAiBusy = uiState.aiOpState is AiOperationState.Loading

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val shareProjectDossier = {
        val summary = viewModel.buildExportSummary()
        if (summary.isNotBlank()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Motion Studio AI — ${project?.title ?: "Dossier"}")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Partager le dossier de production"))
        }
    }

    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour au tableau de bord"
                        )
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = project?.title ?: "Chargement…",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (project != null) {
                                IconButton(
                                    onClick = { showRenameDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renommer le projet",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    ActiveModelBadge(
                        aiSettings = uiState.aiSettings,
                        onClick = onOpenAiServices
                    )
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique des versions"
                        )
                    }
                    IconButton(
                        onClick = shareProjectDossier,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager le dossier"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoadingProject) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = KineticCyan)
                }
            } else if (project == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Projet introuvable",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Retour au tableau de bord")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                ) {
                    WorkflowStepperBar(
                        project = project,
                        onStepSelected = viewModel::selectStep
                    )

                    AiStatusBanner(
                        opState = uiState.aiOpState,
                        onRetryFallback = viewModel::retryWithFallback,
                        onDismiss = viewModel::dismissBanner
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        when (project.currentStep) {
                            WorkflowStep.BRIEF -> BriefStudioTab(
                                project = project,
                                isAiBusy = isAiBusy,
                                onBriefChanged = viewModel::updateBrief,
                                onPickProductImage = viewModel::importProductImage,
                                onRemoveProductImage = viewModel::removeProductImage,
                                onSaveSnapshot = { viewModel.saveManualSnapshot("Brief enregistré") },
                                onValidateBriefOnly = { viewModel.validateBrief(goToNextStep = true) },
                                onGenerateConcepts = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateConcepts)
                                }
                            )

                            WorkflowStep.CONCEPTS -> ConceptStudioTab(
                                project = project,
                                isAiBusy = isAiBusy,
                                onPickProductImage = viewModel::importProductImage,
                                onRemoveProductImage = viewModel::removeProductImage,
                                onSelectConcept = viewModel::selectConcept,
                                onUpdateConcept = viewModel::updateConcept,
                                onRegenerateSingleConcept = { idx, name ->
                                    viewModel.requestAiOperation(
                                        PendingAiOperation.RegenerateConcept(idx, name)
                                    )
                                },
                                onRegenerateAllConcepts = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateConcepts)
                                },
                                onValidateConceptAndNext = { viewModel.validateConcepts(goToNextStep = true) },
                                onGenerateScriptDirectly = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateScript)
                                },
                                onBackToBrief = { viewModel.selectStep(WorkflowStep.BRIEF) }
                            )

                            WorkflowStep.SCRIPT -> ScriptStudioTab(
                                project = project,
                                isAiBusy = isAiBusy,
                                onPickProductImage = viewModel::importProductImage,
                                onRemoveProductImage = viewModel::removeProductImage,
                                onUpdateSection = viewModel::updateScriptSection,
                                onUpdateDirectorNotes = viewModel::updateDirectorNotes,
                                onRegenerateSection = { sec ->
                                    viewModel.requestAiOperation(
                                        PendingAiOperation.RegenerateScriptSection(sec)
                                    )
                                },
                                onRegenerateFullScript = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateScript)
                                },
                                onValidateScriptOnly = { viewModel.validateScript(goToNextStep = true) },
                                onGenerateStoryboardDirectly = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateStoryboard)
                                },
                                onBackToConcepts = { viewModel.selectStep(WorkflowStep.CONCEPTS) }
                            )

                            WorkflowStep.STORYBOARD -> StoryboardStudioTab(
                                project = project,
                                isAiBusy = isAiBusy,
                                onPickProductImage = viewModel::importProductImage,
                                onRemoveProductImage = viewModel::removeProductImage,
                                onUpdateScene = viewModel::updateScene,
                                onAddScene = viewModel::addScene,
                                onDeleteScene = viewModel::deleteScene,
                                onMoveScene = viewModel::moveScene,
                                onAlignDuration = viewModel::alignStoryboardDuration,
                                onRegenerateScene = { idx, number ->
                                    viewModel.requestAiOperation(
                                        PendingAiOperation.RegenerateScene(idx, number)
                                    )
                                },
                                onRegenerateFullStoryboard = {
                                    viewModel.requestAiOperation(PendingAiOperation.GenerateStoryboard)
                                },
                                onValidateStoryboard = viewModel::validateStoryboard,
                                onShareExport = shareProjectDossier,
                                onBackToScript = { viewModel.selectStep(WorkflowStep.SCRIPT) }
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.pendingConfirmation?.let { pending ->
        EconomicConfirmationDialog(
            pendingOperation = pending,
            aiSettings = uiState.aiSettings,
            onConfirm = viewModel::confirmPendingAiOperation,
            onDismiss = viewModel::cancelPendingAiOperation
        )
    }

    if (showRenameDialog && project != null) {
        RenameProjectDialog(
            currentTitle = project.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newTitle ->
                viewModel.updateProjectTitle(newTitle)
                showRenameDialog = false
            }
        )
    }

    if (showHistoryDialog) {
        VersionHistoryDialog(
            versions = uiState.versions,
            onRestore = { ver ->
                viewModel.restoreVersion(ver)
                showHistoryDialog = false
            },
            onSaveNow = {
                viewModel.saveManualSnapshot("Sauvegarde manuelle")
            },
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
private fun RenameProjectDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le projet") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(title.ifBlank { currentTitle }) }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun VersionHistoryDialog(
    versions: List<ProjectVersion>,
    onRestore: (ProjectVersion) -> Unit,
    onSaveNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.FRENCH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historique des versions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSaveNow,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text("Créer un point de sauvegarde maintenant")
                }

                if (versions.isEmpty()) {
                    Text(
                        text = "Aucun instantané enregistré. Les validations et régénérations créent automatiquement une version de secours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(versions, key = { it.id }) { ver ->
                            Surface(
                                color = StudioSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ver.label,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${ver.stepName} • ${dateFormat.format(Date(ver.createdAt))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onRestore(ver) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restaurer cette version",
                                            tint = KineticCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
