package com.example.ui.screens.tabs

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MotionProject
import com.example.data.model.StoryboardScene
import com.example.ui.components.ProductImageSectionCard
import com.example.ui.components.StudioEmptyState
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.DirectorAmberContainer
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer

@Composable
fun StoryboardStudioTab(
    project: MotionProject,
    isAiBusy: Boolean,
    onPickProductImage: (Uri) -> Unit,
    onRemoveProductImage: () -> Unit,
    onUpdateScene: (StoryboardScene) -> Unit,
    onAddScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (index: Int, delta: Int) -> Unit,
    onAlignDuration: () -> Unit,
    onRegenerateScene: (index: Int, sceneNumber: Int) -> Unit,
    onRegenerateFullStoryboard: () -> Unit,
    onValidateStoryboard: () -> Unit,
    onShareExport: () -> Unit,
    onBackToScript: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scenes = project.storyboard
    var editingScene by remember { mutableStateOf<StoryboardScene?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ProductImageSectionCard(
            productImagePath = project.brief.productImagePath,
            productName = project.brief.productName,
            onImagePicked = onPickProductImage,
            onRemoveImage = onRemoveProductImage,
            compact = scenes.isNotEmpty() && !project.brief.productImagePath.isNullOrBlank()
        )

        if (scenes.isEmpty()) {
            StudioEmptyState(
                title = "Storyboard seconde par seconde",
                subtitle = if (project.brief.productImagePath == null) {
                    "Téléchargez d'abord l'image du produit ci-dessus, puis générez le storyboard chronologique de ${project.brief.durationSeconds}s."
                } else {
                    "Image du produit prête ! Gemini va découper votre script validé en scènes adaptées au visuel du produit (${project.brief.durationSeconds}s)."
                },
                actionLabel = "Générer le storyboard avec Gemini",
                onActionClick = onRegenerateFullStoryboard
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBackToScript,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Script")
                }
                OutlinedButton(
                    onClick = onAddScene,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scène manuelle")
                }
            }
        } else {
            val totalDur = scenes.sumOf { it.durationSec }
            val targetDur = project.brief.durationSeconds
            val isExactMatch = totalDur == targetDur

            Surface(
                color = if (isExactMatch) ValidatedEmeraldContainer else DirectorAmberContainer,
                border = BorderStroke(
                    1.dp,
                    if (isExactMatch) ValidatedEmerald.copy(alpha = 0.55f) else DirectorAmber.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total : ${totalDur}s / Cible : ${targetDur}s (${scenes.size} scènes)",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isExactMatch) ValidatedEmerald else DirectorAmber
                            )
                            Text(
                                text = if (isExactMatch) {
                                    "Correspondance exacte avec la durée publicitaire."
                                } else {
                                    val diff = totalDur - targetDur
                                    val sign = if (diff > 0) "+$diff" else "$diff"
                                    "Écart de ${sign}s par rapport à la durée cible du brief."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onRegenerateFullStoryboard,
                            enabled = !isAiBusy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("regen_full_storyboard_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tout")
                        }
                    }

                    if (!isExactMatch) {
                        OutlinedButton(
                            onClick = onAlignDuration,
                            border = BorderStroke(1.dp, DirectorAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("align_storyboard_duration_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = DirectorAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aligner automatiquement sur ${targetDur}s",
                                color = DirectorAmber
                            )
                        }
                    }
                }
            }

            scenes.forEachIndexed { index, scene ->
                StoryboardSceneCard(
                    index = index,
                    totalCount = scenes.size,
                    scene = scene,
                    isAiBusy = isAiBusy,
                    onEdit = { editingScene = scene },
                    onMoveUp = { onMoveScene(index, -1) },
                    onMoveDown = { onMoveScene(index, 1) },
                    onRegenerate = { onRegenerateScene(index, scene.sceneNumber) },
                    onDelete = { onDeleteScene(scene.id) }
                )
            }

            OutlinedButton(
                onClick = onAddScene,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("add_storyboard_scene_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajouter une scène")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBackToScript,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("storyboard_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Script")
                }
                OutlinedButton(
                    onClick = onShareExport,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("storyboard_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager")
                }
            }

            Button(
                onClick = onValidateStoryboard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (project.storyboardValidated) ValidatedEmerald else KineticCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("validate_storyboard_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (project.storyboardValidated) {
                        "Storyboard validé (${totalDur}s) — Re-valider"
                    } else {
                        "Valider le storyboard (${totalDur}s)"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    editingScene?.let { target ->
        EditStoryboardSceneDialog(
            scene = target,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                onUpdateScene(updated)
                editingScene = null
            }
        )
    }
}

@Composable
private fun StoryboardSceneCard(
    index: Int,
    totalCount: Int,
    scene: StoryboardScene,
    isAiBusy: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storyboard_scene_card_$index")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = KineticCyanContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SCÈNE #${scene.sceneNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KineticCyan,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = "${scene.startTimeSec}s → ${scene.endTimeSec}s (${scene.durationSec}s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = DirectorAmber,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("move_scene_up_$index")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Monter la scène", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("move_scene_down_$index")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre la scène", modifier = Modifier.size(18.dp))
                    }
                }
            }

            SceneField("Objectif narratif", scene.narrativeObjective)
            SceneField("Description visuelle", scene.visualDescription)
            SceneField("Texte à afficher", scene.onScreenText)
            SceneField("Narration", scene.narration)

            HorizontalDivider(color = StudioBorder)

            SceneField("Mouvement de caméra", scene.cameraMovement)
            SceneField("Animation", scene.animation)
            SceneField("Transition", scene.transition)
            SceneField("Effets sonores", scene.soundEffects)
            SceneField("Instructions de réalisation", scene.directingInstructions)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_scene_$index")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Modifier")
                }
                OutlinedButton(
                    onClick = onRegenerate,
                    enabled = !isAiBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("regen_scene_$index")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("IA")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_scene_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer la scène",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditStoryboardSceneDialog(
    scene: StoryboardScene,
    onDismiss: () -> Unit,
    onSave: (StoryboardScene) -> Unit
) {
    var durationText by remember { mutableStateOf(scene.durationSec.toString()) }
    var narrativeObjective by remember { mutableStateOf(scene.narrativeObjective) }
    var visualDescription by remember { mutableStateOf(scene.visualDescription) }
    var onScreenText by remember { mutableStateOf(scene.onScreenText) }
    var narration by remember { mutableStateOf(scene.narration) }
    var cameraMovement by remember { mutableStateOf(scene.cameraMovement) }
    var animation by remember { mutableStateOf(scene.animation) }
    var transition by remember { mutableStateOf(scene.transition) }
    var soundEffects by remember { mutableStateOf(scene.soundEffects) }
    var directingInstructions by remember { mutableStateOf(scene.directingInstructions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la scène #${scene.sceneNumber}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Durée (secondes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = narrativeObjective, onValueChange = { narrativeObjective = it }, label = { Text("Objectif narratif") })
                OutlinedTextField(value = visualDescription, onValueChange = { visualDescription = it }, label = { Text("Description visuelle") }, minLines = 2)
                OutlinedTextField(value = onScreenText, onValueChange = { onScreenText = it }, label = { Text("Texte à afficher") })
                OutlinedTextField(value = narration, onValueChange = { narration = it }, label = { Text("Narration") }, minLines = 2)
                OutlinedTextField(value = cameraMovement, onValueChange = { cameraMovement = it }, label = { Text("Mouvement de caméra") })
                OutlinedTextField(value = animation, onValueChange = { animation = it }, label = { Text("Animation") })
                OutlinedTextField(value = transition, onValueChange = { transition = it }, label = { Text("Transition") })
                OutlinedTextField(value = soundEffects, onValueChange = { soundEffects = it }, label = { Text("Effets sonores") })
                OutlinedTextField(value = directingInstructions, onValueChange = { directingInstructions = it }, label = { Text("Instructions de réalisation") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = (durationText.toIntOrNull() ?: scene.durationSec).coerceIn(1, 180)
                    onSave(
                        scene.copy(
                            durationSec = dur,
                            narrativeObjective = narrativeObjective,
                            visualDescription = visualDescription,
                            onScreenText = onScreenText,
                            narration = narration,
                            cameraMovement = cameraMovement,
                            animation = animation,
                            transition = transition,
                            soundEffects = soundEffects,
                            directingInstructions = directingInstructions
                        )
                    )
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
