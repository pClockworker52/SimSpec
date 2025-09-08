package com.simspec

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simspec.utils.EngineeringQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * MainViewModel - Manages the application state for SimSpec
 * Handles analysis results, questions, and UI state
 */
class MainViewModel : ViewModel() {
    
    private companion object {
        const val TAG = "MainViewModel"
    }
    
    // Photo capture states for simplified 3-photo workflow
    enum class PhotoState {
        IDLE,           // Ready to start photo capture
        CAPTURE_1,      // Ready to capture photo 1
        CAPTURE_2,      // Ready to capture photo 2  
        CAPTURE_3,      // Ready to capture photo 3
        ANALYZING,      // Running AI analysis on photos
        COMPLETED       // Analysis complete
    }
    
    // UI State data class
    data class UiState(
        val isInitialized: Boolean = false,
        val isAnalyzing: Boolean = false,
        val analysisResults: List<AnalysisResult> = emptyList(),
        val currentQuestion: EngineeringQuestion? = null,
        val analysisProgress: Pair<Int, Int> = 0 to 3, // current stage to total stages
        val errorMessage: String? = null,
        val isAnalysisComplete: Boolean = false,
        // Photo capture workflow state
        val photoState: PhotoState = PhotoState.IDLE,
        val currentPhotoStage: Int = 1, // 1, 2, or 3
        val capturedPhotos: List<Bitmap> = emptyList(),
        val processingProgress: String? = null
    )
    
