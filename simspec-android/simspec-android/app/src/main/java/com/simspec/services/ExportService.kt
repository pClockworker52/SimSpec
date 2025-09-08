package com.simspec.services

import com.simspec.MainViewModel
import ai.liquid.leap.message.ChatMessage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * ExportService - Generates professional engineering simulation requests
 * Simplified version that preserves raw AI analysis for human interpretation
 */
object ExportService {
    
    /**
     * Generate a professional engineering simulation request from analysis results
     * This version presents the raw AI analysis clearly for FEA engineer review
     */
    fun generateSimulationRequest(
        analysisResults: List<MainViewModel.AnalysisResult>,
        conversationHistory: List<ChatMessage>
    ): String {
        if (analysisResults.isEmpty()) {
            return "No analysis results available for export."
        }
        
        // Get raw results from each stage
        val stage1 = analysisResults.find { it.stage == 1 }
        val stage2 = analysisResults.find { it.stage == 2 }
        val stage3 = analysisResults.find { it.stage == 3 }
        
        // Generate metadata
        val requestId = generateRequestId()
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val totalInferenceTime = analysisResults.sumOf { it.inferenceTime }
        
        // Extract basic component name from Stage 1 if possible
        val componentName = extractBasicComponentName(stage1?.text ?: "")
        
        // Assess analysis quality
        val qualityAssessment = assessAnalysisQuality(analysisResults)
        
        return buildString {
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("                    FEA SCOPING REQUEST")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Request ID:        $requestId")
            appendLine("Date Generated:    $currentDate")
            appendLine("Component:         $componentName")
            appendLine("Analysis Time:     ${totalInferenceTime}ms")
            appendLine("Analysis Quality:  ${qualityAssessment.summary}")
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("                    VISION AI ANALYSIS")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            
            // Stage 1: Component Overview
            appendLine("┌─────────────────────────────────────────────────────────────┐")
            appendLine("│ STAGE 1: COMPONENT IDENTIFICATION & GEOMETRY               │")
            appendLine("├─────────────────────────────────────────────────────────────┤")
            appendLine("│ Prompt: \"${getPromptForStage(1)}\"")
            appendLine("│ Processing Time: ${stage1?.inferenceTime ?: 0}ms")
            appendLine("└─────────────────────────────────────────────────────────────┘")
            appendLine()
            appendLine(formatAIResponse(stage1?.text))
            appendLine()
            
            // Stage 2: Connections
            appendLine("┌─────────────────────────────────────────────────────────────┐")
            appendLine("│ STAGE 2: CONNECTION & INTERFACE ANALYSIS                   │")
            appendLine("├─────────────────────────────────────────────────────────────┤")
            appendLine("│ Prompt: \"${getPromptForStage(2)}\"")
            appendLine("│ Processing Time: ${stage2?.inferenceTime ?: 0}ms")
            appendLine("└─────────────────────────────────────────────────────────────┘")
            appendLine()
            appendLine(formatAIResponse(stage2?.text))
            appendLine()
            
            // Stage 3: Stress Analysis
            appendLine("┌─────────────────────────────────────────────────────────────┐")
            appendLine("│ STAGE 3: STRESS CONCENTRATIONS & LOADING                   │")
            appendLine("├─────────────────────────────────────────────────────────────┤")
            appendLine("│ Prompt: \"${getPromptForStage(3)}\"")
            appendLine("│ Processing Time: ${stage3?.inferenceTime ?: 0}ms")
            appendLine("└─────────────────────────────────────────────────────────────┘")
            appendLine()
            appendLine(formatAIResponse(stage3?.text))
            appendLine()
            
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("                    FEA ENGINEER CHECKLIST")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Based on the AI analysis above, please verify:")
            appendLine()
            appendLine("□ Component type and primary function identified")
            appendLine("□ Main structural members and geometry understood")
            appendLine("□ Connection types (bolted/welded/pinned) clear")
            appendLine("□ Mounting points and constraints identifiable")
            appendLine("□ Stress concentration features noted")
            appendLine("□ Expected loading conditions determinable")
            appendLine()
            appendLine("Information gaps requiring clarification:")
            qualityAssessment.missingInfo.forEach { gap ->
                appendLine("  • $gap")
            }
            appendLine()
            
            // Quick reference section
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("                    QUICK REFERENCE")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Key Terms Identified:")
            val keyTerms = extractKeyTerms(analysisResults)
            keyTerms.forEach { term ->
                appendLine("  • $term")
            }
            appendLine()
            
            // Complexity estimate based on simple heuristics
            val complexity = estimateComplexity(analysisResults)
            appendLine("Preliminary Complexity Estimate: $complexity")
            appendLine()
            
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("Generated by SimSpec v1.0 | Model: LFM2-VL-1.6B")
            appendLine("Note: This is an AI-assisted preliminary analysis.")
            appendLine("Please review all findings before preparing quotation.")
            appendLine("═══════════════════════════════════════════════════════════════")
        }
    }
    
    /**
     * Format AI response for readability
     */
    private fun formatAIResponse(text: String?): String {
        if (text.isNullOrBlank()) {
            return "[No response received - stage may have timed out]"
        }
        
        if (text.contains("error", ignoreCase = true) || text.contains("cancelled", ignoreCase = true)) {
            return "[Analysis error: $text]"
        }
        
        // Check for truncation
        val isTruncated = !text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")
        
        // Wrap text nicely
        val wrapped = wrapText(text, 60)
        
        return if (isTruncated) {
            "$wrapped\n[Note: Response appears truncated]"
        } else {
            wrapped
        }
    }
    
