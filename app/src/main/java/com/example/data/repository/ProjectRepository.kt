package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.ai.AiOutcome
import com.example.data.ai.GeminiStudioService
import com.example.data.local.ProductImageStorage
import com.example.data.local.ProjectVersionEntity
import com.example.data.local.StudioDao
import com.example.data.local.applySnapshot
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AdBrief
import com.example.data.model.AdConcept
import com.example.data.model.AdScript
import com.example.data.model.AiCallLog
import com.example.data.model.AiSettings
import com.example.data.model.MotionProject
import com.example.data.model.ProjectVersion
import com.example.data.model.ScriptSection
import com.example.data.model.StoryboardScene
import com.example.data.preferences.AiSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(
    private val dao: StudioDao,
    private val aiService: GeminiStudioService,
    private val settingsStore: AiSettingsStore,
    private val appContext: Context? = null
) {
    val allProjects: Flow<List<MotionProject>> = dao.observeAllProjects()
        .map { entities -> entities.map { it.toDomain() } }

    val aiSettings: Flow<AiSettings> = settingsStore.settingsFlow

    fun observeProject(projectId: String): Flow<MotionProject?> =
        dao.observeProjectById(projectId).map { it?.toDomain() }

    fun observeVersions(projectId: String): Flow<List<ProjectVersion>> =
        dao.observeVersionsForProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun createProject(title: String, brief: AdBrief = AdBrief()): MotionProject {
        val now = System.currentTimeMillis()
        val cleanTitle = title.ifBlank {
            brief.productName.ifBlank { "Nouvelle publicité" }
        }
        val project = MotionProject(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            createdAt = now,
            updatedAt = now,
            brief = brief
        )
        dao.upsertProject(project.toEntity())
        return project
    }

    suspend fun saveProject(project: MotionProject) {
        dao.upsertProject(project.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun duplicateProject(source: MotionProject): MotionProject {
        val now = System.currentTimeMillis()
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            title = "${source.title} (Copie)",
            createdAt = now,
            updatedAt = now
        )
        dao.upsertProject(copy.toEntity())
        return copy
    }

    suspend fun deleteProject(projectId: String) {
        val existing = dao.observeProjectById(projectId).first()?.toDomain()
        val imagePath = existing?.brief?.productImagePath
        dao.deleteProjectById(projectId)
        if (!imagePath.isNullOrBlank()) {
            val stillReferenced = dao.observeAllProjects().first().any {
                it.id != projectId && it.toDomain().brief.productImagePath == imagePath
            }
            if (!stillReferenced) {
                ProductImageStorage.deleteFile(imagePath)
            }
        }
    }

    suspend fun importProductImage(projectId: String, uri: Uri): String? {
        val ctx = appContext ?: return null
        return ProductImageStorage.saveFromUri(ctx, projectId, uri)
    }

    fun removeProductImage(path: String?) {
        ProductImageStorage.deleteFile(path)
    }

    private suspend fun loadProductBitmap(brief: AdBrief): Bitmap? {
        val path = brief.productImagePath ?: return null
        return withContext(Dispatchers.IO) {
            ProductImageStorage.loadBitmap(path)
        }
    }

    suspend fun saveVersionSnapshot(project: MotionProject, label: String) {
        val version = ProjectVersionEntity(
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            createdAt = System.currentTimeMillis(),
            label = label,
            stepName = project.currentStep.label,
            snapshotJson = project.toSnapshotJson()
        )
        dao.insertVersion(version)
    }

    suspend fun restoreVersion(project: MotionProject, version: ProjectVersion): MotionProject {
        saveVersionSnapshot(project, "Avant restauration (${version.label})")
        val restored = project.applySnapshot(version.snapshotJson)
        saveProject(restored)
        return restored
    }

    suspend fun setPrimaryModel(modelId: String) = settingsStore.setPrimaryModel(modelId)

    suspend fun setFallbackModel(modelId: String?) = settingsStore.setFallbackModel(modelId)

    suspend fun setEconomicMode(enabled: Boolean) = settingsStore.setEconomicMode(enabled)

    suspend fun clearCallLogs() = settingsStore.clearCallLogs()

    suspend fun testGeminiConnection(modelId: String): AiOutcome<String> {
        val outcome = aiService.testConnection(modelId)
        logOutcome("Test connexion", outcome)
        return outcome
    }

    suspend fun generateConcepts(brief: AdBrief, modelId: String): AiOutcome<List<AdConcept>> {
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.generateThreeConcepts(brief, modelId, bitmap)
        logOutcome("Génération 3 concepts", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    suspend fun regenerateSingleConcept(
        brief: AdBrief,
        otherConcepts: List<AdConcept>,
        modelId: String
    ): AiOutcome<AdConcept> {
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.regenerateSingleConcept(brief, otherConcepts, modelId, bitmap)
        logOutcome("Régénération 1 concept", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    suspend fun generateScript(
        brief: AdBrief,
        concept: AdConcept,
        modelId: String
    ): AiOutcome<AdScript> {
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.generateScript(brief, concept, modelId, bitmap)
        logOutcome("Génération script", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    suspend fun regenerateScriptSection(
        brief: AdBrief,
        concept: AdConcept,
        currentScript: AdScript,
        targetSection: ScriptSection,
        modelId: String
    ): AiOutcome<ScriptSection> {
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.regenerateScriptSection(
            brief,
            concept,
            currentScript,
            targetSection,
            modelId,
            bitmap
        )
        logOutcome("Régénération section ${targetSection.type.title}", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    suspend fun generateStoryboard(
        brief: AdBrief,
        concept: AdConcept,
        script: AdScript,
        modelId: String
    ): AiOutcome<List<StoryboardScene>> {
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.generateStoryboard(brief, concept, script, modelId, bitmap)
        if (outcome is AiOutcome.Success) {
            val aligned = alignTimelineToDuration(outcome.data, brief.durationSeconds)
            val updated = outcome.copy(data = aligned)
            logOutcome("Génération storyboard", updated, hasProductImage = bitmap != null)
            return updated
        }
        logOutcome("Génération storyboard", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    suspend fun regenerateSingleScene(
        brief: AdBrief,
        concept: AdConcept,
        targetScene: StoryboardScene,
        allScenes: List<StoryboardScene>,
        modelId: String
    ): AiOutcome<StoryboardScene> {
        val summary = allScenes.joinToString("\n") {
            "Scène ${it.sceneNumber} (${it.startTimeSec}-${it.endTimeSec}s): ${it.narrativeObjective}"
        }
        val bitmap = loadProductBitmap(brief)
        val outcome = aiService.regenerateSingleScene(brief, concept, targetScene, summary, modelId, bitmap)
        logOutcome("Régénération scène #${targetScene.sceneNumber}", outcome, hasProductImage = bitmap != null)
        return outcome
    }

    private suspend fun <T> logOutcome(
        actionName: String,
        outcome: AiOutcome<T>,
        hasProductImage: Boolean = false
    ) {
        val suffix = if (hasProductImage) " • avec image produit" else ""
        val log = when (outcome) {
            is AiOutcome.Success -> AiCallLog(
                actionName = actionName,
                modelId = outcome.modelUsed,
                success = true,
                latencyMs = outcome.latencyMs,
                note = "Succès$suffix"
            )
            is AiOutcome.Failure -> AiCallLog(
                actionName = actionName,
                modelId = outcome.modelAttempted,
                success = false,
                latencyMs = outcome.latencyMs,
                note = outcome.message.take(140)
            )
        }
        settingsStore.recordCall(log)
    }

    fun recalculateTimeline(scenes: List<StoryboardScene>): List<StoryboardScene> {
        var cursor = 0
        return scenes.mapIndexed { idx, scene ->
            val dur = scene.durationSec.coerceAtLeast(1)
            val start = cursor
            val end = cursor + dur
            cursor = end
            scene.copy(
                sceneNumber = idx + 1,
                startTimeSec = start,
                endTimeSec = end,
                durationSec = dur
            )
        }
    }

    fun alignTimelineToDuration(
        scenes: List<StoryboardScene>,
        targetDurationSec: Int
    ): List<StoryboardScene> {
        if (scenes.isEmpty() || targetDurationSec <= 0) return recalculateTimeline(scenes)
        val mutable = scenes.map { it.copy(durationSec = it.durationSec.coerceAtLeast(1)) }.toMutableList()
        var currentTotal = mutable.sumOf { it.durationSec }
        var idx = mutable.lastIndex

        // Proportionally or step-by-step adjust durations without dropping any scene below 1s
        var guard = 0
        while (currentTotal != targetDurationSec && guard < 500) {
            guard++
            if (currentTotal < targetDurationSec) {
                val target = mutable[idx]
                mutable[idx] = target.copy(durationSec = target.durationSec + 1)
                currentTotal++
            } else {
                val target = mutable[idx]
                if (target.durationSec > 1) {
                    mutable[idx] = target.copy(durationSec = target.durationSec - 1)
                    currentTotal--
                }
            }
            idx = if (idx - 1 < 0) mutable.lastIndex else idx - 1
            if (currentTotal > targetDurationSec && mutable.all { it.durationSec <= 1 }) {
                break
            }
        }
        return recalculateTimeline(mutable)
    }

    fun buildExportDocument(project: MotionProject): String = buildString {
        appendLine("====================================================")
        appendLine("MOTION STUDIO AI — DOSSIER DE PRODUCTION")
        appendLine("====================================================")
        appendLine("Projet : ${project.title}")
        appendLine("Statut : ${project.status.label}")
        appendLine()
        appendLine("--- 1. BRIEF COMMERCIAL ---")
        appendLine("Produit : ${project.brief.productName}")
        appendLine("Secteur : ${project.brief.sector}")
        appendLine("Plateforme : ${project.brief.platform}")
        appendLine("Durée cible : ${project.brief.durationSeconds}s")
        appendLine("Objectif : ${project.brief.commercialObjective}")
        appendLine("Cible : ${project.brief.targetAudience}")
        appendLine("Description : ${project.brief.description}")
        appendLine("Caractéristiques : ${project.brief.features}")
        appendLine("Avantages : ${project.brief.benefits}")
        if (!project.brief.productImagePath.isNullOrBlank()) {
            appendLine("Visuel produit : Image importée (${project.brief.productImagePath.substringAfterLast('/')})")
        }
        appendLine()

        project.selectedConcept?.let { c ->
            appendLine("--- 2. CONCEPT RETENU ---")
            appendLine("Nom : ${c.name}")
            appendLine("Angle marketing : ${c.marketingAngle}")
            appendLine("Idée créative : ${c.creativeIdea}")
            appendLine("Accroche : ${c.hook}")
            appendLine("Promesse commerciale : ${c.commercialPromise}")
            appendLine("Ambiance visuelle : ${c.visualMood}")
            appendLine("Style de réalisation : ${c.directingStyle}")
            appendLine("Déroulement général : ${c.generalFlow}")
            appendLine()
        }

        project.script?.let { s ->
            appendLine("--- 3. SCRIPT PUBLICITAIRE (${s.totalDurationSeconds}s) ---")
            if (s.directorNotes.isNotBlank()) appendLine("Notes : ${s.directorNotes}")
            s.sections.forEach { sec ->
                appendLine("[${sec.type.title} - ${sec.durationSeconds}s]")
                appendLine("  Voix off : ${sec.voiceOver}")
                appendLine("  Texte écran : ${sec.onScreenText}")
            }
            appendLine()
        }

        if (project.storyboard.isNotEmpty()) {
            val total = project.storyboard.sumOf { it.durationSec }
            appendLine("--- 4. STORYBOARD SECONDE PAR SECONDE (${total}s) ---")
            project.storyboard.forEach { sc ->
                appendLine("Scène ${sc.sceneNumber} [${sc.startTimeSec}s -> ${sc.endTimeSec}s | ${sc.durationSec}s]")
                appendLine("  Objectif narratif : ${sc.narrativeObjective}")
                appendLine("  Visuel : ${sc.visualDescription}")
                appendLine("  Texte écran : ${sc.onScreenText}")
                appendLine("  Narration : ${sc.narration}")
                appendLine("  Caméra : ${sc.cameraMovement}")
                appendLine("  Animation : ${sc.animation}")
                appendLine("  Transition : ${sc.transition}")
                appendLine("  Effets sonores : ${sc.soundEffects}")
                appendLine("  Réalisation : ${sc.directingInstructions}")
                appendLine()
            }
        }
    }
}
