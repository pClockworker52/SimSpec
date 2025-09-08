package com.simspec.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.last
import kotlin.system.measureTimeMillis
import ai.liquid.leap.LeapClient
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.Conversation
import ai.liquid.leap.ModelLoadingOptions
import ai.liquid.leap.GenerationOptions
import ai.liquid.leap.message.ChatMessage
import ai.liquid.leap.message.ChatMessageContent
import ai.liquid.leap.message.MessageResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take

/**
 * LeapService - Singleton service for LEAP SDK integration
 * Handles model initialization and AI inference operations
 * Optimized based on desktop testing results
 */
object LeapService {
    
    private const val TAG = "LeapService"
    
    private var modelRunner: ModelRunner? = null
    private var conversation: Conversation? = null
    private var isInitialized = false
    
    // Analysis state management for context tracking
    private val analysisHistory = mutableListOf<ChatMessage>()
    private var currentStage = 0
    
    /**
     * Initialize the LEAP SDK with the LFM2-VL model using optimal settings
     * Expected load time: ~2.5 seconds based on testing
     * Call this once from MainActivity or Application class
     */
    suspend fun initialize(context: Context): Boolean {
        if (isInitialized) {
            Log.d(TAG, "LEAP SDK already initialized")
            return true
        }

        return withContext(Dispatchers.IO) {
            try {
                // Switch to LFM2-VL-1.6B for better hackathon performance
                val modelPath = "/data/local/tmp/leap/lfm2-vl-1.6b.bundle"
                
                Log.i(TAG, "🚀 Initializing LEAP SDK with LFM2-VL-1.6B for hackathon...")
                Log.d(TAG, "📍 Model path: $modelPath")
                
                val initTime = measureTimeMillis {
                    // Optimized loading options for LFM2-VL-1.6B
                    val loadingOptions = ModelLoadingOptions.build {
                        cpuThreads = 6 // Use more threads for 1.6B model
                        randomSeed = System.currentTimeMillis() // Varied responses
                        // Add any 1.6B-specific optimizations here
                    }
                    
                    // Check if model file exists
                    val modelFile = java.io.File(modelPath)
                    Log.d(TAG, "🔍 Model file exists: ${modelFile.exists()}")
                    Log.d(TAG, "📏 Model file size: ${if (modelFile.exists()) "${modelFile.length() / 1024 / 1024} MB" else "N/A"}")
                    
                    if (!modelFile.exists()) {
                        throw Exception("Model file not found at $modelPath. Did you deploy the LFM2-VL-1.6B model?")
                    }
                    
                    // Load model with enhanced error handling
                    Log.d(TAG, "📥 Loading model with ${loadingOptions}...")
                    val result = LeapClient.loadModelAsResult(modelPath, loadingOptions)
                    
                    if (result.isFailure) {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "❌ Model loading failed: ${error?.message}", error)
                        throw error ?: Exception("Unknown model loading failure")
                    }
                    
                    modelRunner = result.getOrThrow()
                    Log.d(TAG, "✅ ModelRunner created successfully")
                    
                    // Create initial conversation - will be reset between stages to prevent context buildup
                    conversation = modelRunner!!.createConversation(
                        "You are an engineering analysis AI. Provide concise technical analysis in the exact format requested."
                    )
                }
                
                isInitialized = true
                Log.i(TAG, "✅ LEAP SDK Initialized Successfully in ${initTime}ms")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ LEAP SDK Initialization Failed: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Analyze an image with stage-aware context and optimal generation settings
     * Uses REAL LEAP SDK Vision-Language capabilities based on VLM example
     * 
     * @param bitmap The image to analyze with the LFM2-VL model
     * @param prompt The analysis prompt for current stage
     * @param stage Current analysis stage (1-3)
     * @return Triple of (analysis result, inference time, generation stats)
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String, stage: Int = 1): Triple<String?, Long, Map<String, Any>> {
        if (!isInitialized || conversation == null) {
            Log.e(TAG, "LEAP SDK not initialized")
            return Triple("Error: LEAP SDK not initialized", 0L, emptyMap())
        }

        currentStage = stage
        var analysisResult: String? = null
        var stats: Map<String, Any> = mapOf("stage" to stage)
        
        val inferenceTime = measureTimeMillis {
            withContext(Dispatchers.IO) {
                try {
                    Log.i(TAG, "🎯 Starting Stage $stage VISION analysis")
                    Log.d(TAG, "📝 Base prompt length: ${prompt.length} chars")
                    
                    // Convert bitmap to byte array (JPEG format for LFM2-VL)
                    Log.d(TAG, "🖼️ Converting bitmap to byte array...")
                    val imageByteArray = bitmapToByteArray(bitmap)
                    Log.d(TAG, "📏 Image size: ${imageByteArray.size / 1024} KB")
                    
                    // Stage-specific context prompt with conversation history
                    Log.d(TAG, "🔧 Building contextual prompt for stage $stage...")
                    val contextualPrompt = buildStagePrompt(prompt, stage)
                    Log.d(TAG, "📝 Contextual prompt length: ${contextualPrompt.length} chars")
                    
                    // Create ChatMessage with BOTH image and text (following VLM example pattern)
                    Log.d(TAG, "💬 Creating ChatMessage with image and text...")
                    val userMessage = ChatMessage(
                        role = ChatMessage.Role.USER,
                        content = listOf(
                            ChatMessageContent.Text(contextualPrompt),
                            ChatMessageContent.Image(imageByteArray)
                        )
                    )
                    
                    // Reset conversation for each stage to prevent context buildup
                    if (stage > 1) {
                        Log.i(TAG, "🔄 Creating fresh conversation for stage $stage to prevent timeout")
                        conversation = modelRunner!!.createConversation("You are an engineering analysis AI. Provide concise technical analysis.")
                    }
                    
                    // Add to conversation history for context (but conversation is reset per stage)
                    analysisHistory.add(userMessage)
                    Log.d(TAG, "📚 Analysis history size: ${analysisHistory.size}, Fresh conversation for stage $stage")
                    
                    // Generate response using default model settings (like official VLM example)
                    Log.i(TAG, "🚀 Starting response generation with default settings...")
                    val responseFlow = conversation!!.generateResponse(userMessage)
                    
                    // Collect streaming response with simplified collection
                    var isGenerationStarted = false
                    var chunkCount = 0
                    val maxChunks = if (stage == 3) 25 else 50 // Even more aggressive for stage 3
                    Log.d(TAG, "📡 Starting response collection...")
                    
                    // Use take() to limit the flow to prevent infinite generation
                    responseFlow
                        .take(maxChunks + 1) // +1 to allow for Complete message
                        .collect { response: MessageResponse ->
                            when (response) {
                                is MessageResponse.Chunk -> {
                                    chunkCount++
                                    
                                    if (!isGenerationStarted) {
                                        isGenerationStarted = true
                                        analysisResult = ""
                                        Log.i(TAG, "✅ First chunk received - generation started!")
                                    }
                                    
                                    analysisResult = (analysisResult ?: "") + response.text
                                    Log.v(TAG, "📨 Stage $stage Chunk $chunkCount: ${response.text.take(50)}...")
                                    
                                    // Early exit if we have enough content (more aggressive for stage 3)
                                    val targetLength = if (stage == 3) 300 else 500
                                    if (analysisResult!!.length >= targetLength) {
                                        Log.i(TAG, "🏁 Stage $stage reached sufficient length: ${analysisResult!!.length} chars")
                                        return@collect
                                    }
                                }
                            is MessageResponse.Complete -> {
                                Log.i(TAG, "🏁 Stage $stage generation complete!")
                                
                                // Extract final text content
                                val generatedContent = response.fullMessage.content.first() as ChatMessageContent.Text
                                analysisResult = generatedContent.text
                                Log.d(TAG, "📄 Final result length: ${analysisResult?.length ?: 0} chars")
                                
                                // Add assistant response to history for context in next stages
                                analysisHistory.add(response.fullMessage)
                                
                                // Extract generation statistics
                                stats = mapOf<String, Any>(
                                    "stage" to stage,
                                    "tokens_generated" to "available", 
                                    "finish_reason" to response.finishReason.toString(),
                                    "chunks_received" to chunkCount
                                )
                                
                                Log.d(TAG, "📊 Stage $stage stats: $stats")
                            }
                            is MessageResponse.FunctionCalls -> {
                                // Handle function calls if needed
                                Log.d(TAG, "Function calls received: ${response.functionCalls}")
                            }
                            is MessageResponse.ReasoningChunk -> {
                                // Handle reasoning chunks
                                Log.v(TAG, "Reasoning chunk received")
                            }
                        }
                    }
                    
                    // Handle early termination case
                    if (analysisResult != null && analysisResult!!.isNotBlank() && chunkCount >= maxChunks) {
                        Log.i(TAG, "🏁 Stage $stage completed with flow limitation after $chunkCount chunks")
                        // Add to conversation history 
                        val terminatedResponse = ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = listOf(ChatMessageContent.Text(analysisResult!!))
                        )
                        analysisHistory.add(terminatedResponse)
                        
                        stats = mapOf<String, Any>(
                            "stage" to stage,
                            "chunks_received" to chunkCount,
                            "flow_limited" to true,
                            "result_length" to analysisResult!!.length
                        )
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ LEAP Vision Analysis Failed: ${e.message}", e)
                    analysisResult = "Error during vision analysis: ${e.message}"
                }
            }
        }

        Log.i(TAG, "🧠👁️ Stage $stage Vision Analysis complete in ${inferenceTime}ms")
        
        return Triple(analysisResult, inferenceTime, stats)
    }
    
    /**
     * Convert Android Bitmap to ByteArray in JPEG format for LFM2-VL
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream) // 85% quality like VLM example
        return outputStream.toByteArray()
    }
    
    /**
     * Build stage-specific prompt with enhanced context
     * Focuses on technical decomposition without gatekeeping
     */
    private fun buildStagePrompt(basePrompt: String, stage: Int): String {
        val stageContext = when (stage) {
            1 -> "TECHNICAL DECOMPOSITION - Break down this object into its engineering components. Every object has structural elements, materials, and assembly methods that affect performance.\n\n$basePrompt"
            2 -> "FAILURE MODE ANALYSIS - Identify where this object could fail under use. Look for stress risers, wear points, and critical joints. All objects have potential failure modes.\n\n$basePrompt"
            3 -> {
                val objectInfo = extractObjectInfoFromHistory()
                "SIMULATION OPPORTUNITIES - Generate specific engineering questions about the $objectInfo. Focus on what analysis would optimize performance, ensure safety, or reduce cost.\n\n$basePrompt"
            }
            else -> basePrompt
        }
        
        return stageContext
    }
    
    /**
     * Extract object name and key features from conversation history
     */
    private fun extractObjectInfoFromHistory(): String {
        if (analysisHistory.isEmpty()) return "component"
        
        // Look for object name and feature counts in previous responses
        val previousResponses = analysisHistory
            .filter { it.role == ChatMessage.Role.ASSISTANT }
            .joinToString(" ") { message ->
                (message.content.firstOrNull() as? ChatMessageContent.Text)?.text ?: ""
            }
            .lowercase()
        
        // Extract basic info for context (simplified extraction)
        val objectName = when {
            previousResponses.contains("chair") -> "chair"
            previousResponses.contains("bicycle") || previousResponses.contains("bike") -> "bicycle"
            previousResponses.contains("bracket") -> "bracket"
            previousResponses.contains("flange") -> "flange"
            else -> "component"
        }
        
        return objectName
    }
    
    /**
     * Reset analysis session for new component
     */
    fun resetAnalysis() {
        analysisHistory.clear()
        currentStage = 0
        Log.d(TAG, "Analysis session reset")
    }
    
    /**
     * Get analysis history for export/review
     */
    fun getAnalysisHistory(): List<ChatMessage> = analysisHistory.toList()
    
    /**
     * Check if the service is initialized and ready
     */
    fun isReady(): Boolean = isInitialized && modelRunner != null && conversation != null
    
    /**
     * Get current analysis stage
     */
    fun getCurrentStage(): Int = currentStage
    
    /**
     * Get conversation context summary for UI display
     */
    fun getContextSummary(): String {
        return if (analysisHistory.isNotEmpty()) {
            "Analysis in progress: Stage $currentStage (${analysisHistory.size / 2} stages completed)"
        } else {
            "Ready for new analysis"
        }
    }
    
}