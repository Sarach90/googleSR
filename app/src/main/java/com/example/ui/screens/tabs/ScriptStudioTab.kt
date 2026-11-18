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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.ScriptSection
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
fun ScriptStudioTab(
    project: MotionProject,
    isAiBusy: Boolean,
    onPickProductImage: (Uri) -> Unit,
    onRemoveProductImage: () -> Unit,
    onUpdateSection: (ScriptSection) -> Unit,
    onUpdateDirectorNotes: (String) -> Unit,
    onRegenerateSection: (ScriptSection) -> Unit,
    onRegenerateFullScript: () -> Unit,
    onValidateScriptOnly: () -> Unit,
    onGenerateStoryboardDirectly: () -> Unit,
    onBackToConcepts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val script = project.script
    var editingSection by remember { mutableStateOf<ScriptSection?>(null) }

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
            compact = script != null && !project.brief.productImagePath.isNullOrBlank()
        )

        if (script == null) {
            StudioEmptyState(
                title = "Prêt pour le script",
                subtitle = if (project.brief.productImagePath == null) {
                    "Importez d'abord l'image du produit ci-dessus (recommandé) puis générez un script structuré (Voix off + Texte à l'écran) adapté à ${project.brief.durationSeconds}s."
                } else {
                    "Image du produit prête ! Gemini va rédiger un script structuré (Voix off + Texte à l'écran) adapté à ${project.brief.durationSeconds}s à partir du concept « ${project.selectedConcept?.name ?: "retenu"} »."
                },
                actionLabel = "Générer le script avec Gemini",
                onActionClick = onRegenerateFullScript
            )
            OutlinedButton(
                onClick = onBackToConcepts,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revenir aux concepts")
            }
        } else {
            val totalDur = script.totalDurationSeconds
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Durée script : ${totalDur}s / Cible brief : ${targetDur}s",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isExactMatch) ValidatedEmerald else DirectorAmber
                        )
                        Text(
                            text = if (isExactMatch) {
                                "Rythme calibré exactement sur la durée cible."
                            } else {
                                "Ajustez la durée des sections ou laissez le Storyboard réaligner la timeline."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onRegenerateFullScript,
                        enabled = !isAiBusy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("regen_full_script_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tout")
                    }
                }
            }

            OutlinedTextField(
                value = script.directorNotes,
                onValueChange = onUpdateDirectorNotes,
                label = { Text("Notes de ton & rythme") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_director_notes_input")
            )

            script.sections.forEachIndexed { idx, section ->
                ScriptSectionCard(
                    index = idx,
                    section = section,
                    isAiBusy = isAiBusy,
                    onEdit = { editingSection = section },
                    onRegenerate = { onRegenerateSection(section) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBackToConcepts,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("script_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Concepts")
                }
                OutlinedButton(
                    onClick = onValidateScriptOnly,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("script_validate_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Valider")
                }
            }

            Button(
                onClick = onGenerateStoryboardDirectly,
                enabled = !isAiBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("script_generate_storyboard_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (project.storyboard.isEmpty()) {
                        "Valider le script & générer le storyboard"
                    } else {
                        "Régénérer le storyboard depuis ce script"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    editingSection?.let { target ->
        EditScriptSectionDialog(
            section = target,
            onDismiss = { editingSection = null },
            onSave = { updated ->
                onUpdateSection(updated)
                editingSection = null
            }
        )
    }
}

@Composable
private fun ScriptSectionCard(
    index: Int,
    section: ScriptSection,
    isAiBusy: Boolean,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("script_section_card_$index")
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
                        text = section.type.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = KineticCyan
                    )
                    Text(
                        text = section.type.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = "${section.durationSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = DirectorAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Voice Over Block
            Surface(
                color = KineticCyanContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, KineticCyan.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voix off",
                            tint = KineticCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VOIX OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = KineticCyan
                        )
                    }
                    Text(
                        text = section.voiceOver,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // On-screen Text Block
            Surface(
                color = DirectorAmberContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DirectorAmber.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Texte à l'écran",
                            tint = DirectorAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TEXTE À L'ÉCRAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = DirectorAmber
                        )
                    }
                    Text(
                        text = section.onScreenText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_script_section_$index")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier")
                }
                OutlinedButton(
                    onClick = onRegenerate,
                    enabled = !isAiBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("regen_script_section_$index")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Régénérer")
                }
            }
        }
    }
}

@Composable
private fun EditScriptSectionDialog(
    section: ScriptSection,
    onDismiss: () -> Unit,
    onSave: (ScriptSection) -> Unit
) {
    var voiceOver by remember { mutableStateOf(section.voiceOver) }
    var onScreenText by remember { mutableStateOf(section.onScreenText) }
    var durationText by remember { mutableStateOf(section.durationSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier : ${section.type.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = voiceOver,
                    onValueChange = { voiceOver = it },
                    label = { Text("Voix off") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = onScreenText,
                    onValueChange = { onScreenText = it },
                    label = { Text("Texte à l'écran") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Durée (secondes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = (durationText.toIntOrNull() ?: section.durationSeconds).coerceIn(1, 180)
                    onSave(
                        section.copy(
                            voiceOver = voiceOver,
                            onScreenText = onScreenText,
                            durationSeconds = dur
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
