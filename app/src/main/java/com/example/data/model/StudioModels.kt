package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class WorkflowStep(val index: Int, val label: String) {
    BRIEF(0, "Brief"),
    CONCEPTS(1, "Concepts"),
    SCRIPT(2, "Script"),
    STORYBOARD(3, "Storyboard")
}

enum class ProjectStatus(val label: String) {
    DRAFT("Brouillon"),
    IN_PROGRESS("En cours"),
    VALIDATED("Validé")
}

enum class ScriptSectionType(val title: String, val badge: String) {
    HOOK("Accroche", "01 • HOOK"),
    PRODUCT_INTRO("Présentation du produit", "02 • PRODUIT"),
    COMMERCIAL_ARGUMENT("Argument commercial", "03 • ARGUMENT"),
    BENEFITS_HIGHLIGHT("Mise en valeur des avantages", "04 • AVANTAGES"),
    CALL_TO_ACTION("Appel à l'action", "05 • CTA");

    companion object {
        fun fromNameOrDefault(value: String, fallbackIndex: Int): ScriptSectionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.getOrElse(fallbackIndex) { HOOK }
        }
    }
}

data class AdBrief(
    val productName: String = "",
    val sector: String = "",
    val description: String = "",
    val features: String = "",
    val benefits: String = "",
    val targetAudience: String = "",
    val commercialObjective: String = "Conversion & Ventes",
    val durationSeconds: Int = 30,
    val platform: String = "TikTok / Reels (9:16)",
    val productImagePath: String? = null
) {
    fun isReadyForGeneration(): Boolean {
        return validationMissingMessage() == null
    }

    fun validationMissingMessage(): String? = when {
        productName.isBlank() ->
            "Renseignez le nom du produit pour valider le brief."
        description.isBlank() && features.isBlank() && benefits.isBlank() ->
            "Ajoutez une description du produit (ou ses caractéristiques / avantages)."
        durationSeconds !in 5..180 ->
            "La durée de la vidéo doit être comprise entre 5 et 180 secondes."
        else -> null
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("productName", productName)
        put("sector", sector)
        put("description", description)
        put("features", features)
        put("benefits", benefits)
        put("targetAudience", targetAudience)
        put("commercialObjective", commercialObjective)
        put("durationSeconds", durationSeconds)
        put("platform", platform)
        put("productImagePath", productImagePath ?: "")
    }

    companion object {
        fun fromJson(json: JSONObject?): AdBrief {
            if (json == null) return AdBrief()
            return AdBrief(
                productName = json.optString("productName", ""),
                sector = json.optString("sector", ""),
                description = json.optString("description", ""),
                features = json.optString("features", ""),
                benefits = json.optString("benefits", ""),
                targetAudience = json.optString("targetAudience", ""),
                commercialObjective = json.optString("commercialObjective", "Conversion & Ventes"),
                durationSeconds = json.optInt("durationSeconds", 30).coerceIn(5, 180),
                platform = json.optString("platform", "TikTok / Reels (9:16)"),
                productImagePath = json.optString("productImagePath", "").ifBlank { null }
            )
        }
    }
}

data class AdConcept(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val marketingAngle: String = "",
    val creativeIdea: String = "",
    val hook: String = "",
    val commercialPromise: String = "",
    val visualMood: String = "",
    val directingStyle: String = "",
    val generalFlow: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("marketingAngle", marketingAngle)
        put("creativeIdea", creativeIdea)
        put("hook", hook)
        put("commercialPromise", commercialPromise)
        put("visualMood", visualMood)
        put("directingStyle", directingStyle)
        put("generalFlow", generalFlow)
    }

    companion object {
        fun fromJson(json: JSONObject): AdConcept {
            return AdConcept(
                id = json.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                name = json.optString("name", ""),
                marketingAngle = json.optString("marketingAngle", ""),
                creativeIdea = json.optString("creativeIdea", ""),
                hook = json.optString("hook", ""),
                commercialPromise = json.optString("commercialPromise", ""),
                visualMood = json.optString("visualMood", ""),
                directingStyle = json.optString("directingStyle", ""),
                generalFlow = json.optString("generalFlow", "")
            )
        }
    }
}

