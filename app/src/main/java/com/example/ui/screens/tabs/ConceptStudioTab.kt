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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AdConcept
import com.example.data.model.MotionProject
import com.example.ui.components.ProductImageSectionCard
import com.example.ui.components.StudioEmptyState
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer

@Composable
fun ConceptStudioTab(
    project: MotionProject,
    isAiBusy: Boolean,
    onPickProductImage: (Uri) -> Unit,
    onRemoveProductImage: () -> Unit,
    onSelectConcept: (String) -> Unit,
    onUpdateConcept: (AdConcept) -> Unit,
    onRegenerateSingleConcept: (index: Int, conceptName: String) -> Unit,
    onRegenerateAllConcepts: () -> Unit,
    onValidateConceptAndNext: () -> Unit,
    onGenerateScriptDirectly: () -> Unit,
    onBackToBrief: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingConcept by remember { mutableStateOf<AdConcept?>(null) }

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
            compact = project.concepts.isNotEmpty() && !project.brief.productImagePath.isNullOrBlank()
        )

        if (project.concepts.isEmpty()) {
            StudioEmptyState(
                title = "Aucun concept généré",
                subtitle = "Gemini va analyser votre brief « ${project.brief.productName.ifBlank { project.title }} » et proposer 3 concepts publicitaires distincts.",
                actionLabel = "Générer 3 concepts avec Gemini",
                onActionClick = onRegenerateAllConcepts
            )
            OutlinedButton(
                onClick = onBackToBrief,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revenir au brief")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3 propositions créatives",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Sélectionnez, modifiez manuellement ou régénérez un concept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onRegenerateAllConcepts,
                    enabled = !isAiBusy,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("regenerate_all_concepts_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tout régénérer")
                }
            }

            project.concepts.forEachIndexed { index, concept ->
                val isSelected = concept.id == project.selectedConcept?.id
                ConceptDetailCard(
                    index = index,
                    concept = concept,
                    isSelected = isSelected,
                    isAiBusy = isAiBusy,
                    onSelect = { onSelectConcept(concept.id) },
                    onEdit = { editingConcept = concept },
                    onRegenerate = { onRegenerateSingleConcept(index, concept.name) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBackToBrief,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("concepts_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Brief")
                }
                OutlinedButton(
                    onClick = onValidateConceptAndNext,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("concepts_validate_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Valider")
                }
            }

            Button(
                onClick = onGenerateScriptDirectly,
                enabled = !isAiBusy && project.selectedConcept != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("concepts_generate_script_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (project.script == null) {
                        "Valider ce concept & générer le script"
                    } else {
                        "Régénérer le script depuis ce concept"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    editingConcept?.let { target ->
        EditConceptDialog(
            concept = target,
            onDismiss = { editingConcept = null },
            onSave = { updated ->
                onUpdateConcept(updated)
                editingConcept = null
            }
        )
    }
}

@Composable
private fun ConceptDetailCard(
    index: Int,
    concept: AdConcept,
    isSelected: Boolean,
    isAiBusy: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StudioSurfaceElevated else StudioSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) KineticCyan else StudioBorder
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("concept_card_$index")
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
                        text = "CONCEPT 0${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) KineticCyan else DirectorAmber
                    )
                    Text(
                        text = concept.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = if (isSelected) ValidatedEmeraldContainer else KineticCyanContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSelected) "Sélectionné" else "Choisir",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) ValidatedEmerald else KineticCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            ConceptFieldRow("Angle marketing", concept.marketingAngle)
            ConceptFieldRow("Idée créative", concept.creativeIdea)
            ConceptFieldRow("Accroche (0–3s)", concept.hook)
            ConceptFieldRow("Promesse commerciale", concept.commercialPromise)
            ConceptFieldRow("Ambiance visuelle", concept.visualMood)
            ConceptFieldRow("Style de réalisation", concept.directingStyle)
            ConceptFieldRow("Déroulement général", concept.generalFlow)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_concept_$index")
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
                        .testTag("regen_concept_$index")
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
private fun ConceptFieldRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KineticCyan
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditConceptDialog(
    concept: AdConcept,
    onDismiss: () -> Unit,
    onSave: (AdConcept) -> Unit
) {
    var name by remember { mutableStateOf(concept.name) }
    var marketingAngle by remember { mutableStateOf(concept.marketingAngle) }
    var creativeIdea by remember { mutableStateOf(concept.creativeIdea) }
    var hook by remember { mutableStateOf(concept.hook) }
    var commercialPromise by remember { mutableStateOf(concept.commercialPromise) }
    var visualMood by remember { mutableStateOf(concept.visualMood) }
    var directingStyle by remember { mutableStateOf(concept.directingStyle) }
    var generalFlow by remember { mutableStateOf(concept.generalFlow) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le concept") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du concept") })
                OutlinedTextField(value = marketingAngle, onValueChange = { marketingAngle = it }, label = { Text("Angle marketing") })
                OutlinedTextField(value = creativeIdea, onValueChange = { creativeIdea = it }, label = { Text("Idée créative") }, minLines = 2)
                OutlinedTextField(value = hook, onValueChange = { hook = it }, label = { Text("Accroche") }, minLines = 2)
                OutlinedTextField(value = commercialPromise, onValueChange = { commercialPromise = it }, label = { Text("Promesse commerciale") })
                OutlinedTextField(value = visualMood, onValueChange = { visualMood = it }, label = { Text("Ambiance visuelle") })
                OutlinedTextField(value = directingStyle, onValueChange = { directingStyle = it }, label = { Text("Style de réalisation") })
                OutlinedTextField(value = generalFlow, onValueChange = { generalFlow = it }, label = { Text("Déroulement général") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        concept.copy(
                            name = name,
                            marketingAngle = marketingAngle,
                            creativeIdea = creativeIdea,
                            hook = hook,
                            commercialPromise = commercialPromise,
                            visualMood = visualMood,
                            directingStyle = directingStyle,
                            generalFlow = generalFlow
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
