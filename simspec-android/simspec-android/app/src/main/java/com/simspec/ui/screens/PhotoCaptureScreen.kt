package com.simspec.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil3.compose.AsyncImage
import com.simspec.MainViewModel
import com.simspec.ui.components.AnalysisResultCard
import com.simspec.ui.components.QuantifiedProgressIndicator
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Photo Capture Screen - Simplified 3-photo capture workflow
 * Replaces the complex video recording with simple photo capture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    modifier: Modifier = Modifier,
    uiState: MainViewModel.UiState,
    onCapturePhoto: (Bitmap) -> Unit,
    onResetAnalysis: () -> Unit,
    onErrorDismiss: () -> Unit,
    onShareResults: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Image capture
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    // Scroll state for results
    val scrollState = rememberLazyListState()
    
    // Auto-scroll to latest result
    LaunchedEffect(uiState.analysisResults.size) {
        if (uiState.analysisResults.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.analysisResults.size - 1)
        }
    }
    
    // Clean up camera executor
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    "SimSpec Photo Analysis",
                    fontWeight = FontWeight.Bold
                ) 
            },
            actions = {
                // Reset button
                IconButton(
                    onClick = onResetAnalysis,
                    enabled = uiState.photoState == MainViewModel.PhotoState.IDLE || 
                             uiState.photoState == MainViewModel.PhotoState.COMPLETED
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Analysis")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Error handling
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onErrorDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
        
        // Check if initialized
        if (!uiState.isInitialized) {
            // Loading screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initializing AI Engine...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "This may take a few seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Main content
            when (uiState.photoState) {
                MainViewModel.PhotoState.IDLE,
                MainViewModel.PhotoState.CAPTURE_1,
                MainViewModel.PhotoState.CAPTURE_2,
                MainViewModel.PhotoState.CAPTURE_3 -> {
                    PhotoCaptureState(
                        modifier = Modifier.weight(1f),
                        uiState = uiState,
                        lifecycleOwner = lifecycleOwner,
                        cameraExecutor = cameraExecutor,
                        onImageCaptureReady = { capture ->
                            imageCapture = capture
                        },
                        onTriggerCapture = {
                            imageCapture?.let { capture ->
                                Log.d("PhotoCapture", "📸 Taking photo ${uiState.currentPhotoStep}")
                                
                                // Capture image in memory instead of to file  
                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                            Log.d("PhotoCapture", "✅ Photo ${uiState.currentPhotoStep} captured successfully")
                                            
                                            // Convert ImageProxy to Bitmap
                                            val bitmap = com.simspec.utils.FrameExtractor.imageProxyToBitmap(image)
                                            
                                            if (bitmap != null) {
                                                Log.d("PhotoCapture", "✅ Bitmap created: ${com.simspec.utils.FrameExtractor.getBitmapInfo(bitmap)}")
                                                // Call the onCapturePhoto callback with the bitmap - this will update the ViewModel
                                                onCapturePhoto(bitmap)
                                            } else {
                                                Log.e("PhotoCapture", "❌ Failed to convert ImageProxy to Bitmap")
                                            }
                                            
                                            image.close()
                                        }
                                        
                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("PhotoCapture", "❌ Photo capture failed: ${exception.message}", exception)
                                        }
                                    }
                                )
                            }
                        },
                        onCapturePhoto = onCapturePhoto
                    )
                }
                
                MainViewModel.PhotoState.ANALYZING -> {
                    AnalyzingState(
                        modifier = Modifier.weight(1f),
                        uiState = uiState,
                        scrollState = scrollState,
                        onShareResults = onShareResults
                    )
                }
                
                MainViewModel.PhotoState.COMPLETED -> {
                    CompletedState(
                        modifier = Modifier.weight(1f),
                        uiState = uiState,
                        scrollState = scrollState,
                        onShareResults = onShareResults
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCaptureState(
    modifier: Modifier = Modifier,
    uiState: MainViewModel.UiState,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onTriggerCapture: () -> Unit,
    onCapturePhoto: (Bitmap) -> Unit
) {
    Row(modifier = modifier) {
        // Camera Preview (Left side)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CameraPreviewForPhotos(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                onImageCaptureReady = onImageCaptureReady
            )
            
            // Photo capture controls overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                // Large capture button
                FloatingActionButton(
                    onClick = onTriggerCapture,
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Take Photo",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
            
            // Instructions overlay
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = getInstructionText(uiState.photoState, uiState.currentPhotoStep),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Progress Panel (Right side)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "3-Photo Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress indicator (photo capture progress)
                QuantifiedProgressIndicator(
                    current = uiState.currentPhotoStep - 1,
                    total = 3,
                    stageTimeSeconds = 10, // Quick photo capture, 10s per photo
                    timerActive = false, // No timer during photo capture
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = """
                        Photo ${uiState.currentPhotoStep} of 3
                        
                        ${getDetailedInstructions(uiState.photoState)}
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show captured photos
                if (uiState.capturedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Captured Photos:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.capturedPhotos) { photo ->
                            AsyncImage(
                                model = photo,
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzingState(
    modifier: Modifier = Modifier,
    uiState: MainViewModel.UiState,
    scrollState: LazyListState,
    onShareResults: () -> Unit
) {
    Row(modifier = modifier) {
        // Photos Panel (Left side)
        if (uiState.capturedPhotos.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Captured Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.capturedPhotos.withIndex().toList()) { (index, photo) ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = "Photo ${index + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                    Text(
                                        text = "Photo ${index + 1}",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Results Panel (Right side)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Results header with share button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analysis Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Share button - show when analysis is complete
                    if (uiState.isAnalysisComplete && uiState.analysisResults.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = onShareResults,
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Results",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Quantified analysis progress with time estimates
                QuantifiedProgressIndicator(
                    current = uiState.analysisProgress.first,
                    total = uiState.analysisProgress.second,
                    stageTimeSeconds = 40, // 40 seconds per analysis stage
                    timerActive = true, // Timer active during analysis
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Current stage message
                if (uiState.processingProgress != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.processingProgress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Results list
                LazyColumn(
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.analysisResults) { result ->
                        AnalysisResultCard(
                            result = result,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Questions removed - focus on pure technical analysis
                }
            }
        }
    }
}

@Composable
private fun CompletedState(
    modifier: Modifier = Modifier,
    uiState: MainViewModel.UiState,
    scrollState: LazyListState,
    onShareResults: () -> Unit
) {
    // Same as AnalyzingState but with share button enabled
    AnalyzingState(
        modifier = modifier,
        uiState = uiState,
        scrollState = scrollState,
        onShareResults = onShareResults
    )
}

@Composable
private fun CameraPreviewForPhotos(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    // Preview use case
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    // Image capture use case
                    val imageCapture = ImageCapture.Builder()
                        .build()
                    
                    // Camera selector
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    // Bind to lifecycle
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    onImageCaptureReady(imageCapture)
                    
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun getInstructionText(photoState: MainViewModel.PhotoState, currentStep: Int): String {
    return when (photoState) {
        MainViewModel.PhotoState.IDLE -> "📸 Ready to start 3-photo analysis"
        MainViewModel.PhotoState.CAPTURE_1 -> "📸 Photo 1: Capture overview of component"
        MainViewModel.PhotoState.CAPTURE_2 -> "📸 Photo 2: Capture connections & joints"
        MainViewModel.PhotoState.CAPTURE_3 -> "📸 Photo 3: Capture problem areas"
        else -> "📸 Photo capture complete"
    }
}

private fun getDetailedInstructions(photoState: MainViewModel.PhotoState): String {
    return when (photoState) {
        MainViewModel.PhotoState.IDLE -> "Take 3 photos of the engineering component from different angles for comprehensive analysis."
        MainViewModel.PhotoState.CAPTURE_1 -> "Position the camera to show the overall component. Include the complete structure and its general shape."
        MainViewModel.PhotoState.CAPTURE_2 -> "Focus on connections, joints, welds, bolts, or any interfaces between parts."
        MainViewModel.PhotoState.CAPTURE_3 -> "Capture any areas of concern, wear, damage, or critical stress points."
        else -> "All photos captured successfully."
    }
}