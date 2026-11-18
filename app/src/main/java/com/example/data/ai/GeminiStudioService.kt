package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AdBrief
import com.example.data.model.AdConcept
import com.example.data.model.AdScript
import com.example.data.model.ScriptSection
import com.example.data.model.ScriptSectionType
import com.example.data.model.StoryboardScene
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiStudioService"
private const val TIMEOUT_MS = 60_000L

sealed interface AiOutcome<out T> {
    data class Success<T>(
        val data: T,
        val modelUsed: String,
        val latencyMs: Long
    ) : AiOutcome<T>

    data class Failure(
        val message: String,
        val modelAttempted: String,
        val latencyMs: Long,
        val isQuotaOrNetworkError: Boolean = false
    ) : AiOutcome<Nothing>
}

class GeminiStudioService {

    private fun buildTextModel(modelId: String): GenerativeModel {
        return Firebase.ai.generativeModel(modelName = modelId)
    }

    private fun buildStructuredModel(modelId: String, schema: Schema): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    suspend fun testConnection(modelId: String): AiOutcome<String> {
        val start = System.currentTimeMillis()
        return try {
            val model = buildTextModel(modelId)
            val response = withTimeout(30_000L) {
                model.generateContent("Réponds uniquement par : OK Motion Studio AI opérationnel.")
            }
            val elapsed = System.currentTimeMillis() - start
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                AiOutcome.Failure(
                    message = "Le modèle $modelId a renvoyé une réponse vide.",
                    modelAttempted = modelId,
                    latencyMs = elapsed
                )
            } else {
                AiOutcome.Success(text, modelId, elapsed)
            }
        } catch (e: TimeoutCancellationException) {
            AiOutcome.Failure(
                message = "Délai d'attente dépassé (30s) lors du test sur $modelId.",
                modelAttempted = modelId,
                latencyMs = System.currentTimeMillis() - start
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed on $modelId", e)
            AiOutcome.Failure(
                message = formatAiError(e, modelId),
                modelAttempted = modelId,
                latencyMs = System.currentTimeMillis() - start,
                isQuotaOrNetworkError = isQuotaError(e)
            )
        }
    }

    suspend fun generateThreeConcepts(
        brief: AdBrief,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<List<AdConcept>> {
        val conceptItemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string("Nom court et percutant du concept"),
                "marketingAngle" to Schema.string("Angle marketing stratégique"),
                "creativeIdea" to Schema.string("Idée créative centrale de la publicité"),
                "hook" to Schema.string("Accroche des 3 premières secondes"),
                "commercialPromise" to Schema.string("Promesse commerciale claire"),
                "visualMood" to Schema.string("Ambiance visuelle, palette, éclairage"),
                "directingStyle" to Schema.string("Style de réalisation et rythme motion design"),
                "generalFlow" to Schema.string("Déroulement général du début à la fin")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "concepts" to Schema.array(
                    items = conceptItemSchema,
                    description = "Exactement 3 concepts publicitaires distincts"
                )
            )
        )

        val prompt = """
            Tu es un directeur de création et motion designer senior.
            Analyse ce brief publicitaire et propose EXACTEMENT 3 concepts publicitaires distincts, réalisables en motion design / vidéo publicitaire professionnelle.
            Réponds uniquement en français.

            BRIEF COMMERCIAL :
            - Produit : ${brief.productName}
            - Secteur : ${brief.sector.ifBlank { "Non précisé" }}
            - Description : ${brief.description}
            - Caractéristiques : ${brief.features.ifBlank { "Non précisées" }}
            - Avantages : ${brief.benefits.ifBlank { "Non précisés" }}
            - Public cible : ${brief.targetAudience.ifBlank { "Grand public" }}
            - Objectif commercial : ${brief.commercialObjective}
            - Durée cible : ${brief.durationSeconds} secondes
            - Plateforme de diffusion : ${brief.platform}

            Exigence : chaque concept doit avoir une approche créative vraiment différente (ex: 1. Démonstration cinématique, 2. Storytelling émotionnel/problème-solution, 3. Typographie cinétique & rythme social).
        """.trimIndent()