data class ScriptSection(
    val id: String = UUID.randomUUID().toString(),
    val type: ScriptSectionType = ScriptSectionType.HOOK,
    val voiceOver: String = "",
    val onScreenText: String = "",
    val durationSeconds: Int = 5
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("voiceOver", voiceOver)
        put("onScreenText", onScreenText)
        put("durationSeconds", durationSeconds)
    }

    companion object {
        fun fromJson(json: JSONObject, index: Int = 0): ScriptSection {
            return ScriptSection(
                id = json.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                type = ScriptSectionType.fromNameOrDefault(json.optString("type", ""), index),
                voiceOver = json.optString("voiceOver", ""),
                onScreenText = json.optString("onScreenText", ""),
                durationSeconds = json.optInt("durationSeconds", 5).coerceAtLeast(1)
            )
        }
    }
}

data class AdScript(
    val sections: List<ScriptSection> = emptyList(),
    val directorNotes: String = ""
) {
    val totalDurationSeconds: Int
        get() = sections.sumOf { it.durationSeconds }

    fun toJson(): JSONObject = JSONObject().apply {
        put("directorNotes", directorNotes)
        val arr = JSONArray()
        sections.forEach { arr.put(it.toJson()) }
        put("sections", arr)
    }

    companion object {
        fun fromJson(json: JSONObject?): AdScript? {
            if (json == null) return null
            val arr = json.optJSONArray("sections") ?: return null
            val sections = (0 until arr.length()).map { idx ->
                ScriptSection.fromJson(arr.getJSONObject(idx), idx)
            }
            if (sections.isEmpty()) return null
            return AdScript(
                sections = sections,
                directorNotes = json.optString("directorNotes", "")
            )
        }
    }
}

data class StoryboardScene(
    val id: String = UUID.randomUUID().toString(),
    val sceneNumber: Int = 1,
    val startTimeSec: Int = 0,
    val endTimeSec: Int = 5,
    val durationSec: Int = 5,
    val narrativeObjective: String = "",
    val visualDescription: String = "",
    val onScreenText: String = "",
    val narration: String = "",
    val cameraMovement: String = "",
    val animation: String = "",
    val transition: String = "",
    val soundEffects: String = "",
    val directingInstructions: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sceneNumber", sceneNumber)
        put("startTimeSec", startTimeSec)
        put("endTimeSec", endTimeSec)
        put("durationSec", durationSec)
        put("narrativeObjective", narrativeObjective)
        put("visualDescription", visualDescription)
        put("onScreenText", onScreenText)
        put("narration", narration)
        put("cameraMovement", cameraMovement)
        put("animation", animation)
        put("transition", transition)
        put("soundEffects", soundEffects)
        put("directingInstructions", directingInstructions)
    }

    companion object {
        fun fromJson(json: JSONObject, fallbackNumber: Int = 1): StoryboardScene {
            val dur = json.optInt("durationSec", 5).coerceAtLeast(1)
            val start = json.optInt("startTimeSec", 0).coerceAtLeast(0)
            val end = json.optInt("endTimeSec", start + dur).coerceAtLeast(start + 1)
            return StoryboardScene(
                id = json.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                sceneNumber = json.optInt("sceneNumber", fallbackNumber),
                startTimeSec = start,
                endTimeSec = end,
                durationSec = dur,
                narrativeObjective = json.optString("narrativeObjective", ""),
                visualDescription = json.optString("visualDescription", ""),
                onScreenText = json.optString("onScreenText", ""),
                narration = json.optString("narration", ""),
                cameraMovement = json.optString("cameraMovement", ""),
                animation = json.optString("animation", ""),
                transition = json.optString("transition", ""),
                soundEffects = json.optString("soundEffects", ""),
                directingInstructions = json.optString("directingInstructions", "")
            )
        }
    }
}

data class MotionProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Nouveau projet",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val currentStep: WorkflowStep = WorkflowStep.BRIEF,
    val briefValidated: Boolean = false,
    val conceptsValidated: Boolean = false,
    val scriptValidated: Boolean = false,
    val storyboardValidated: Boolean = false,
    val brief: AdBrief = AdBrief(),
    val concepts: List<AdConcept> = emptyList(),
    val selectedConceptId: String? = null,
    val script: AdScript? = null,
    val storyboard: List<StoryboardScene> = emptyList()
) {
    val selectedConcept: AdConcept?
        get() = concepts.firstOrNull { it.id == selectedConceptId } ?: concepts.firstOrNull()

    val status: ProjectStatus
        get() = when {
            storyboardValidated -> ProjectStatus.VALIDATED
            briefValidated || concepts.isNotEmpty() || script != null -> ProjectStatus.IN_PROGRESS
            else -> ProjectStatus.DRAFT
        }

    val validatedStepsCount: Int
        get() = listOf(briefValidated, conceptsValidated, scriptValidated, storyboardValidated).count { it }

    fun toSnapshotJson(): String {
        val root = JSONObject().apply {
            put("title", title)
            put("currentStep", currentStep.name)
            put("briefValidated", briefValidated)
            put("conceptsValidated", conceptsValidated)
            put("scriptValidated", scriptValidated)
            put("storyboardValidated", storyboardValidated)
            put("brief", brief.toJson())
            put("selectedConceptId", selectedConceptId ?: "")
            val conceptsArray = JSONArray()
            concepts.forEach { conceptsArray.put(it.toJson()) }
            put("concepts", conceptsArray)
            if (script != null) put("script", script.toJson())
            val scenesArray = JSONArray()
            storyboard.forEach { scenesArray.put(it.toJson()) }
            put("storyboard", scenesArray)
        }
        return root.toString()
    }
}

