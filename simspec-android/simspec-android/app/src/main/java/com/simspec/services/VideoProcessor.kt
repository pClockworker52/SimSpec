package com.simspec.services

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.simspec.utils.ContextInterpreter
import com.simspec.utils.FrameExtractor
import com.simspec.utils.QuestionGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * VideoProcessor - CameraX ImageAnalysis.Analyzer implementation
 * Processes camera frames with 5-second throttling (optimized from testing)
 * Implements the progressive 3-stage analysis system
 */
class VideoProcessor(
    private val coroutineScope: CoroutineScope,
    private val onAnalysisResult: (String, Long) -> Unit,
    private val onQuestionGenerated: (String, List<String>) -> Unit,
    private val onAnalysisComplete: () -> Unit = {}
) : ImageAnalysis.Analyzer {
    
    private companion object {
        const val TAG = "VideoProcessor"
        const val ANALYSIS_INTERVAL_MS = 5000L // 5 seconds (optimized from testing)
    }

    private var lastAnalysisTime = 0L
    
    /**
     * Business-focused 3-stage analysis prompts
     * Designed for CAE project scoping, complexity assessment, and quote preparation
     */
    private val prompts = listOf(
        "Identify this mechanical system. Describe its primary function, apparent complexity (e.g., single part, small assembly, complex machine), and estimate its overall scale (e.g., handheld, desktop-sized, automotive part). Describe the main structural components and their likely materials based on visual characteristics like texture, color, and forming method (e.g., cast, machined, sheet metal).",
        "Analyze the structural connections and boundary conditions for FEA modeling. Count and classify all visible joints (e.g., welds, bolts, rivets). Describe the load-bearing paths through the structure. Identify features that suggest high stress concentrations (e.g., sharp corners, holes, sudden changes in cross-section) and where the component is likely constrained or attached to a larger system.",
        "Based on the component's apparent function and complexity, propose the most relevant type of numerical simulation (e.g., static structural, thermal, fatigue). What is the likely engineering question being answered (e.g., 'Will it break?', 'Will it get too hot?', 'How long will it last?')? Infer the potential business impact or risk being mitigated by this analysis to help justify the simulation's value."
    )
    
    private var currentPromptIndex = 0
    private var isProcessing = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        try {
            // Check throttling interval
            if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
                return
            }
            
            // Check if we've completed all prompts
            if (currentPromptIndex >= prompts.size) {
                Log.i(TAG, "✅ Progressive analysis complete")
                onAnalysisComplete()
                return
            }
            
            // Avoid concurrent processing
            if (isProcessing) {
                Log.d(TAG, "Analysis already in progress, skipping frame")
                return
            }
            
            // Convert ImageProxy to Bitmap
            val bitmap = FrameExtractor.imageProxyToBitmap(imageProxy)
            if (bitmap == null) {
                Log.w(TAG, "Failed to convert ImageProxy to Bitmap")
                return
            }
            
            // Resize for performance (512x512 as per testing, may optimize to 384x384 later)
            val resizedBitmap = FrameExtractor.resizeBitmap(bitmap, 512, 512)
            val currentPrompt = prompts[currentPromptIndex]
            
            Log.d(TAG, "🎯 Starting analysis step ${currentPromptIndex + 1}/${prompts.size}")
            Log.d(TAG, "Prompt: $currentPrompt")
            
            isProcessing = true
            lastAnalysisTime = currentTime
            
            // Launch analysis in background
            coroutineScope.launch {
                try {
                    val (result, inferenceTime, stats) = LeapService.analyzeImage(resizedBitmap, currentPrompt, currentPromptIndex + 1)
                    
                    if (result != null && result.isNotBlank() && !result.startsWith("Error")) {
                        // Deliver analysis result to UI
                        onAnalysisResult("Step ${currentPromptIndex + 1}: $result", inferenceTime)
                        
                        // Generate engineering context and questions
                        val analysisType = ContextInterpreter.getAnalysisType(result)
                        val question = QuestionGenerator.generateQuestion(analysisType)
                        
                        if (question != null) {
                            Log.d(TAG, "📋 Generated question: ${question.questionText}")
                            onQuestionGenerated(question.questionText, question.options)
                        }
                        
                        // Move to next prompt
                        currentPromptIndex++
                        
                        Log.i(TAG, "✅ Step ${currentPromptIndex} complete in ${inferenceTime}ms")
                        
                    } else {
                        Log.w(TAG, "⚠️ Analysis failed or returned error: $result")
                        onAnalysisResult("Analysis failed: ${result ?: "Unknown error"}", inferenceTime)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Analysis error: ${e.message}", e)
                    onAnalysisResult("Analysis error: ${e.message}", 0L)
                } finally {
                    isProcessing = false
                }
            }
            
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Reset the analysis to start from the beginning
     * Useful for analyzing a new component
     */
    fun resetAnalysis() {
        Log.i(TAG, "🔄 Resetting progressive analysis")
        currentPromptIndex = 0
        isProcessing = false
        lastAnalysisTime = 0L
        // Reset LeapService conversation context for new component
        LeapService.resetAnalysis()
    }
    
    /**
     * Get current analysis progress
     */
    fun getProgress(): Pair<Int, Int> = currentPromptIndex to prompts.size
    
    /**
     * Check if analysis is complete
     */
    fun isAnalysisComplete(): Boolean = currentPromptIndex >= prompts.size
}