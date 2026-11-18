package com.example.ui.screens.tabs

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.AdBrief
import com.example.data.model.MotionProject
import com.example.ui.components.ProductImageSectionCard
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BriefStudioTab(
    project: MotionProject,
    isAiBusy: Boolean,
    onBriefChanged: (AdBrief) -> Unit,
    onPickProductImage: (Uri) -> Unit,
    onRemoveProductImage: () -> Unit,
    onSaveSnapshot: () -> Unit,
    onValidateBriefOnly: () -> Unit,
    onGenerateConcepts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brief = project.brief
    var durationInput by remember(project.id, brief.durationSeconds) {
        mutableStateOf(brief.durationSeconds.toString())
    }
    val missingMessage = brief.validationMissingMessage()

    val objectives = listOf(
        "Conversion & Ventes",
        "Notoriété de marque",
        "Lancement produit",
        "Trafic & Clics",
        "Engagement social"
    )
    val platforms = listOf(
        "TikTok / Reels (9:16)",
        "YouTube Shorts (9:16)",
        "YouTube Ads (16:9)",
        "Instagram Feed (4:5)",
        "LinkedIn (16:9 / 1:1)"
    )
    val presetDurations = listOf(15, 30, 45, 60)

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (project.briefValidated) {
                Surface(
                    color = ValidatedEmeraldContainer,
                    border = BorderStroke(1.dp, ValidatedEmerald.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ValidatedEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Brief validé. Les modifications restent locales et n'effacent pas vos concepts ou scripts déjà validés.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            ProductImageSectionCard(
                productImagePath = brief.productImagePath,
                productName = brief.productName,
                onImagePicked = onPickProductImage,
                onRemoveImage = onRemoveProductImage,
                compact = false
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Identité & Proposition de valeur",
                        style = MaterialTheme.typography.titleMedium,
                        color = KineticCyan
                    )

                    OutlinedTextField(
                        value = brief.productName,
                        onValueChange = { onBriefChanged(brief.copy(productName = it)) },
                        label = { Text("Nom du produit *") },
                        placeholder = { Text("Ex: Casque Nova ANC Pro") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_product_name_input")
                    )

                    OutlinedTextField(
                        value = brief.sector,
                        onValueChange = { onBriefChanged(brief.copy(sector = it)) },
                        label = { Text("Secteur d'activité") },
                        placeholder = { Text("Ex: Audio & Tech grand public") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_sector_input")
                    )

                    OutlinedTextField(
                        value = brief.description,
                        onValueChange = { onBriefChanged(brief.copy(description = it)) },
                        label = { Text("Description du produit / offre *") },
                        placeholder = { Text("Ce que fait le produit et ce qui le rend unique…") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_description_input")
                    )

                    OutlinedTextField(
                        value = brief.features,
                        onValueChange = { onBriefChanged(brief.copy(features = it)) },
                        label = { Text("Caractéristiques techniques / clés") },
                        placeholder = { Text("Ex: 48h d'autonomie, réduction active -45dB, USB-C") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_features_input")
                    )

                    OutlinedTextField(
                        value = brief.benefits,
                        onValueChange = { onBriefChanged(brief.copy(benefits = it)) },
                        label = { Text("Avantages client") },
                        placeholder = { Text("Ex: Concentration immédiate dans les transports, confort 8h") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_benefits_input")
                    )

                    OutlinedTextField(
                        value = brief.targetAudience,
                        onValueChange = { onBriefChanged(brief.copy(targetAudience = it)) },
                        label = { Text("Public cible") },
                        placeholder = { Text("Ex: Créatifs urbains 22-38 ans, télétravailleurs") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_audience_input")
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Format & Objectif de diffusion",
                        style = MaterialTheme.typography.titleMedium,
                        color = DirectorAmber
                    )

                    Text(
                        text = "Objectif commercial",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        objectives.forEach { obj ->
                            FilterChip(
                                selected = brief.commercialObjective == obj,
                                onClick = { onBriefChanged(brief.copy(commercialObjective = obj)) },
                                label = { Text(obj) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KineticCyanContainer,
                                    selectedLabelColor = KineticCyan
                                )
                            )
                        }
                    }

                    Text(
                        text = "Durée de la vidéo (secondes)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetDurations.forEach { dur ->
                            FilterChip(
                                selected = brief.durationSeconds == dur,
                                onClick = {
                                    durationInput = dur.toString()
                                    onBriefChanged(brief.copy(durationSeconds = dur))
                                },
                                label = { Text("${dur}s") }
                            )
                        }
                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() }.take(3)
                                durationInput = filtered
                                val parsed = filtered.toIntOrNull()
                                if (parsed != null && parsed in 5..180) {
                                    onBriefChanged(brief.copy(durationSeconds = parsed))
                                }
                            },
                            label = { Text("Perso (s)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brief_duration_input")
                        )
                    }

                    Text(
                        text = "Plateforme de diffusion",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        platforms.forEach { plat ->
                            FilterChip(
                                selected = brief.platform == plat,
                                onClick = { onBriefChanged(brief.copy(platform = plat)) },
                                label = { Text(plat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KineticCyanContainer,
                                    selectedLabelColor = KineticCyan
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = "* Champs requis avant l'analyse Gemini. Sauvegarde locale automatique activée.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Sticky bottom action bar so validation & next step are always immediately accessible
        Surface(
            color = StudioSurface,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column {
                HorizontalDivider(color = StudioBorder)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = missingMessage ?: "Brief prêt — validez ou générez directement les 3 concepts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (missingMessage == null) ValidatedEmerald else DirectorAmber
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onSaveSnapshot,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("brief_save_snapshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Enregistrer un instantané",
                                tint = TextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = onValidateBriefOnly,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("brief_validate_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Valider → Concepts", maxLines = 1)
                        }

                        Button(
                            onClick = onGenerateConcepts,
                            enabled = !isAiBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = KineticCyan),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("brief_generate_concepts_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (project.concepts.isEmpty()) "3 Concepts IA" else "Régénérer IA",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
