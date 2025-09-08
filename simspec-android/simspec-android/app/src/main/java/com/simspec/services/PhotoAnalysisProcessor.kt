package com.simspec.services

import android.graphics.Bitmap
import android.util.Log
import com.simspec.MainViewModel
import com.simspec.services.LeapService
import com.simspec.utils.FrameExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * PhotoAnalysisProcessor - Handles direct photo analysis workflow
 * Replaces the complex video recording approach with simple photo processing
 */
class PhotoAnalysisProcessor(
    private val viewModel: MainViewModel,
    private val coroutineScope: CoroutineScope
) {
    
    private companion object {
        const val TAG = "PhotoAnalysisProcessor"
    }
    
    /**
     * Progressive focal length analysis prompts
     * Wide View → Medium View → Close-up with different response formats
     */
    private val prompts = listOf(
        // Stage 1 - WIDE VIEW: Context & Overview (prose for system understanding)
        "Describe what you see. What does this do? How big? What environment? What forces or loads would it experience?",
        
        // Stage 2 - MEDIUM VIEW: Inventory & Assembly (structured lists)
        "List: PARTS:[visible components] FASTENERS:[bolts/screws/welds] JOINTS:[fixed/rotating/sliding] MOUNTING:[how attached]",
        
        // Stage 3 - CLOSE-UP: Focus Point Analysis (targeted detail)
        "CENTER FOCUS: What specific feature is centered? Is it: bolt/weld/hole/corner? Load path through this point? Failure risk?"
    )
    
    /**
     * Process 3 captured photos through the complete analysis pipeline
     * 
     * @param photos List of 3 captured photos as Bitmaps
     */
    fun processPhotos(photos: List<Bitmap>) {
        coroutineScope.launch {
            try {
                Log.i(TAG, "🎬 Starting photo analysis workflow with ${photos.size} photos")
                
                if (photos.size != 3) {
                    viewModel.setPhotoError("Expected 3 photos, got ${photos.size}")
                    return@launch
                }
                
                // Stage 1: Resize photos for optimal performance
                Log.d(TAG, "🖼️ Resizing ${photos.size} photos to 512x512...")
                val resizedPhotos = withContext(Dispatchers.IO) {
                    try {
                        photos.mapIndexed { index, photo ->
                            Log.d(TAG, "📐 Resizing photo ${index + 1}/${photos.size}")
                            FrameExtractor.resizeBitmap(photo, 512, 512)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Photo resizing failed: ${e.message}", e)
                        throw e
                    }
                }
                
                Log.i(TAG, "✅ Photo resizing completed: ${resizedPhotos.size} photos ready for analysis")
                
                // Stage 2: Run analysis on each photo with corresponding prompt
                for ((index, photo) in resizedPhotos.withIndex()) {
                    val currentPrompt = prompts[index]
                    val stage = index + 1
                    
                    Log.i(TAG, "🎯 Starting analysis stage $stage/${prompts.size}")
                    Log.d(TAG, "🤖 Prompt: ${currentPrompt.take(100)}...")
                    
                    try {
                        Log.d(TAG, "🧠 Calling LeapService.analyzeImage for stage $stage...")
                        
                        // Run LEAP analysis on this photo with timeout handling
                        val (result, inferenceTime, stats) = withContext(Dispatchers.IO) {
                            withTimeout(60000L) { // 60 second timeout per stage
                                LeapService.analyzeImage(photo, currentPrompt, stage)
                            }
                        }
                        
                        Log.i(TAG, "✅ LeapService returned for stage $stage in ${inferenceTime}ms")
                        Log.d(TAG, "📊 Stage $stage stats: $stats")
                        
                        if (result != null && result.isNotBlank() && !result.startsWith("Error")) {
                            // Add analysis result to ViewModel
                            viewModel.addAnalysisResult("Stage $stage: $result", inferenceTime)
                            Log.i(TAG, "✅ Stage $stage complete in ${inferenceTime}ms")
                            
                        } else {
                            Log.w(TAG, "⚠️ Stage $stage analysis failed or returned error: $result")
                            viewModel.addAnalysisResult("Stage $stage failed: ${result ?: "Unknown error"}", inferenceTime)
                        }
                        
                        // Update progress - this is crucial for UI updates
                        Log.d(TAG, "📈 Updating progress: stage $stage of ${prompts.size}")
                        viewModel.updateAnalysisProgress(stage, prompts.size)
                        
                    } catch (timeout: TimeoutCancellationException) {
                        Log.e(TAG, "⏰ Stage $stage timed out after 60 seconds")
                        viewModel.addAnalysisResult("Stage $stage timed out", 0L)
                        viewModel.updateAnalysisProgress(stage, prompts.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Stage $stage analysis error: ${e.message}", e)
                        viewModel.addAnalysisResult("Stage $stage error: ${e.message}", 0L)
                        viewModel.updateAnalysisProgress(stage, prompts.size)
                    }
                    
                    // Add a small delay between stages to avoid overwhelming the system
                    kotlinx.coroutines.delay(1000L)
                }
                
                // Stage 3: Complete analysis
                Log.i(TAG, "✅ Complete photo analysis workflow finished")
                viewModel.setAnalysisComplete()
                viewModel.completePhotoAnalysis()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Photo analysis workflow failed: ${e.message}", e)
                viewModel.setPhotoError("Analysis failed: ${e.message}")
            }
        }
    }
    
    /**
     * Reset analysis state for new photos
     */
    fun resetForNewAnalysis() {
        Log.i(TAG, "🔄 Resetting for new photo analysis")
        viewModel.resetPhotoWorkflow()
        LeapService.resetAnalysis()
    }
}