        return executeStructured(modelId, rootSchema, prompt, productImage) { json ->
            val arr = json.getJSONArray("concepts")
            val list = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                AdConcept(
                    id = UUID.randomUUID().toString(),
                    name = obj.getString("name"),
                    marketingAngle = obj.getString("marketingAngle"),
                    creativeIdea = obj.getString("creativeIdea"),
                    hook = obj.getString("hook"),
                    commercialPromise = obj.getString("commercialPromise"),
                    visualMood = obj.getString("visualMood"),
                    directingStyle = obj.getString("directingStyle"),
                    generalFlow = obj.getString("generalFlow")
                )
            }
            if (list.isEmpty()) throw IllegalStateException("Aucun concept généré.")
            list.take(3)
        }
    }

    suspend fun regenerateSingleConcept(
        brief: AdBrief,
        otherConcepts: List<AdConcept>,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<AdConcept> {
        val schema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string("Nom court et percutant du concept"),
                "marketingAngle" to Schema.string("Angle marketing stratégique"),
                "creativeIdea" to Schema.string("Idée créative centrale"),
                "hook" to Schema.string("Accroche des 3 premières secondes"),
                "commercialPromise" to Schema.string("Promesse commerciale"),
                "visualMood" to Schema.string("Ambiance visuelle"),
                "directingStyle" to Schema.string("Style de réalisation motion design"),
                "generalFlow" to Schema.string("Déroulement général")
            )
        )
        val existingSummary = otherConcepts.joinToString("\n") { "- ${it.name} (${it.marketingAngle})" }
        val prompt = """
            Tu es un directeur de création motion design senior.
            Propose 1 NOUVEAU concept publicitaire en français, clairement différent des concepts existants ci-dessous :
            $existingSummary

            BRIEF :
            - Produit : ${brief.productName} (${brief.sector})
            - Description : ${brief.description}
            - Avantages : ${brief.benefits}
            - Cible : ${brief.targetAudience}
            - Objectif : ${brief.commercialObjective}
            - Durée : ${brief.durationSeconds}s sur ${brief.platform}
        """.trimIndent()

        return executeStructured(modelId, schema, prompt, productImage) { obj ->
            AdConcept(
                id = UUID.randomUUID().toString(),
                name = obj.getString("name"),
                marketingAngle = obj.getString("marketingAngle"),
                creativeIdea = obj.getString("creativeIdea"),
                hook = obj.getString("hook"),
                commercialPromise = obj.getString("commercialPromise"),
                visualMood = obj.getString("visualMood"),
                directingStyle = obj.getString("directingStyle"),
                generalFlow = obj.getString("generalFlow")
            )
        }
    }

    suspend fun generateScript(
        brief: AdBrief,
        concept: AdConcept,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<AdScript> {
        val sectionSchema = Schema.obj(
            properties = mapOf(
                "type" to Schema.enumeration(
                    values = ScriptSectionType.entries.map { it.name },
                    description = "Type de section du script"
                ),
                "voiceOver" to Schema.string("Texte exact de la voix off, calibré pour la durée"),
                "onScreenText" to Schema.string("Texte court et impactant affiché à l'écran (motion typography)"),
                "durationSeconds" to Schema.integer("Durée en secondes de cette section (entier >= 1)")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "directorNotes" to Schema.string("Notes globales de rythme, ton de voix et typographie"),
                "sections" to Schema.array(
                    items = sectionSchema,
                    description = "Les 5 sections dans l'ordre: HOOK, PRODUCT_INTRO, COMMERCIAL_ARGUMENT, BENEFITS_HIGHLIGHT, CALL_TO_ACTION"
                )
            )
        )

        val prompt = """
            Tu es un concepteur-rédacteur publicitaire et motion designer senior.
            Rédige un script publicitaire complet en français pour une vidéo de EXACTEMENT ${brief.durationSeconds} secondes sur ${brief.platform}.
            La somme des durationSeconds des 5 sections DOIT être égale à ${brief.durationSeconds}.
            Distingue clairement la voix off (naturelle, ~2.5 mots par seconde) du texte affiché à l'écran (court, percutant, lisible en motion design).

            PRODUIT : ${brief.productName}
            DESCRIPTION : ${brief.description}
            CARACTÉRISTIQUES : ${brief.features}
            AVANTAGES : ${brief.benefits}
            CIBLE : ${brief.targetAudience}
            OBJECTIF : ${brief.commercialObjective}

            CONCEPT RETENU :
            - Nom : ${concept.name}
            - Angle : ${concept.marketingAngle}
            - Idée : ${concept.creativeIdea}
            - Accroche : ${concept.hook}
            - Promesse : ${concept.commercialPromise}
            - Ambiance : ${concept.visualMood}
            - Réalisation : ${concept.directingStyle}

            Structure obligatoire en 5 sections dans l'ordre :
            1. HOOK (Accroche)
            2. PRODUCT_INTRO (Présentation du produit)
            3. COMMERCIAL_ARGUMENT (Argument commercial)
            4. BENEFITS_HIGHLIGHT (Mise en valeur des avantages)
            5. CALL_TO_ACTION (Appel à l'action)
        """.trimIndent()

        return executeStructured(modelId, rootSchema, prompt, productImage) { json ->
            val arr = json.getJSONArray("sections")
            val parsed = (0 until arr.length()).mapIndexed { idx, _ ->
                val obj = arr.getJSONObject(idx)
                ScriptSection(
                    id = UUID.randomUUID().toString(),
                    type = ScriptSectionType.fromNameOrDefault(obj.optString("type"), idx),
                    voiceOver = obj.getString("voiceOver"),
                    onScreenText = obj.getString("onScreenText"),
                    durationSeconds = obj.getInt("durationSeconds").coerceAtLeast(1)
                )
            }
            if (parsed.isEmpty()) throw IllegalStateException("Le script généré est vide.")
            AdScript(
                sections = parsed,
                directorNotes = json.optString("directorNotes", "")
            )
        }
    }

    suspend fun regenerateScriptSection(
        brief: AdBrief,
        concept: AdConcept,
        currentScript: AdScript,
        targetSection: ScriptSection,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<ScriptSection> {
        val schema = Schema.obj(
            properties = mapOf(
                "voiceOver" to Schema.string("Nouvelle voix off"),
                "onScreenText" to Schema.string("Nouveau texte à l'écran"),
                "durationSeconds" to Schema.integer("Durée en secondes (viser ${targetSection.durationSeconds}s)")
            )
        )
        val contextSummary = currentScript.sections.joinToString("\n") {
            "- ${it.type.title} (${it.durationSeconds}s) | VO: ${it.voiceOver} | Écran: ${it.onScreenText}"
        }
        val prompt = """
            Réécris uniquement la partie "${targetSection.type.title}" (${targetSection.type.name}) du script publicitaire ci-dessous en français, en gardant une durée proche de ${targetSection.durationSeconds} secondes et en assurant la cohérence avec les autres parties.
            Produit : ${brief.productName} | Concept : ${concept.name} | Plateforme : ${brief.platform}
            Script actuel :
            $contextSummary
        """.trimIndent()

        return executeStructured(modelId, schema, prompt, productImage) { obj ->
            targetSection.copy(
                voiceOver = obj.getString("voiceOver"),
                onScreenText = obj.getString("onScreenText"),
                durationSeconds = obj.getInt("durationSeconds").coerceAtLeast(1)
            )
        }
    }

    suspend fun generateStoryboard(
        brief: AdBrief,
        concept: AdConcept,
        script: AdScript,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<List<StoryboardScene>> {
        val sceneSchema = Schema.obj(
            properties = mapOf(
                "durationSec" to Schema.integer("Durée de la scène en secondes (>= 1)"),
                "narrativeObjective" to Schema.string("Objectif narratif de la scène"),
                "visualDescription" to Schema.string("Description visuelle précise (cadrage, sujets, couleurs, lumière)"),
                "onScreenText" to Schema.string("Texte affiché à l'écran (ou 'Aucun')"),
                "narration" to Schema.string("Voix off sur cette scène (ou 'Silence / musique')"),
                "cameraMovement" to Schema.string("Mouvement de caméra précis"),
                "animation" to Schema.string("Animation motion design précise (textes, produit, formes)"),
                "transition" to Schema.string("Transition vers la scène suivante"),
                "soundEffects" to Schema.string("Effets sonores et ambiance audio"),
                "directingInstructions" to Schema.string("Instructions techniques de réalisation")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "scenes" to Schema.array(
                    items = sceneSchema,
                    description = "Liste chronologique de scènes dont la somme des durationSec est EXACTEMENT égale à ${brief.durationSeconds}"
                )
            )
        )

        val scriptSummary = script.sections.joinToString("\n") {
            "- ${it.type.title} (${it.durationSeconds}s): VO=\"${it.voiceOver}\" | Écran=\"${it.onScreenText}\""
        }
        val prompt = """
            Tu es réalisateur et motion designer senior.
            Construis un storyboard détaillé seconde par seconde en français pour cette publicité de EXACTEMENT ${brief.durationSeconds} secondes sur ${brief.platform}.
            RÈGLE CRITIQUE : La somme exacte de `durationSec` de toutes les scènes DOIT être égale à ${brief.durationSeconds}.
            Découpe le script en scènes dynamiques (généralement entre 2 et 6 secondes par scène selon le rythme).

            PRODUIT : ${brief.productName}
            CONCEPT : ${concept.name} — ${concept.visualMood} / ${concept.directingStyle}
            SCRIPT VALIDÉ :
            $scriptSummary
        """.trimIndent()

        return executeStructured(modelId, rootSchema, prompt, productImage) { json ->
            val arr = json.getJSONArray("scenes")
            var cursor = 0
            val list = (0 until arr.length()).mapIndexed { idx, _ ->
                val obj = arr.getJSONObject(idx)
                val dur = obj.getInt("durationSec").coerceAtLeast(1)
                val start = cursor
                val end = cursor + dur
                cursor = end
                StoryboardScene(
                    id = UUID.randomUUID().toString(),
                    sceneNumber = idx + 1,
                    startTimeSec = start,
                    endTimeSec = end,
                    durationSec = dur,
                    narrativeObjective = obj.getString("narrativeObjective"),
                    visualDescription = obj.getString("visualDescription"),
                    onScreenText = obj.getString("onScreenText"),
                    narration = obj.getString("narration"),
                    cameraMovement = obj.getString("cameraMovement"),
                    animation = obj.getString("animation"),
                    transition = obj.getString("transition"),
                    soundEffects = obj.getString("soundEffects"),
                    directingInstructions = obj.getString("directingInstructions")
                )
            }
            if (list.isEmpty()) throw IllegalStateException("Le storyboard généré est vide.")
            list
        }
    }

    suspend fun regenerateSingleScene(
        brief: AdBrief,
        concept: AdConcept,
        targetScene: StoryboardScene,
        surroundingSummary: String,
        modelId: String,
        productImage: Bitmap? = null
    ): AiOutcome<StoryboardScene> {
        val sceneSchema = Schema.obj(
            properties = mapOf(
                "durationSec" to Schema.integer("Durée en secondes (conserver ${targetScene.durationSec})"),
                "narrativeObjective" to Schema.string("Objectif narratif"),
                "visualDescription" to Schema.string("Description visuelle"),
                "onScreenText" to Schema.string("Texte à afficher"),
                "narration" to Schema.string("Narration"),
                "cameraMovement" to Schema.string("Mouvement de caméra"),
                "animation" to Schema.string("Animation"),
                "transition" to Schema.string("Transition"),
                "soundEffects" to Schema.string("Effets sonores"),
                "directingInstructions" to Schema.string("Instructions de réalisation")
            )
        )
        val prompt = """
            Propose une nouvelle version créative en français pour la Scène #${targetScene.sceneNumber} (${targetScene.startTimeSec}s - ${targetScene.endTimeSec}s, durée ${targetScene.durationSec}s).
            Produit : ${brief.productName} | Plateforme : ${brief.platform} | Style : ${concept.directingStyle}
            Contexte du storyboard :
            $surroundingSummary
        """.trimIndent()

        return executeStructured(modelId, sceneSchema, prompt, productImage) { obj ->
            targetScene.copy(
                durationSec = obj.optInt("durationSec", targetScene.durationSec).coerceAtLeast(1),
                narrativeObjective = obj.getString("narrativeObjective"),
                visualDescription = obj.getString("visualDescription"),
                onScreenText = obj.getString("onScreenText"),
                narration = obj.getString("narration"),
                cameraMovement = obj.getString("cameraMovement"),
                animation = obj.getString("animation"),
                transition = obj.getString("transition"),
                soundEffects = obj.getString("soundEffects"),
                directingInstructions = obj.getString("directingInstructions")
            )
        }
    }

    private suspend fun <T> executeStructured(
        modelId: String,
        schema: Schema,
        prompt: String,
        productImage: Bitmap? = null,
        parser: (JSONObject) -> T
    ): AiOutcome<T> {
        val start = System.currentTimeMillis()
        return try {
            val model = buildStructuredModel(modelId, schema)
            val finalPrompt = if (productImage != null) {
                """
                $prompt

                VISUEL PRODUIT FOURNI (IMAGE JOINTE) :
                Analyse attentivement l'image réelle du produit jointe (couleurs dominantes, matériaux, silhouette, ergonomie, détails visuels et positionnement) et intègre fidèlement ses caractéristiques visuelles dans ta proposition créative, ton cadrage, tes animations et tes choix de palette.
                """.trimIndent()
            } else {
                prompt
            }

            val response = withTimeout(TIMEOUT_MS) {
                if (productImage != null) {
                    model.generateContent(
                        content {
                            image(productImage)
                            text(finalPrompt)
                        }
                    )
                } else {
                    model.generateContent(finalPrompt)
                }
            }
            val elapsed = System.currentTimeMillis() - start
            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return AiOutcome.Failure(
                    message = "Le modèle $modelId a renvoyé une réponse vide.",
                    modelAttempted = modelId,
                    latencyMs = elapsed
                )
            }
            val jsonObj = JSONObject(jsonText)
            val parsed = parser(jsonObj)
            AiOutcome.Success(parsed, modelId, elapsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout on model $modelId", e)
            AiOutcome.Failure(
                message = "La génération avec $modelId a expiré après 60 secondes. Veuillez réessayer.",
                modelAttempted = modelId,
                latencyMs = System.currentTimeMillis() - start
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini structured generation failed on $modelId", e)
            AiOutcome.Failure(
                message = formatAiError(e, modelId),
                modelAttempted = modelId,
                latencyMs = System.currentTimeMillis() - start,
                isQuotaOrNetworkError = isQuotaError(e)
            )
        }
    }

    private fun isQuotaError(e: Exception): Boolean {
        val msg = e.message.orEmpty().lowercase()
        return msg.contains("quota") ||
            msg.contains("429") ||
            msg.contains("resource_exhausted") ||
            msg.contains("unavailable") ||
            msg.contains("billing")
    }

    private fun formatAiError(e: Exception, modelId: String): String {
        val raw = e.localizedMessage ?: e.message ?: "Erreur inconnue"
        return when {
            raw.contains("403") || raw.contains("AppCheck", ignoreCase = true) ->
                "Erreur d'attestation App Check (403) sur $modelId : $raw"
            raw.contains("429") || raw.contains("quota", ignoreCase = true) || raw.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "Quota atteint ou limite de débit sur $modelId : $raw"
            raw.contains("billing", ignoreCase = true) ->
                "Le modèle $modelId nécessite la facturation Firebase Blaze : $raw"
            else -> "Erreur Gemini ($modelId) : $raw"
        }
    }
}
