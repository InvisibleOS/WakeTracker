package com.shivansh.waketracker.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shivansh.waketracker.NfcProvisioningState
import com.shivansh.waketracker.NfcProvisioningViewModel
import kotlinx.coroutines.delay

@Composable
fun NfcReaderModeEffect(
    isActive: Boolean,
    onTagDiscovered: (Tag) -> Unit,
    onNfcDisabled: () -> Unit = {},
    onNfcEnabled: () -> Unit = {}
) {
    val context = LocalContext.current
    
    DisposableEffect(isActive, context) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val activity = context.findActivity()
        
        if (nfcAdapter == null || !nfcAdapter.isEnabled) {
            onNfcDisabled()
        } else {
            onNfcEnabled()
        }

        if (isActive && activity != null && nfcAdapter != null && nfcAdapter.isEnabled) {
            val flags = NfcAdapter.FLAG_READER_NFC_A or 
                        NfcAdapter.FLAG_READER_NFC_B or 
                        NfcAdapter.FLAG_READER_NFC_F or 
                        NfcAdapter.FLAG_READER_NFC_V
            
            try {
                nfcAdapter.enableReaderMode(
                    activity,
                    { tag -> onTagDiscovered(tag) },
                    flags,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            onDispose {
                try {
                    nfcAdapter.disableReaderMode(activity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            onDispose { }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcProvisioningSheet(
    viewModel: NfcProvisioningViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState = viewModel.uiState
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // Active reader mode effect driven by the UI state!
        NfcReaderModeEffect(
            isActive = uiState == NfcProvisioningState.SCANNING,
            onTagDiscovered = viewModel::onTagDiscovered,
            onNfcDisabled = viewModel::onNfcDisabled,
            onNfcEnabled = viewModel::onNfcEnabled
        )
        
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                (fadeIn(tween(400, easing = EaseInOutCubic)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400, easing = EaseOutBack)))
                    .togetherWith(fadeOut(tween(300, easing = EaseInOutCubic)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300)))
            },
            label = "NfcProvisioningContent",
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            Box(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    NfcProvisioningState.SCANNING -> ScanningState()
                    NfcProvisioningState.SUCCESS -> SuccessState(onDismissRequest)
                    NfcProvisioningState.FAILURE -> FailureState(viewModel::retry)
                    NfcProvisioningState.NFC_OFF -> NfcDisabledState(onDismissRequest)
                }
            }
        }
        
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ScanningState() {
    // Pulsing and rotating animation for expressive hardware connection indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Higher container to prevent the pulsing circle from being clipped
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "Scanning",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ready to Scan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Hold your phone near the NFC tag\nto set it up for your wake tracking.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun SuccessState(onDone: () -> Unit) {
    // Auto-dismiss after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        onDone()
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tag Ready!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your wake tag has been configured.\nYou can now scan it to log your wake time.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FailureState(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "The tag couldn't be written. Try holding your phone steadier or moving it slightly.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun NfcDisabledState(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = "NFC Off",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NFC is Disabled",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Please enable NFC in your system settings to provision a wake tag.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
            },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Settings")
        }
        TextButton(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}

private fun android.view.animation.Interpolator.toEasing() = Easing { fraction ->
    this.getInterpolation(fraction)
}
