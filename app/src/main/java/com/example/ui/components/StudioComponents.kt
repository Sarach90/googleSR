package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductImageStorage
import com.example.data.model.AiSettings
import com.example.data.model.GeminiModelCatalog
import com.example.data.model.MotionProject
import com.example.data.model.WorkflowStep
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.DirectorAmberContainer
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ValidatedEmerald
import com.example.ui.theme.ValidatedEmeraldContainer
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.PendingAiOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ActiveModelBadge(
    aiSettings: AiSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = aiSettings.primaryModel
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = StudioSurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (model.requiresBilling) DirectorAmber.copy(alpha = 0.6f) else KineticCyan.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("active_model_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Modèle IA actif",
                tint = if (model.requiresBilling) DirectorAmber else KineticCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = model.id,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (aiSettings.economicModeEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ValidatedEmeraldContainer)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ÉCO",
                        style = MaterialTheme.typography.labelSmall,
                        color = ValidatedEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowStepperBar(
    project: MotionProject,
    onStepSelected: (WorkflowStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkflowStep.entries.forEach { step ->
            val isValidated = when (step) {
                WorkflowStep.BRIEF -> project.briefValidated
                WorkflowStep.CONCEPTS -> project.conceptsValidated
                WorkflowStep.SCRIPT -> project.scriptValidated
                WorkflowStep.STORYBOARD -> project.storyboardValidated
            }
            val isUnlocked = when (step) {
                WorkflowStep.BRIEF -> true
                WorkflowStep.CONCEPTS -> project.briefValidated || project.concepts.isNotEmpty()
                WorkflowStep.SCRIPT -> project.conceptsValidated || project.script != null
                WorkflowStep.STORYBOARD -> project.scriptValidated || project.storyboard.isNotEmpty()
            }
            val isCurrent = project.currentStep == step

            val containerColor = when {
                isCurrent -> KineticCyanContainer
                isValidated -> ValidatedEmeraldContainer
                else -> StudioSurfaceElevated
            }
            val borderColor = when {
                isCurrent -> KineticCyan
                isValidated -> ValidatedEmerald.copy(alpha = 0.65f)
                else -> StudioBorder
            }
            val contentColor = when {
                isCurrent -> KineticCyan
                isValidated -> ValidatedEmerald
                isUnlocked -> MaterialTheme.colorScheme.onSurface
                else -> TextMuted
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStepSelected(step) }
                    .testTag("workflow_step_${step.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when {
                        isValidated -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Étape validée",
                            tint = ValidatedEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        !isUnlocked -> Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Étape verrouillée",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        else -> Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) KineticCyan else StudioBorder
                                )
                        ) {
                            Text(
                                text = "${step.index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${step.index + 1}. ${step.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun AiStatusBanner(
    opState: AiOperationState,
    onRetryFallback: (PendingAiOperation, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = opState !is AiOperationState.Idle, modifier = modifier) {
        when (opState) {
            is AiOperationState.Idle -> Unit
            is AiOperationState.Loading -> {
                Surface(
                    color = KineticCyanContainer,
                    border = BorderStroke(1.dp, KineticCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ai_loading_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = KineticCyan,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opState.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Appel sécurisé via Firebase AI Logic (${opState.modelId})…",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            is AiOperationState.SuccessBanner -> {
                Surface(
                    color = ValidatedEmeraldContainer,
                    border = BorderStroke(1.dp, ValidatedEmerald.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ai_success_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Succès",
                            tint = ValidatedEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = opState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer le message",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            is AiOperationState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ai_error_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Erreur IA",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = opState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer l'erreur",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        if (opState.fallbackModelId != null && opState.retryAction != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onRetryFallback(opState.retryAction, opState.fallbackModelId)
                                },
                                border = BorderStroke(1.dp, DirectorAmber),
                                modifier = Modifier.testTag("retry_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = DirectorAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Réessayer avec le modèle de secours (${opState.fallbackModelId})",
                                    color = DirectorAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EconomicConfirmationDialog(
    pendingOperation: PendingAiOperation,
    aiSettings: AiSettings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val modelSpec = GeminiModelCatalog.findById(aiSettings.primaryModelId)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (modelSpec.requiresBilling) Icons.Default.WarningAmber else Icons.Default.Savings,
                contentDescription = null,
                tint = if (modelSpec.requiresBilling) DirectorAmber else KineticCyan
            )
        },
        title = {
            Text(
                text = pendingOperation.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = pendingOperation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Surface(
                    color = if (modelSpec.requiresBilling) DirectorAmberContainer else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (modelSpec.requiresBilling) DirectorAmber.copy(alpha = 0.5f) else StudioBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Modèle : ${modelSpec.displayName} (${modelSpec.id})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = modelSpec.quotaInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = modelSpec.costNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (modelSpec.requiresBilling) DirectorAmber else ValidatedEmerald
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (modelSpec.requiresBilling) DirectorAmber else KineticCyan
                ),
                modifier = Modifier.testTag("confirm_ai_call_button")
            ) {
                Text("Confirmer l'appel Gemini", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_ai_call_button")
            ) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun StudioEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_director_slate),
            contentDescription = "Illustration studio de production",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onActionClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("empty_state_primary_button")
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
fun ProductImageSectionCard(
    productImagePath: String?,
    productName: String,
    onImagePicked: (Uri) -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    var pickerError by remember { mutableStateOf<String?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickerError = null
            onImagePicked(uri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickerError = null
            onImagePicked(uri)
        }
    }

    val contentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickerError = null
            onImagePicked(uri)
        }
    }

    val launchDocumentPicker = {
        pickerError = null
        try {
            documentPickerLauncher.launch(arrayOf("image/*"))
        } catch (_: Exception) {
            pickerError = "Aucun sélecteur de fichiers compatible trouvé sur cet appareil."
        }
    }

    val launchPhotoOrContentPicker = {
        pickerError = null
        try {
            contentPickerLauncher.launch("image/*")
        } catch (_: Exception) {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                try {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } catch (_: Exception) {
                    launchDocumentPicker()
                }
            } else {
                launchDocumentPicker()
            }
        }
    }

    val galleryIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri = data?.data
                ?: data?.clipData?.let { clip ->
                    if (clip.itemCount > 0) clip.getItemAt(0).uri else null
                }
            if (pickedUri != null) {
                pickerError = null
                onImagePicked(pickedUri)
            } else {
                pickerError = "La galerie n'a renvoyé aucune image. Essayez « Photos / Fichiers »."
            }
        }
    }

    val launchGalleryPicker = {
        pickerError = null
        try {
            val pickIntent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            galleryIntentLauncher.launch(pickIntent)
        } catch (_: Exception) {
            launchPhotoOrContentPicker()
        }
    }

    val previewBitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = productImagePath
    ) {
        value = if (productImagePath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ProductImageStorage.loadBitmap(productImagePath)?.asImageBitmap()
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(
            1.dp,
            if (previewBitmap != null) ValidatedEmerald.copy(alpha = 0.55f) else KineticCyan.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_image_section_card")
    ) {
        if (compact && previewBitmap != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Image(
                    bitmap = previewBitmap!!,
                    contentDescription = "Aperçu du produit ${productName.ifBlank { "" }}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Image du produit chargée",
                        style = MaterialTheme.typography.titleSmall,
                        color = ValidatedEmerald
                    )
                    Text(
                        text = "Incluse dans l'analyse multimodale Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = launchGalleryPicker,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("replace_product_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Changer depuis la galerie",
                        tint = KineticCyan
                    )
                }
                IconButton(
                    onClick = launchPhotoOrContentPicker,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("browse_product_image_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Parcourir Photos ou Fichiers",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("remove_product_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer l'image du produit",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (previewBitmap != null) ValidatedEmeraldContainer else KineticCyanContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (previewBitmap != null) ValidatedEmerald else KineticCyan,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visuel du produit (Galerie du portable)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (previewBitmap != null) {
                                "Gemini analysera cette image (couleurs, silhouette, matières) pour concevoir les scènes."
                            } else {
                                "Importez la photo du produit depuis votre galerie ou vos fichiers avant de générer le script et le storyboard."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (previewBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioSurfaceElevated)
                    ) {
                        Image(
                            bitmap = previewBitmap!!,
                            contentDescription = "Image importée du produit ${productName.ifBlank { "" }}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (compact) 140.dp else 190.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = launchGalleryPicker,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("replace_product_image_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galerie", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = launchPhotoOrContentPicker,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("browse_product_image_files_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fichiers", maxLines = 1)
                        }
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remove_product_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Retirer l'image du produit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = launchGalleryPicker,
                            colors = ButtonDefaults.buttonColors(containerColor = KineticCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("upload_product_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ouvrir la galerie du portable",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = launchPhotoOrContentPicker,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("browse_product_image_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Photos / Fichiers de l'appareil")
                        }
                    }
                }

                pickerError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