    /**
     * Wrap text to specified width for better readability
     */
    private fun wrapText(text: String, width: Int): String {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        words.forEach { word ->
            if (currentLine.length + word.length + 1 > width) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines.joinToString("\n")
    }
    
    /**
     * Extract basic component name without complex parsing
     */
    private fun extractBasicComponentName(text: String): String {
        if (text.isBlank()) return "Unidentified Component"
        
        // Just take the first meaningful phrase
        val cleaned = text
            .replace(Regex("^(Stage \\d+:|This is|I see|It appears to be)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        
        // Take up to first comma or period
        val firstPhrase = cleaned.split(Regex("[,.]")).firstOrNull()?.trim() ?: cleaned
        
        return firstPhrase.take(50).ifEmpty { "Component" }
    }
    
    /**
     * Get the prompt used for a specific stage
     */
    private fun getPromptForStage(stage: Int): String {
        val prompts = listOf(
            "Describe this mechanical system. What are the main structural members and their approximate geometry?",
            "How are components joined together? Describe connection types: bolted, welded, pinned, or clamped. Where are the mounting points?",
            "What geometric features create stress concentrations? Sharp corners, holes, thickness changes? What loads would this experience in operation?"
        )
        return prompts.getOrNull(stage - 1)?.take(80) + "..." ?: "Unknown prompt"
    }
    
    /**
     * Simple quality assessment
     */
    private fun assessAnalysisQuality(results: List<MainViewModel.AnalysisResult>): QualityAssessment {
        val missingInfo = mutableListOf<String>()
        var successCount = 0
        
        results.forEach { result ->
            if (result.text.length > 50 && 
                !result.text.contains("error", ignoreCase = true) &&
                !result.text.contains("timeout", ignoreCase = true)) {
                successCount++
            } else {
                missingInfo.add("Stage ${result.stage} incomplete or failed")
            }
        }
        
        // Check for common missing elements in all text
        val allText = results.joinToString(" ") { it.text }
        
        if (!allText.contains(Regex("bolt|screw|weld|pin|rivet|glue|attach", RegexOption.IGNORE_CASE))) {
            missingInfo.add("Connection methods not clearly identified")
        }
        
        if (!allText.contains(Regex("\\d+", RegexOption.IGNORE_CASE))) {
            missingInfo.add("No quantitative information (counts, dimensions)")
        }
        
        if (!allText.contains(Regex("steel|aluminum|plastic|metal|composite|material", RegexOption.IGNORE_CASE))) {
            missingInfo.add("Material not specified or inferred")
        }
        
        val summary = when (successCount) {
            3 -> "Complete (${successCount}/3 stages)"
            2 -> "Partial (${successCount}/3 stages)"
            1 -> "Limited (${successCount}/3 stages)"
            else -> "Failed"
        }
        
        return QualityAssessment(summary, missingInfo)
    }
    
    /**
     * Extract key engineering terms mentioned
     */
    private fun extractKeyTerms(results: List<MainViewModel.AnalysisResult>): List<String> {
        val terms = mutableSetOf<String>()
        val allText = results.joinToString(" ") { it.text }.lowercase()
        
        // Engineering-relevant terms to look for
        val termPatterns = mapOf(
            "frame" to "Frame structure",
            "bracket" to "Brackets",
            "weld" to "Welded joints",
            "bolt" to "Bolted connections",
            "hole" to "Holes/openings",
            "corner" to "Corner features",
            "bend" to "Bends/curves",
            "support" to "Support structures",
            "mount" to "Mounting points",
            "bearing" to "Bearings",
            "shaft" to "Shafts",
            "gear" to "Gears",
            "spring" to "Springs",
            "cylinder" to "Cylinders"
        )
        
        termPatterns.forEach { (keyword, term) ->
            if (allText.contains(keyword)) {
                terms.add(term)
            }
        }
        
        return terms.take(6).ifEmpty { listOf("No specific terms identified") }
    }
    
    /**
     * Simple complexity estimation
     */
    private fun estimateComplexity(results: List<MainViewModel.AnalysisResult>): String {
        val allText = results.joinToString(" ") { it.text }
        
        // Count complexity indicators
        val connectionCount = Regex("bolt|weld|pin|rivet|screw|clamp", RegexOption.IGNORE_CASE).findAll(allText).count()
        val featureCount = Regex("hole|corner|bend|edge|groove|notch", RegexOption.IGNORE_CASE).findAll(allText).count()
        val loadCount = Regex("tension|compression|bend|twist|vibrat|force|load", RegexOption.IGNORE_CASE).findAll(allText).count()
        
        val totalScore = connectionCount + featureCount + loadCount
        
        return when {
            totalScore >= 10 -> "High - Complex geometry with multiple connections and loading modes"
            totalScore >= 5 -> "Medium - Standard component with several features"
            totalScore >= 2 -> "Low - Simple component with basic features"
            else -> "Basic - Minimal complexity identified"
        }
    }
    
    /**
     * Data classes for structured information
     */
    private data class QualityAssessment(
        val summary: String,
        val missingInfo: List<String>
    )
    
    /**
     * Generate unique request ID
     */
    private fun generateRequestId(): String {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val random = Random.nextInt(100, 999)
        return "FEA-$timestamp-$random"
    }
}