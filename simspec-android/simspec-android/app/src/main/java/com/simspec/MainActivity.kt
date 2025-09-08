package com.simspec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.simspec.services.LeapService
import com.simspec.services.ExportService
import com.simspec.services.PhotoAnalysisProcessor
import com.simspec.ui.SimSpecApp
import com.simspec.ui.theme.SimSpecTheme
import kotlinx.coroutines.launch

/**
 * MainActivity - Entry point for SimSpec application
 * Handles permissions, LEAP SDK initialization, and UI setup
 */
class MainActivity : ComponentActivity() {
    
    private companion object {
        const val TAG = "MainActivity"
    }
    
    private val viewModel: MainViewModel by viewModels()
    
    // Photo analysis processor for new photo capture workflow
    private lateinit var photoAnalysisProcessor: PhotoAnalysisProcessor
    
    /**
     * Camera permission launcher (audio no longer needed since we disabled it)
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Camera permission granted: $isGranted")
        if (isGranted) {
            initializeLeapService()
        } else {
            viewModel.setError("Camera permission is required for video recording")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "SimSpec starting up...")
        
        // Check camera permission
        if (hasCameraPermission()) {
            initializeLeapService()
        } else {
            requestCameraPermission()
        }
        
        // Set up Compose UI
        setContent {
            SimSpecTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    SimSpecApp(
                        uiState = uiState,
                        onCapturePhoto = { bitmap ->
                            Log.d("MainActivity", "📸 Photo captured, processing bitmap")
                            // Add photo to ViewModel which will trigger analysis when we have 3 photos
                            viewModel.capturePhoto(bitmap)
                            
                            // Check if we have 3 photos after the update
                            val currentState = viewModel.uiState.value
                            if (currentState.capturedPhotos.size == 3) {
                                Log.i("MainActivity", "🎯 3 photos collected, starting analysis")
                                if (::photoAnalysisProcessor.isInitialized) {
                                    photoAnalysisProcessor.processPhotos(currentState.capturedPhotos)
                                }
                            }
                        },
                        onResetAnalysis = { 
                            viewModel.resetPhotoWorkflow()
                            if (::photoAnalysisProcessor.isInitialized) {
                                photoAnalysisProcessor.resetForNewAnalysis()
                            }
                        },
                        onErrorDismiss = {
                            viewModel.clearError()
                        },
                        onShareResults = {
                            shareAnalysisResults(uiState)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Share analysis results using Android's share intent
     * Generates professional engineering simulation request
     */
    private fun shareAnalysisResults(uiState: MainViewModel.UiState) {
        try {
            // Get conversation history from LeapService
            val conversationHistory = LeapService.getAnalysisHistory()
            
            // Generate professional simulation request
            val simulationRequest = ExportService.generateSimulationRequest(
                uiState.analysisResults,
                conversationHistory
            )
            
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Engineering Simulation Request - SimSpec Analysis")
                putExtra(Intent.EXTRA_TEXT, simulationRequest)
            }
            
            // Launch share chooser
            val chooserIntent = Intent.createChooser(shareIntent, "Share SimSpec Analysis")
            startActivity(chooserIntent)
            
            Log.i(TAG, "Analysis results shared successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share analysis results: ${e.message}", e)
        }
    }
    
    /**
     * Initialize LEAP SDK in background
     */
    private fun initializeLeapService() {
        Log.d(TAG, "Initializing LEAP Service...")
        
        lifecycleScope.launch {
            try {
                val success = LeapService.initialize(applicationContext)
                viewModel.setInitialized(success)
                
                if (success) {
                    Log.i(TAG, "✅ LEAP Service initialization successful")
                    
                    // Initialize photo analysis processor (questions removed)
                    photoAnalysisProcessor = PhotoAnalysisProcessor(
                        viewModel = viewModel,
                        coroutineScope = lifecycleScope
                    )
                    
                    // Start photo capture workflow
                    viewModel.startPhotoCapture()
                    
                    Log.d(TAG, "PhotoAnalysisProcessor initialized")
                } else {
                    Log.e(TAG, "❌ LEAP Service initialization failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during LEAP initialization: ${e.message}", e)
                viewModel.setError("Failed to initialize AI engine: ${e.message}")
            }
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request camera permission
     */
    private fun requestCameraPermission() {
        Log.d(TAG, "Requesting camera permission...")
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")
        
        // Clear any temporary errors when app resumes
        viewModel.clearError()
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity paused")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
        
        // Cleanup for photo analysis
        if (::photoAnalysisProcessor.isInitialized) {
            photoAnalysisProcessor.resetForNewAnalysis()
        }
    }
}