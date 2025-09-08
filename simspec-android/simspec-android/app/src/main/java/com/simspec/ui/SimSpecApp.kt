package com.simspec.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.simspec.MainViewModel
import com.simspec.ui.screens.PhotoCaptureScreen

/**
 * Main SimSpec app composable
 * Orchestrates the navigation and screen management
 */
@Composable
fun SimSpecApp(
    uiState: MainViewModel.UiState,
    onCapturePhoto: (Bitmap) -> Unit,
    onResetAnalysis: () -> Unit,
    onErrorDismiss: () -> Unit,
    onShareResults: () -> Unit
) {
    Scaffold { paddingValues ->
        // Updated to use new photo capture workflow
        PhotoCaptureScreen(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onCapturePhoto = onCapturePhoto,
            onResetAnalysis = onResetAnalysis,
            onErrorDismiss = onErrorDismiss,
            onShareResults = onShareResults
        )
    }
}