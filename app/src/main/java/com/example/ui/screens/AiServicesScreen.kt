package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GeminiModelCatalog
import com.example.data.model.GeminiModelSpec
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.DirectorAmberContainer
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer
import com.example.ui.viewmodel.AiServicesUiState
import com.example.ui.viewmodel.ConnectionTestState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ServiceMatrixEntry(
    val provider: String,
    val model: String,
    val feature: String,
    val configState: String,
    val availability: String,
    val quotas: String,
    val cost: String,
    val isActiveNow: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiServicesScreen(
    uiState: AiServicesUiState,
    onSelectPrimaryModel: (String) -> Unit,
    onSelectFallbackModel: (String?) -> Unit,
    onToggleEconomicMode: (Boolean) -> Unit,
    onTestConnection: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.FRENCH) }

    val serviceMatrix = remember(settings.primaryModelId, settings.fallbackModelId) {
        listOf(
            ServiceMatrixEntry(
                provider = "Firebase AI Logic (Google Gemini)",
                model = settings.primaryModelId,
                feature = "Brief, Concepts, Script & Storyboard JSON",
                configState = "Configuré (App Check actif, aucune clé exposée)",
                availability = "Disponible",
                quotas = settings.primaryModel.quotaInfo,
                cost = settings.primaryModel.costNotice,
                isActiveNow = true
            ),
            ServiceMatrixEntry(
                provider = "Firebase AI Logic (Secours)",
                model = settings.fallbackModelId ?: "Désactivé",
                feature = "Bascule manuelle en cas d'erreur / quota",
                configState = if (settings.fallbackModelId != null) "Prêt" else "Non activé",
                availability = if (settings.fallbackModelId != null) "Disponible" else "—",
                quotas = settings.fallbackModel?.quotaInfo ?: "Aucun modèle de secours configuré",
                cost = settings.fallbackModel?.costNotice ?: "—",
                isActiveNow = settings.fallbackModelId != null
            ),
            ServiceMatrixEntry(
                provider = "Stockage local (Room + DataStore)",
                model = "SQLite local chiffré OS",
                feature = "Sauvegarde auto projets, versions & cache IA",
                configState = "Actif",
                availability = "Hors-ligne permanent",
                quotas = "Limité uniquement par le stockage de l'appareil",
                cost = "100% Gratuit & Open Source",
                isActiveNow = true
            ),
            ServiceMatrixEntry(
                provider = "Modules Phase 3–5 (Visuel, Voix, Son, Export)",
                model = "Non activés dans le socle Phase 1–2",
                feature = "Prompts visuels, TTS, CapCut, Export ZIP",
                configState = "À venir (phases suivantes)",
                availability = "Non déployé",
                quotas = "Aucune promesse de quota illimité",
                cost = "Priorité solutions gratuites lors de l'activation",
                isActiveNow = false
            )
        )
    }

    Scaffold(
        containerColor = StudioObsidian,
        modifier = modifier
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Gestion des services IA",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Configuration Gemini sécurisée, contrôle des quotas et transparence des coûts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Security & Economic Mode card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = ValidatedEmerald
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Passerelle sécurisée sans clé exposée",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Les appels passent par Firebase AI Logic avec attestation App Check. Aucune clé API n'est stockée dans l'interface.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = StudioBorder)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = KineticCyan
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mode économique",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Demande confirmation avant chaque appel IA, évite les régénérations automatiques et privilégie l'édition manuelle.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = settings.economicModeEnabled,
                                    onCheckedChange = onToggleEconomicMode,
                                    modifier = Modifier.testTag("economic_mode_switch")
                                )
                            }
                        }
                    }
                }

                // Live connection test
                item {
                    ConnectionTestCard(
                        primaryModelId = settings.primaryModelId,
                        testState = uiState.testState,
                        onTestClick = { onTestConnection(settings.primaryModelId) }
                    )
                }

                // Primary Model Selector
                item {
                    Text(
                        text = "Modèle principal Gemini",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(GeminiModelCatalog.supportedModels, key = { it.id }) { spec ->
                    ModelSelectionCard(
                        spec = spec,
                        isSelected = settings.primaryModelId == spec.id,
                        onSelect = { onSelectPrimaryModel(spec.id) }
                    )
                }

                // Fallback model selector
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = DirectorAmber
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Modèle de secours",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "Utilisé sur action explicite si le modèle principal rencontre un quota ou une erreur réseau.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = settings.fallbackModelId == null,
                                    onClick = { onSelectFallbackModel(null) },
                                    label = { Text("Désactivé") },
                                    modifier = Modifier.testTag("fallback_none")
                                )
                                GeminiModelCatalog.supportedModels
                                    .filter { it.id != settings.primaryModelId }
                                    .forEach { spec ->
                                        FilterChip(
                                            selected = settings.fallbackModelId == spec.id,
                                            onClick = { onSelectFallbackModel(spec.id) },
                                            label = { Text(spec.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DirectorAmberContainer,
                                                selectedLabelColor = DirectorAmber
                                            ),
                                            modifier = Modifier.testTag("fallback_${spec.id}")
                                        )
                                    }
                            }
                        }
                    }
                }

                // Service Status Matrix
                item {
                    Text(
                        text = "État des services, quotas & coûts",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(serviceMatrix) { item ->
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.provider,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (item.isActiveNow) ValidatedEmeraldContainer else StudioSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.availability,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.isActiveNow) ValidatedEmerald else TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Modèle : ${item.model}",
                                style = MaterialTheme.typography.labelMedium,
                                color = KineticCyan
                            )
                            Text(
                                text = "Fonctionnalité : ${item.feature}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Configuration : ${item.configState}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Quotas : ${item.quotas}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Coût : ${item.cost}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.cost.contains("Blaze")) DirectorAmber else ValidatedEmerald
                            )
                        }
                    }
                }

                // Call logs
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Journal des appels IA (${settings.sessionCallCount})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (settings.callLogs.isNotEmpty()) {
                            TextButton(
                                onClick = onClearLogs,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Effacer")
                            }
                        }
                    }
                }

                if (settings.callLogs.isEmpty()) {
                    item {
                        Surface(
                            color = StudioSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Aucun appel Gemini enregistré pour le moment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(settings.callLogs, key = { it.id }) { log ->
                        Surface(
                            color = StudioSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (log.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (log.success) ValidatedEmerald else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${log.actionName} • ${log.modelId}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${timeFormatter.format(Date(log.timestamp))} • ${log.latencyMs} ms • ${log.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ConnectionTestCard(
    primaryModelId: String,
    testState: ConnectionTestState,
    onTestClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
        border = BorderStroke(1.dp, KineticCyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp)
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
                        text = "Vérification en direct",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Teste l'attestation App Check et la disponibilité réelle de $primaryModelId.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onTestClick,
                    enabled = testState !is ConnectionTestState.Testing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("test_gemini_connection_button")
                ) {
                    if (testState is ConnectionTestState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tester")
                    }
                }
            }

            when (testState) {
                is ConnectionTestState.Idle -> Unit
                is ConnectionTestState.Testing -> {
                    Text(
                        text = "Envoi d'une requête réelle à ${testState.modelId}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = KineticCyan
                    )
                }
                is ConnectionTestState.Success -> {
                    Surface(
                        color = ValidatedEmeraldContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Connecté à ${testState.modelId} (${testState.latencyMs} ms) : « ${testState.message} »",
                            style = MaterialTheme.typography.bodySmall,
                            color = ValidatedEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
                is ConnectionTestState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = testState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelSelectionCard(
    spec: GeminiModelSpec,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StudioSurfaceElevated else StudioSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = when {
                isSelected && spec.requiresBilling -> DirectorAmber
                isSelected -> KineticCyan
                else -> StudioBorder
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${spec.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spec.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = spec.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = KineticCyan
                    )
                }
                Surface(
                    color = if (spec.requiresBilling) DirectorAmberContainer else ValidatedEmeraldContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (spec.requiresBilling) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = DirectorAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (spec.requiresBilling) "Blaze requis" else "Quota gratuit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (spec.requiresBilling) DirectorAmber else ValidatedEmerald
                        )
                    }
                }
            }

            Text(
                text = spec.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "${spec.quotaInfo} • ${spec.costNotice}",
                style = MaterialTheme.typography.labelSmall,
                color = if (spec.requiresBilling) DirectorAmber else TextMuted
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spec.capabilities.forEach { cap ->
                    Surface(
                        color = StudioObsidian,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = cap,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