    // Analysis result data class
    data class AnalysisResult(
        val stage: Int,
        val text: String,
        val inferenceTime: Long,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // Private mutable state
    private val _uiState = MutableStateFlow(UiState())
    
    // Public read-only state
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * Update initialization status
     */
    fun setInitialized(isInitialized: Boolean) {
        Log.d(TAG, "Setting initialization status: $isInitialized")
        _uiState.value = _uiState.value.copy(
            isInitialized = isInitialized,
            errorMessage = if (isInitialized) null else "Failed to initialize LEAP SDK"
        )
    }
    
    /**
     * Add a new analysis result
     */
    fun addAnalysisResult(resultText: String, inferenceTime: Long) {
        val currentResults = _uiState.value.analysisResults
        val newStage = currentResults.size + 1
        
        val newResult = AnalysisResult(
            stage = newStage,
            text = resultText,
            inferenceTime = inferenceTime
        )
        
        Log.d(TAG, "Adding analysis result - Stage $newStage: ${resultText.take(50)}...")
        
        _uiState.value = _uiState.value.copy(
            analysisResults = currentResults + newResult,
            analysisProgress = newStage to 3,
            errorMessage = null
        )
    }
    
    /**
     * Set the current engineering question
     */
    fun setCurrentQuestion(question: EngineeringQuestion) {
        Log.d(TAG, "Setting current question: ${question.questionText}")
        _uiState.value = _uiState.value.copy(
            currentQuestion = question
        )
    }
    
    /**
     * Update analysis progress
     */
    fun updateAnalysisProgress(current: Int, total: Int) {
        Log.d(TAG, "Updating progress: $current/$total")
        _uiState.value = _uiState.value.copy(
            analysisProgress = current to total,
            isAnalyzing = current < total
        )
    }
    
    /**
     * Set analysis as complete
     */
    fun setAnalysisComplete() {
        Log.i(TAG, "Analysis marked as complete")
        _uiState.value = _uiState.value.copy(
            isAnalysisComplete = true,
            isAnalyzing = false
        )
    }
    
    /**
     * Set analyzing status
     */
    fun setAnalyzing(isAnalyzing: Boolean) {
        Log.d(TAG, "Setting analyzing status: $isAnalyzing")
        _uiState.value = _uiState.value.copy(
            isAnalyzing = isAnalyzing
        )
    }
    
    /**
     * Set error message
     */
    fun setError(message: String) {
        Log.e(TAG, "Setting error: $message")
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            isAnalyzing = false
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Reset all analysis data (for analyzing a new component)
     */
    fun resetAnalysis() {
        Log.i(TAG, "Resetting analysis data")
        _uiState.value = _uiState.value.copy(
            analysisResults = emptyList(),
            currentQuestion = null,
            analysisProgress = 0 to 3,
            isAnalysisComplete = false,
            isAnalyzing = false,
            errorMessage = null
        )
    }
    
    /**
     * Handle question answer (for future implementation)
     */
    fun answerQuestion(questionId: String, answer: String) {
        Log.d(TAG, "Question answered: $questionId = $answer")
        // TODO: Store answer for final report generation
        
        // Clear current question to show it's been answered
        _uiState.value = _uiState.value.copy(
            currentQuestion = null
        )
    }
    
    /**
     * Get analysis summary for export/sharing
     */
    fun getAnalysisSummary(): String {
        val state = _uiState.value
        val results = state.analysisResults
        
        if (results.isEmpty()) return "No analysis performed"
        
        return buildString {
            appendLine("SimSpec Analysis Report")
            appendLine("Generated: ${System.currentTimeMillis()}")
            appendLine("Stages Completed: ${results.size}/3")
            appendLine()
            
            results.forEachIndexed { index, result ->
                appendLine("Stage ${result.stage}:")
                appendLine(result.text)
                appendLine("Inference Time: ${result.inferenceTime}ms")
                appendLine()
            }
            
            val avgInferenceTime = results.map { it.inferenceTime }.average()
            appendLine("Average Inference Time: ${avgInferenceTime.toInt()}ms")
            
            if (state.isAnalysisComplete) {
                appendLine("Status: Analysis Complete")
            } else {
                appendLine("Status: Analysis In Progress")
            }
        }
    }
    
    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        val results = _uiState.value.analysisResults
        
        return if (results.isEmpty()) {
            mapOf("status" to "No data")
        } else {
            mapOf(
                "total_stages" to results.size,
                "avg_inference_time_ms" to results.map { it.inferenceTime }.average().toInt(),
                "min_inference_time_ms" to (results.minOfOrNull { it.inferenceTime } ?: 0L),
                "max_inference_time_ms" to (results.maxOfOrNull { it.inferenceTime } ?: 0L),
                "total_analysis_time_ms" to results.sumOf { it.inferenceTime },
                "is_complete" to _uiState.value.isAnalysisComplete
            )
        }
    }
    
    // Photo Capture Workflow Methods
    
    /**
     * Capture a photo and advance to next stage
     */
    fun capturePhoto(photoBitmap: Bitmap) {
        val currentStage = _uiState.value.currentPhotoStage
        val newPhotos = _uiState.value.capturedPhotos + photoBitmap
        
        Log.d(TAG, "📸 Photo $currentStage captured, total photos: ${newPhotos.size}")
        
        when (currentStage) {
            1 -> {
                _uiState.value = _uiState.value.copy(
                    photoState = PhotoState.CAPTURE_2,
                    currentPhotoStage = 2,
                    capturedPhotos = newPhotos,
                    errorMessage = null
                )
            }
            2 -> {
                _uiState.value = _uiState.value.copy(
                    photoState = PhotoState.CAPTURE_3,
                    currentPhotoStage = 3,
                    capturedPhotos = newPhotos
                )
            }
            3 -> {
                _uiState.value = _uiState.value.copy(
                    photoState = PhotoState.ANALYZING,
                    capturedPhotos = newPhotos,
                    processingProgress = "Starting analysis of 3 photos...",
                    isAnalyzing = true
                )
                Log.i(TAG, "✅ All 3 photos captured, starting analysis")
            }
        }
    }
    
    /**
     * Start the photo capture workflow
     */
    fun startPhotoCapture() {
        Log.d(TAG, "Starting 3-photo capture workflow")
        _uiState.value = _uiState.value.copy(
            photoState = PhotoState.CAPTURE_1,
            currentPhotoStage = 1,
            capturedPhotos = emptyList(),
            errorMessage = null
        )
    }
    
    /**
     * Update processing progress message
     */
    fun updateProcessingProgress(message: String) {
        _uiState.value = _uiState.value.copy(
            processingProgress = message
        )
    }
    
    /**
     * Complete photo analysis workflow
     */
    fun completePhotoAnalysis() {
        Log.i(TAG, "Photo analysis workflow complete")
        _uiState.value = _uiState.value.copy(
            photoState = PhotoState.COMPLETED,
            processingProgress = null,
            isAnalysisComplete = true,
            isAnalyzing = false
        )
    }
    
    /**
     * Reset photo workflow (for new analysis)
     */
    fun resetPhotoWorkflow() {
        Log.i(TAG, "Resetting photo workflow")
        _uiState.value = _uiState.value.copy(
            photoState = PhotoState.IDLE,
            currentPhotoStage = 1,
            capturedPhotos = emptyList(),
            processingProgress = null,
            analysisResults = emptyList(),
            currentQuestion = null,
            analysisProgress = 0 to 3,
            isAnalysisComplete = false,
            isAnalyzing = false,
            errorMessage = null
        )
    }
    
    /**
     * Handle photo capture error
     */
    fun setPhotoError(error: String) {
        Log.e(TAG, "Photo capture error: $error")
        _uiState.value = _uiState.value.copy(
            errorMessage = "Photo capture failed: $error",
            processingProgress = null
        )
    }
}