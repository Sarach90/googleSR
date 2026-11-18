package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiOutcome
import com.example.data.model.AdBrief
import com.example.data.model.AdConcept
import com.example.data.model.AiSettings
import com.example.data.model.GeminiModelCatalog
import com.example.data.model.MotionProject
import com.example.data.model.ProjectVersion
import com.example.data.model.ScriptSection
import com.example.data.model.StoryboardScene
import com.example.data.model.WorkflowStep
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface AiOperationState {
    data object Idle : AiOperationState
    data class Loading(val label: String, val modelId: String) : AiOperationState
    data class SuccessBanner(val message: String) : AiOperationState
    data class Error(
        val message: String,
        val fallbackModelId: String?,
        val retryAction: PendingAiOperation?
    ) : AiOperationState
}

sealed interface PendingAiOperation {
    val title: String
    val description: String

    data object GenerateConcepts : PendingAiOperation {
        override val title = "Générer 3 concepts publicitaires"
        override val description = "Gemini va analyser votre brief et produire 3 concepts distincts. Les concepts existants seront sauvegardés dans l'historique avant remplacement."
    }

    data class RegenerateConcept(val index: Int, val conceptName: String) : PendingAiOperation {
        override val title = "Régénérer le concept « $conceptName »"
        override val description = "Gemini va créer une nouvelle proposition distincte des deux autres concepts."
    }

    data object GenerateScript : PendingAiOperation {
        override val title = "Générer le script publicitaire"
        override val description = "Gemini va rédiger les 5 parties (voix off + texte écran) calibrées sur la durée cible."
    }

    data class RegenerateScriptSection(val section: ScriptSection) : PendingAiOperation {
        override val title = "Régénérer la section « ${section.type.title} »"
        override val description = "Gemini va réécrire uniquement cette partie en conservant le reste du script."
    }

    data object GenerateStoryboard : PendingAiOperation {
        override val title = "Générer le storyboard seconde par seconde"
        override val description = "Gemini va découper le script en scènes détaillées alignées exactement sur la durée cible."
    }

    data class RegenerateScene(val index: Int, val sceneNumber: Int) : PendingAiOperation {
        override val title = "Régénérer la scène #$sceneNumber"
        override val description = "Gemini va proposer une nouvelle mise en scène pour ce plan tout en préservant les autres scènes."
    }
}

data class WorkspaceUiState(
    val project: MotionProject? = null,
    val versions: List<ProjectVersion> = emptyList(),
    val aiSettings: AiSettings = AiSettings(),
    val aiOpState: AiOperationState = AiOperationState.Idle,
    val pendingConfirmation: PendingAiOperation? = null,
    val isLoadingProject: Boolean = true
)

private data class WorkspaceMeta(
    val isLoading: Boolean,
    val opState: AiOperationState,
    val pending: PendingAiOperation?
)

