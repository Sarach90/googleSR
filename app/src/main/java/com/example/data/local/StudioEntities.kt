package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AdBrief
import com.example.data.model.AdConcept
import com.example.data.model.AdScript
import com.example.data.model.MotionProject
import com.example.data.model.ProjectVersion
import com.example.data.model.StoryboardScene
import com.example.data.model.WorkflowStep
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val currentStep: String,
    val briefValidated: Boolean,
    val conceptsValidated: Boolean,
    val scriptValidated: Boolean,
    val storyboardValidated: Boolean,
    val briefJson: String,
    val conceptsJson: String,
    val selectedConceptId: String?,
    val scriptJson: String?,
    val storyboardJson: String
)

@Entity(
    tableName = "project_versions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectVersionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val createdAt: Long,
    val label: String,
    val stepName: String,
    val snapshotJson: String
)

fun MotionProject.toEntity(): ProjectEntity {
    val conceptsArr = JSONArray().apply { concepts.forEach { put(it.toJson()) } }
    val scenesArr = JSONArray().apply { storyboard.forEach { put(it.toJson()) } }
    return ProjectEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        currentStep = currentStep.name,
        briefValidated = briefValidated,
        conceptsValidated = conceptsValidated,
        scriptValidated = scriptValidated,
        storyboardValidated = storyboardValidated,
        briefJson = brief.toJson().toString(),
        conceptsJson = conceptsArr.toString(),
        selectedConceptId = selectedConceptId,
        scriptJson = script?.toJson()?.toString(),
        storyboardJson = scenesArr.toString()
    )
}

fun ProjectEntity.toDomain(): MotionProject {
    val step = WorkflowStep.entries.firstOrNull { it.name == currentStep } ?: WorkflowStep.BRIEF
    val brief = runCatching { AdBrief.fromJson(JSONObject(briefJson)) }.getOrDefault(AdBrief())
    val concepts = runCatching {
        val arr = JSONArray(conceptsJson)
        (0 until arr.length()).map { AdConcept.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    val script = runCatching {
        scriptJson?.let { AdScript.fromJson(JSONObject(it)) }
    }.getOrNull()
    val scenes = runCatching {
        val arr = JSONArray(storyboardJson)
        (0 until arr.length()).map { idx -> StoryboardScene.fromJson(arr.getJSONObject(idx), idx + 1) }
    }.getOrDefault(emptyList())

    return MotionProject(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        currentStep = step,
        briefValidated = briefValidated,
        conceptsValidated = conceptsValidated,
        scriptValidated = scriptValidated,
        storyboardValidated = storyboardValidated,
        brief = brief,
        concepts = concepts,
        selectedConceptId = selectedConceptId,
        script = script,
        storyboard = scenes
    )
}

fun ProjectVersionEntity.toDomain(): ProjectVersion {
    return ProjectVersion(
        id = id,
        projectId = projectId,
        createdAt = createdAt,
        label = label,
        stepName = stepName,
        snapshotJson = snapshotJson
    )
}

fun MotionProject.applySnapshot(snapshotJson: String): MotionProject {
    val root = JSONObject(snapshotJson)
    val brief = AdBrief.fromJson(root.optJSONObject("brief"))
    val conceptsArr = root.optJSONArray("concepts") ?: JSONArray()
    val concepts = (0 until conceptsArr.length()).map { AdConcept.fromJson(conceptsArr.getJSONObject(it)) }
    val script = AdScript.fromJson(root.optJSONObject("script"))
    val scenesArr = root.optJSONArray("storyboard") ?: JSONArray()
    val scenes = (0 until scenesArr.length()).mapIndexed { idx, _ ->
        StoryboardScene.fromJson(scenesArr.getJSONObject(idx), idx + 1)
    }
    val step = WorkflowStep.entries.firstOrNull { it.name == root.optString("currentStep") } ?: WorkflowStep.BRIEF
    return copy(
        title = root.optString("title", title),
        updatedAt = System.currentTimeMillis(),
        currentStep = step,
        briefValidated = root.optBoolean("briefValidated", briefValidated),
        conceptsValidated = root.optBoolean("conceptsValidated", conceptsValidated),
        scriptValidated = root.optBoolean("scriptValidated", scriptValidated),
        storyboardValidated = root.optBoolean("storyboardValidated", storyboardValidated),
        brief = brief,
        concepts = concepts,
        selectedConceptId = root.optString("selectedConceptId", "").ifBlank { null },
        script = script,
        storyboard = scenes
    )
}
