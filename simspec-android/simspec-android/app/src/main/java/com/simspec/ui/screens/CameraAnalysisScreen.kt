package com.simspec.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.simspec.MainViewModel
import com.simspec.services.VideoProcessor
import com.simspec.ui.components.AnalysisResultCard
import com.simspec.ui.components.QuestionCard
import com.simspec.ui.components.ProgressIndicator
import com.simspec.utils.EngineeringQuestion
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera Analysis Screen - Main screen for live analysis
 * Combines camera preview with real-time AI analysis results
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAnalysisScreen(
    modifier: Modifier = Modifier,
    uiState: MainViewModel.UiState,
    onAnalysisResult: (String, Long) -> Unit,
    onQuestionGenerated: (EngineeringQuestion) -> Unit,
    onAnalysisComplete: () -> Unit,
    onResetAnalysis: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    onErrorDismiss: () -> Unit,
    onShareResults: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Video processor
    val videoProcessor = remember {
        VideoProcessor(
            coroutineScope = coroutineScope,
            onAnalysisResult = onAnalysisResult,
            onQuestionGenerated = { questionText, options -> 
                // Convert to EngineeringQuestion for UI
                val question = EngineeringQuestion(questionText, options)
                onQuestionGenerated(question)
            },
            onAnalysisComplete = onAnalysisComplete
        )
    }
    
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
                    "SimSpec Analysis",
                    fontWeight = FontWeight.Bold
                ) 
            },
            actions = {
                // Reset button
                IconButton(
                    onClick = {
                        videoProcessor.resetAnalysis()
                        onResetAnalysis()
                    }
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
            // Main content - Camera and Results
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Camera Preview (Left side)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        lifecycleOwner = lifecycleOwner,
                        cameraExecutor = cameraExecutor,
                        videoProcessor = videoProcessor
                    )
                    
                    // Overlay with instructions and progress
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        // Progress indicator
                        ProgressIndicator(
                            current = uiState.analysisProgress.first,
                            total = uiState.analysisProgress.second,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Instructions
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = when {
                                    uiState.isAnalysisComplete -> "✅ Analysis Complete"
                                    uiState.isAnalyzing -> "🧠 Analyzing component..."
                                    else -> "📱 Point camera at engineering component"
                                },
                                color = Color.White,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                            
                            // Share button - only show when analysis is complete
                            if (uiState.isAnalysisComplete && uiState.analysisResults.isNotEmpty()) {
                                IconButton(
                                    onClick = onShareResults
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Results",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        if (uiState.analysisResults.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Point the camera at an engineering component to begin analysis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
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
                                
                                // Current question
                                item {
                                    uiState.currentQuestion?.let { question ->
                                        QuestionCard(
                                            question = question,
                                            onAnswerSelected = { answer ->
                                                onAnswerQuestion(question.questionText, answer)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    videoProcessor: VideoProcessor
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
                    
                    // Image analysis use case
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, videoProcessor)
                        }
                    
                    // Camera selector
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    // Bind to lifecycle
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}