class ProjectWorkspaceViewModel(
    private val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    private val _project = MutableStateFlow<MotionProject?>(null)
    private val _isLoadingProject = MutableStateFlow(true)
    private val aiOpState = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    private val pendingConfirmation = MutableStateFlow<PendingAiOperation?>(null)
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            _project.value = repository.observeProject(projectId).firstOrNull()
            _isLoadingProject.value = false
        }
    }

    val uiState: StateFlow<WorkspaceUiState> = combine(
        _project,
        repository.observeVersions(projectId),
        repository.aiSettings,
        combine(_isLoadingProject, aiOpState, pendingConfirmation, ::WorkspaceMeta)
    ) { project, versions, settings, meta ->
        WorkspaceUiState(
            project = project,
            versions = versions,
            aiSettings = settings,
            aiOpState = meta.opState,
            pendingConfirmation = meta.pending,
            isLoadingProject = meta.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = WorkspaceUiState()
    )

    private fun updateProjectSync(transform: (MotionProject) -> MotionProject): MotionProject? {
        val current = _project.value ?: return null
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        _project.value = updated
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveProject(updated)
            }
        }
        return updated
    }

    fun dismissBanner() {
        aiOpState.value = AiOperationState.Idle
    }

    fun selectStep(step: WorkflowStep) {
        val current = _project.value ?: return
        when (step) {
            WorkflowStep.BRIEF -> {
                updateProjectSync { it.copy(currentStep = WorkflowStep.BRIEF) }
            }

            WorkflowStep.CONCEPTS -> {
                if (current.briefValidated || current.concepts.isNotEmpty()) {
                    updateProjectSync { it.copy(currentStep = WorkflowStep.CONCEPTS) }
                } else {
                    val missing = current.brief.validationMissingMessage()
                    if (missing == null) {
                        validateBrief(goToNextStep = true)
                    } else {
                        aiOpState.value = AiOperationState.Error(missing, null, null)
                    }
                }
            }

            WorkflowStep.SCRIPT -> {
                if (current.conceptsValidated || current.script != null) {
                    updateProjectSync { it.copy(currentStep = WorkflowStep.SCRIPT) }
                } else {
                    aiOpState.value = AiOperationState.Error(
                        "Sélectionnez et validez d'abord un concept dans l'étape 2.",
                        null,
                        null
                    )
                }
            }

            WorkflowStep.STORYBOARD -> {
                if (current.scriptValidated || current.storyboard.isNotEmpty()) {
                    updateProjectSync { it.copy(currentStep = WorkflowStep.STORYBOARD) }
                } else {
                    aiOpState.value = AiOperationState.Error(
                        "Générez et validez d'abord le script dans l'étape 3.",
                        null,
                        null
                    )
                }
            }
        }
    }

    fun updateProjectTitle(newTitle: String) {
        updateProjectSync { it.copy(title = newTitle) }
    }

    fun updateBrief(newBrief: AdBrief) {
        updateProjectSync { current ->
            val updatedTitle = if (current.title == "Nouveau projet" || current.title == "Nouvelle publicité") {
                newBrief.productName.ifBlank { current.title }
            } else {
                current.title
            }
            current.copy(title = updatedTitle, brief = newBrief)
        }
    }

    fun importProductImage(uri: Uri) {
        viewModelScope.launch {
            val current = _project.value ?: _project.filterNotNull().first()
            val oldPath = current.brief.productImagePath
            val newPath = repository.importProductImage(current.id, uri)
            if (newPath == null) {
                aiOpState.value = AiOperationState.Error(
                    "Impossible d'importer l'image sélectionnée depuis la galerie. Vérifiez le format du fichier.",
                    null,
                    null
                )
                return@launch
            }
            updateProjectSync { latest ->
                latest.copy(brief = latest.brief.copy(productImagePath = newPath))
            }
            if (oldPath != null && oldPath != newPath) {
                repository.removeProductImage(oldPath)
            }
            aiOpState.value = AiOperationState.SuccessBanner(
                "Image du produit importée et prête pour l'analyse multimodale Gemini."
            )
        }
    }

    fun removeProductImage() {
        val current = _project.value ?: return
        val oldPath = current.brief.productImagePath ?: return
        updateProjectSync { latest ->
            latest.copy(brief = latest.brief.copy(productImagePath = null))
        }
        repository.removeProductImage(oldPath)
        aiOpState.value = AiOperationState.SuccessBanner("Image du produit retirée.")
    }

    fun validateBrief(goToNextStep: Boolean = true) {
        val current = _project.value ?: return
        val missing = current.brief.validationMissingMessage()
        if (missing != null) {
            aiOpState.value = AiOperationState.Error(
                message = missing,
                fallbackModelId = null,
                retryAction = null
            )
            return
        }
        val next = updateProjectSync {
            it.copy(
                briefValidated = true,
                currentStep = if (goToNextStep) WorkflowStep.CONCEPTS else it.currentStep
            )
        } ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveVersionSnapshot(next, "Brief validé")
            }
            aiOpState.value = AiOperationState.SuccessBanner("Brief validé ! Étape Concepts débloquée.")
        }
    }

    fun requestAiOperation(operation: PendingAiOperation) {
        val current = _project.value ?: return
        if (operation is PendingAiOperation.GenerateConcepts) {
            val missing = current.brief.validationMissingMessage()
            if (missing != null) {
                aiOpState.value = AiOperationState.Error(missing, null, null)
                return
            }
        }
        val settings = uiState.value.aiSettings
        val primarySpec = GeminiModelCatalog.findById(settings.primaryModelId)
        if (settings.economicModeEnabled || primarySpec.requiresBilling) {
            pendingConfirmation.value = operation
        } else {
            executeAiOperation(operation, settings.primaryModelId)
        }
    }

    fun confirmPendingAiOperation() {
        val op = pendingConfirmation.value ?: return
        pendingConfirmation.value = null
        val modelId = uiState.value.aiSettings.primaryModelId
        executeAiOperation(op, modelId)
    }

    fun cancelPendingAiOperation() {
        pendingConfirmation.value = null
    }

    fun retryWithFallback(operation: PendingAiOperation, fallbackModelId: String) {
        executeAiOperation(operation, fallbackModelId)
    }

    private fun executeAiOperation(operation: PendingAiOperation, modelId: String) {
        val current = _project.value ?: return
        val fallbackId = uiState.value.aiSettings.fallbackModelId?.takeIf { it != modelId }
        viewModelScope.launch {
            aiOpState.value = AiOperationState.Loading(operation.title, modelId)
            when (operation) {
                is PendingAiOperation.GenerateConcepts -> {
                    val missing = current.brief.validationMissingMessage()
                    if (missing != null) {
                        aiOpState.value = AiOperationState.Error(missing, null, null)
                        return@launch
                    }
                    when (val res = repository.generateConcepts(current.brief, modelId)) {
                        is AiOutcome.Success -> {
                            if (current.concepts.isNotEmpty()) {
                                saveMutex.withLock {
                                    repository.saveVersionSnapshot(current, "Avant régénération des concepts")
                                }
                            }
                            updateProjectSync {
                                it.copy(
                                    briefValidated = true,
                                    currentStep = WorkflowStep.CONCEPTS,
                                    concepts = res.data,
                                    selectedConceptId = res.data.firstOrNull()?.id
                                )
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "3 concepts générés avec ${res.modelUsed} (${res.latencyMs} ms)."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }

                is PendingAiOperation.RegenerateConcept -> {
                    val existing = current.concepts
                    val others = existing.filterIndexed { idx, _ -> idx != operation.index }
                    when (val res = repository.regenerateSingleConcept(current.brief, others, modelId)) {
                        is AiOutcome.Success -> {
                            saveMutex.withLock {
                                repository.saveVersionSnapshot(current, "Avant régénération concept #${operation.index + 1}")
                            }
                            updateProjectSync { latest ->
                                val mutable = latest.concepts.toMutableList()
                                if (operation.index in mutable.indices) {
                                    val wasSelected = mutable[operation.index].id == latest.selectedConceptId
                                    mutable[operation.index] = res.data
                                    latest.copy(
                                        concepts = mutable,
                                        selectedConceptId = if (wasSelected) res.data.id else latest.selectedConceptId
                                    )
                                } else {
                                    latest
                                }
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "Concept régénéré via ${res.modelUsed}."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }

                is PendingAiOperation.GenerateScript -> {
                    val selected = current.selectedConcept
                    if (selected == null) {
                        aiOpState.value = AiOperationState.Error("Sélectionnez d'abord un concept.", null, null)
                        return@launch
                    }
                    when (val res = repository.generateScript(current.brief, selected, modelId)) {
                        is AiOutcome.Success -> {
                            if (current.script != null) {
                                saveMutex.withLock {
                                    repository.saveVersionSnapshot(current, "Avant régénération du script")
                                }
                            }
                            updateProjectSync {
                                it.copy(
                                    conceptsValidated = true,
                                    currentStep = WorkflowStep.SCRIPT,
                                    script = res.data
                                )
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "Script généré (${res.data.totalDurationSeconds}s) via ${res.modelUsed}."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }

                is PendingAiOperation.RegenerateScriptSection -> {
                    val selected = current.selectedConcept
                    val script = current.script
                    if (selected == null || script == null) {
                        aiOpState.value = AiOperationState.Error("Concept ou script introuvable.", null, null)
                        return@launch
                    }
                    when (val res = repository.regenerateScriptSection(
                        current.brief,
                        selected,
                        script,
                        operation.section,
                        modelId
                    )) {
                        is AiOutcome.Success -> {
                            saveMutex.withLock {
                                repository.saveVersionSnapshot(current, "Avant régénération ${operation.section.type.title}")
                            }
                            updateProjectSync { latest ->
                                val latestScript = latest.script ?: return@updateProjectSync latest
                                val updatedSections = latestScript.sections.map {
                                    if (it.id == operation.section.id) res.data else it
                                }
                                latest.copy(script = latestScript.copy(sections = updatedSections))
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "Section « ${operation.section.type.title} » régénérée."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }

                is PendingAiOperation.GenerateStoryboard -> {
                    val selected = current.selectedConcept
                    val script = current.script
                    if (selected == null || script == null) {
                        aiOpState.value = AiOperationState.Error("Validez un concept et un script avant le storyboard.", null, null)
                        return@launch
                    }
                    when (val res = repository.generateStoryboard(current.brief, selected, script, modelId)) {
                        is AiOutcome.Success -> {
                            if (current.storyboard.isNotEmpty()) {
                                saveMutex.withLock {
                                    repository.saveVersionSnapshot(current, "Avant régénération du storyboard")
                                }
                            }
                            updateProjectSync {
                                it.copy(
                                    scriptValidated = true,
                                    currentStep = WorkflowStep.STORYBOARD,
                                    storyboard = res.data
                                )
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "Storyboard généré (${res.data.size} scènes) via ${res.modelUsed}."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }

                is PendingAiOperation.RegenerateScene -> {
                    val selected = current.selectedConcept
                    val target = current.storyboard.getOrNull(operation.index)
                    if (selected == null || target == null) {
                        aiOpState.value = AiOperationState.Error("Scène ou concept introuvable.", null, null)
                        return@launch
                    }
                    when (val res = repository.regenerateSingleScene(
                        current.brief,
                        selected,
                        target,
                        current.storyboard,
                        modelId
                    )) {
                        is AiOutcome.Success -> {
                            saveMutex.withLock {
                                repository.saveVersionSnapshot(current, "Avant régénération scène #${target.sceneNumber}")
                            }
                            updateProjectSync { latest ->
                                val mutable = latest.storyboard.toMutableList()
                                if (operation.index in mutable.indices) {
                                    mutable[operation.index] = res.data
                                }
                                latest.copy(storyboard = repository.recalculateTimeline(mutable))
                            }
                            aiOpState.value = AiOperationState.SuccessBanner(
                                "Scène #${target.sceneNumber} régénérée."
                            )
                        }
                        is AiOutcome.Failure -> {
                            aiOpState.value = AiOperationState.Error(res.message, fallbackId, operation)
                        }
                    }
                }
            }
        }
    }

    // --- Manual Concept Actions ---
    fun selectConcept(conceptId: String) {
        updateProjectSync { it.copy(selectedConceptId = conceptId) }
    }

    fun updateConcept(edited: AdConcept) {
        updateProjectSync { current ->
            val updatedList = current.concepts.map { if (it.id == edited.id) edited else it }
            current.copy(concepts = updatedList)
        }
    }

    fun validateConcepts(goToNextStep: Boolean = true) {
        val current = _project.value ?: return
        if (current.selectedConcept == null) {
            aiOpState.value = AiOperationState.Error("Sélectionnez un concept avant de valider.", null, null)
            return
        }
        val next = updateProjectSync {
            it.copy(
                conceptsValidated = true,
                selectedConceptId = it.selectedConcept?.id,
                currentStep = if (goToNextStep) WorkflowStep.SCRIPT else it.currentStep
            )
        } ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveVersionSnapshot(next, "Concept « ${next.selectedConcept?.name} » validé")
            }
            aiOpState.value = AiOperationState.SuccessBanner("Concept validé.")
        }
    }

    // --- Manual Script Actions ---
    fun updateScriptSection(edited: ScriptSection) {
        updateProjectSync { current ->
            val script = current.script ?: return@updateProjectSync current
            val updatedSections = script.sections.map { if (it.id == edited.id) edited else it }
            current.copy(script = script.copy(sections = updatedSections))
        }
    }

    fun updateDirectorNotes(notes: String) {
        updateProjectSync { current ->
            val script = current.script ?: return@updateProjectSync current
            current.copy(script = script.copy(directorNotes = notes))
        }
    }

    fun validateScript(goToNextStep: Boolean = true) {
        val current = _project.value ?: return
        if (current.script == null || current.script.sections.isEmpty()) {
            aiOpState.value = AiOperationState.Error("Générez d'abord un script avant de valider.", null, null)
            return
        }
        val next = updateProjectSync {
            it.copy(
                scriptValidated = true,
                currentStep = if (goToNextStep) WorkflowStep.STORYBOARD else it.currentStep
            )
        } ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveVersionSnapshot(next, "Script validé")
            }
            aiOpState.value = AiOperationState.SuccessBanner("Script validé et sauvegardé.")
        }
    }

    // --- Manual Storyboard Actions ---
    fun updateScene(edited: StoryboardScene) {
        updateProjectSync { current ->
            val replaced = current.storyboard.map { if (it.id == edited.id) edited else it }
            current.copy(storyboard = repository.recalculateTimeline(replaced))
        }
    }

    fun addScene() {
        updateProjectSync { current ->
            val newScene = StoryboardScene(
                id = UUID.randomUUID().toString(),
                sceneNumber = current.storyboard.size + 1,
                durationSec = 3,
                narrativeObjective = "Nouvelle séquence",
                visualDescription = "À définir",
                onScreenText = "",
                narration = "",
                cameraMovement = "Plan fixe ou léger push-in",
                animation = "Apparition fluide 300ms",
                transition = "Cut dynamique",
                soundEffects = "Whoosh léger",
                directingInstructions = "Respecter la charte visuelle"
            )
            current.copy(storyboard = repository.recalculateTimeline(current.storyboard + newScene))
        }
    }

    fun deleteScene(sceneId: String) {
        updateProjectSync { current ->
            val filtered = current.storyboard.filterNot { it.id == sceneId }
            current.copy(storyboard = repository.recalculateTimeline(filtered))
        }
    }

    fun moveScene(index: Int, delta: Int) {
        updateProjectSync { current ->
            val targetIdx = index + delta
            if (index !in current.storyboard.indices || targetIdx !in current.storyboard.indices) {
                return@updateProjectSync current
            }
            val mutable = current.storyboard.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(targetIdx, item)
            current.copy(storyboard = repository.recalculateTimeline(mutable))
        }
    }

    fun alignStoryboardDuration() {
        val updated = updateProjectSync { current ->
            current.copy(
                storyboard = repository.alignTimelineToDuration(
                    current.storyboard,
                    current.brief.durationSeconds
                )
            )
        } ?: return
        aiOpState.value = AiOperationState.SuccessBanner(
            "Timeline alignée sur ${updated.brief.durationSeconds}s."
        )
    }

    fun validateStoryboard() {
        val current = _project.value ?: return
        if (current.storyboard.isEmpty()) {
            aiOpState.value = AiOperationState.Error("Ajoutez ou générez au moins une scène.", null, null)
            return
        }
        val total = current.storyboard.sumOf { it.durationSec }
        if (total != current.brief.durationSeconds) {
            aiOpState.value = AiOperationState.Error(
                "La durée totale du storyboard (${total}s) doit correspondre exactement à la durée du brief (${current.brief.durationSeconds}s). Utilisez l'alignement automatique ou ajustez les scènes.",
                null,
                null
            )
            return
        }
        val next = updateProjectSync { it.copy(storyboardValidated = true) } ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveVersionSnapshot(next, "Storyboard validé (${total}s)")
            }
            aiOpState.value = AiOperationState.SuccessBanner("Storyboard validé ! Dossier Phase 1–2 prêt.")
        }
    }

    fun saveManualSnapshot(label: String) {
        val current = _project.value ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                repository.saveProject(current)
                repository.saveVersionSnapshot(current, label.ifBlank { "Sauvegarde manuelle" })
            }
            aiOpState.value = AiOperationState.SuccessBanner("Instantané enregistré dans l'historique.")
        }
    }

    fun restoreVersion(version: ProjectVersion) {
        val current = _project.value ?: return
        viewModelScope.launch {
            val restored = saveMutex.withLock {
                repository.restoreVersion(current, version)
            }
            _project.value = restored
            aiOpState.value = AiOperationState.SuccessBanner("Version « ${version.label} » restaurée.")
        }
    }

    fun buildExportSummary(): String {
        val current = _project.value ?: return ""
        return repository.buildExportDocument(current)
    }

    class Factory(
        private val projectId: String,
        private val repository: ProjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectWorkspaceViewModel(projectId, repository) as T
        }
    }
}