data class ProjectVersion(
    val id: String,
    val projectId: String,
    val createdAt: Long,
    val label: String,
    val stepName: String,
    val snapshotJson: String
)

data class GeminiModelSpec(
    val id: String,
    val displayName: String,
    val summary: String,
    val requiresBilling: Boolean,
    val quotaInfo: String,
    val costNotice: String,
    val capabilities: List<String>
)

object GeminiModelCatalog {
    const val DEFAULT_PRIMARY_ID = "gemini-3.8-flash"
    const val DEFAULT_FALLBACK_ID = "gemini-3.5-flash-lite"

    val supportedModels: List<GeminiModelSpec> = listOf(
        GeminiModelSpec(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            summary = "Modèle principal polyvalent : analyse de brief, concepts, scripts et storyboards JSON structurés.",
            requiresBilling = false,
            quotaInfo = "Quota gratuit Firebase Spark limité (RPM/RPD selon projet)",
            costNotice = "Aucune facturation requise sur quota gratuit",
            capabilities = listOf("Brief & Analyse", "JSON structuré", "Multimodal", "Scripts & Storyboards")
        ),
        GeminiModelSpec(
            id = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            summary = "Modèle ultra-rapide et sobre en quota : idéal comme modèle économique ou de secours.",
            requiresBilling = false,
            quotaInfo = "Quota gratuit Firebase Spark disponible (latence réduite)",
            costNotice = "Aucune facturation requise sur quota gratuit",
            capabilities = listOf("Itérations rapides", "JSON structuré", "Modèle de secours")
        ),
        GeminiModelSpec(
            id = "gemini-4-argon",
            displayName = "Gemini 4 Argon",
            summary = "Modèle haute capacité pour direction créative complexe et scénarisation avancée.",
            requiresBilling = true,
            quotaInfo = "Plan Firebase Blaze requis (pas de palier gratuit garanti)",
            costNotice = "Susceptible d'entraîner des frais Google Cloud / Blaze",
            capabilities = listOf("Raisonnement avancé", "Direction créative", "JSON structuré")
        )
    )

    fun findById(id: String?): GeminiModelSpec {
        return supportedModels.firstOrNull { it.id == id } ?: supportedModels.first()
    }
}

data class AiCallLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val modelId: String,
    val success: Boolean,
    val latencyMs: Long,
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("actionName", actionName)
        put("modelId", modelId)
        put("success", success)
        put("latencyMs", latencyMs)
        put("note", note)
    }

    companion object {
        fun fromJson(json: JSONObject): AiCallLog {
            return AiCallLog(
                id = json.optString("id", UUID.randomUUID().toString()),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                actionName = json.optString("actionName", ""),
                modelId = json.optString("modelId", ""),
                success = json.optBoolean("success", false),
                latencyMs = json.optLong("latencyMs", 0L),
                note = json.optString("note", "")
            )
        }
    }
}

data class AiSettings(
    val primaryModelId: String = GeminiModelCatalog.DEFAULT_PRIMARY_ID,
    val fallbackModelId: String? = GeminiModelCatalog.DEFAULT_FALLBACK_ID,
    val economicModeEnabled: Boolean = true,
    val sessionCallCount: Int = 0,
    val callLogs: List<AiCallLog> = emptyList()
) {
    val primaryModel: GeminiModelSpec
        get() = GeminiModelCatalog.findById(primaryModelId)

    val fallbackModel: GeminiModelSpec?
        get() = fallbackModelId?.let { id -> GeminiModelCatalog.supportedModels.firstOrNull { it.id == id } }
}
