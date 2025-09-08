package com.simspec.services

import com.simspec.MainViewModel
import ai.liquid.leap.message.ChatMessage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * ExportService - Generates professional engineering simulation requests
 * Optimized for the new FEA-focused prompts
 */
object ExportService {
    
    /**
     * Generate a professional engineering simulation request from analysis results
     */
    fun generateSimulationRequest(
        analysisResults: List<MainViewModel.AnalysisResult>,
        conversationHistory: List<ChatMessage>
    ): String {
        if (analysisResults.isEmpty()) {
            return "No analysis results available for export."
        }
        
        // Extract analysis from each stage
        val stage1Result = analysisResults.find { it.step == 1 }
        val stage2Result = analysisResults.find { it.step == 2 }
        val stage3Result = analysisResults.find { it.step == 3 }
        
        // Parse key information from each stage
        val componentInfo = parseComponentDescription(stage1Result?.text ?: "")
        val connectionInfo = parseConnectionAnalysis(stage2Result?.text ?: "")
        val stressInfo = parseStressAnalysis(stage3Result?.text ?: "")
        
        // Generate metadata
        val requestId = generateRequestId()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val totalInferenceTime = analysisResults.sumOf { it.inferenceTime }
        
        return buildString {
            appendLine("SIMULATION SCOPING REPORT")
            appendLine("═══════════════════════════════════════════════")
            appendLine()
            appendLine("Request ID: $requestId")
            appendLine("Analysis Date: $currentDate")
            appendLine("Vision Processing Time: ${totalInferenceTime}ms")
            appendLine()
            
            // Section 1: Component Overview
            appendLine("1. COMPONENT IDENTIFICATION")
            appendLine("─────────────────────────────────")
            appendLine("Component Type: ${componentInfo.componentName}")
            appendLine("System Classification: ${componentInfo.systemType}")
            appendLine()
            appendLine("Structural Configuration:")
            appendLine(componentInfo.structuralDescription)
            appendLine()
            appendLine("Primary Structural Members:")
            componentInfo.structuralMembers.forEach { member ->
                appendLine("  • $member")
            }
            appendLine()
            appendLine("Geometric Complexity: ${componentInfo.geometricComplexity}")
            appendLine()
            
            // Section 2: Interface Analysis
            appendLine("2. CONNECTION & INTERFACE ANALYSIS")
            appendLine("─────────────────────────────────")
            appendLine("Connection Summary:")
            connectionInfo.connectionTypes.forEach { connection ->
                appendLine("  • $connection")
            }
            appendLine()
            if (connectionInfo.mountingPoints.isNotEmpty()) {
                appendLine("Mounting/Constraint Locations:")
                connectionInfo.mountingPoints.forEach { mount ->
                    appendLine("  • $mount")
                }
                appendLine()
            }
            appendLine("Assembly Complexity: ${connectionInfo.assemblyComplexity}")
            appendLine()
            
            // Section 3: FEA Requirements
            appendLine("3. FEA MODEL REQUIREMENTS")
            appendLine("─────────────────────────────────")
            appendLine("Critical Stress Concentration Features:")
            stressInfo.stressConcentrations.forEach { feature ->
                appendLine("  • $feature")
            }
            appendLine()
            appendLine("Expected Loading Conditions:")
            stressInfo.loadingConditions.forEach { load ->
                appendLine("  • $load")
            }
            appendLine()
            appendLine("High Stress Regions:")
            appendLine(stressInfo.criticalAreas)
            appendLine()
            
            // Section 4: Simulation Recommendations
            appendLine("4. SIMULATION RECOMMENDATIONS")
            appendLine("─────────────────────────────────")
            appendLine("Recommended Analysis Type: ${determineAnalysisType(stressInfo)}")
            appendLine()
            appendLine("Mesh Requirements:")
            appendLine("  • Global Element Size: ${determineMeshSize(componentInfo)}")
            appendLine("  • Refinement Zones: ${stressInfo.stressConcentrations.size} regions identified")
            appendLine("  • Contact Modeling: ${if (connectionInfo.hasContacts) "Required" else "Not required"}")
            appendLine()
            appendLine("Material Properties Required:")
            appendLine("  • ${determineMaterialRequirements(stressInfo)}")
            appendLine()
            
            // Section 5: Complexity Assessment
            appendLine("5. PROJECT COMPLEXITY ASSESSMENT")
            appendLine("─────────────────────────────────")
            val complexity = calculateComplexity(componentInfo, connectionInfo, stressInfo)
            appendLine("Overall Complexity: ${complexity.level}")
            appendLine("Estimated Setup Time: ${complexity.setupHours} hours")
            appendLine("Estimated Solve Time: ${complexity.solveHours} hours")
            appendLine("Recommended Engineer Level: ${complexity.engineerLevel}")
            appendLine()
            
            // Section 6: Data Quality
            appendLine("6. ANALYSIS CONFIDENCE")
            appendLine("─────────────────────────────────")
            val confidence = assessDataQuality(analysisResults)
            appendLine("Vision Analysis Quality: ${confidence.overallQuality}")
            appendLine("Information Completeness: ${confidence.completeness}%")
            if (confidence.missingInfo.isNotEmpty()) {
                appendLine("Additional Information Needed:")
                confidence.missingInfo.forEach { info ->
                    appendLine("  • $info")
                }
            }
            appendLine()
            
            appendLine("═══════════════════════════════════════════════")
            appendLine("Generated by SimSpec v1.0 | LFM2-VL-1.6B Vision Analysis")
        }
    }
    
    /**
     * Parse component description from Stage 1 (system overview)
     */
    private fun parseComponentDescription(text: String): ComponentInfo {
        if (text.isBlank() || text.contains("error", ignoreCase = true)) {
            return ComponentInfo(
                componentName = "Unidentified Component",
                systemType = "Analysis Pending",
                structuralDescription = "Component identification failed - manual review required",
                structuralMembers = listOf("Visual analysis incomplete"),
                geometricComplexity = "Unknown"
            )
        }
        
        // Extract component name (first meaningful noun phrase)
        val componentName = extractComponentName(text)
        
        // Identify structural members mentioned
        val members = mutableListOf<String>()
        val structuralKeywords = listOf(
            "frame", "beam", "tube", "plate", "bracket", "shaft", "housing",
            "support", "member", "strut", "bar", "column", "rail", "arm"
        )
        
        structuralKeywords.forEach { keyword ->
            if (text.contains(keyword, ignoreCase = true)) {
                // Try to extract context around the keyword
                val pattern = Regex("\\b(\\w+\\s+)?$keyword(\\s+\\w+)?", RegexOption.IGNORE_CASE)
                pattern.findAll(text).forEach { match ->
                    members.add(match.value.trim())
                }
            }
        }
        
        // Determine geometric complexity based on description
        val geometricComplexity = when {
            text.contains(Regex("complex|intricate|detailed", RegexOption.IGNORE_CASE)) -> "High - Multiple features"
            text.contains(Regex("simple|basic|standard", RegexOption.IGNORE_CASE)) -> "Low - Simple geometry"
            members.size > 5 -> "Medium-High - Multiple components"
            members.size > 2 -> "Medium - Several components"
            else -> "Low-Medium - Few components"
        }
        
        // Clean up the structural description
        val structuralDescription = text
            .split(". ")
            .filter { it.length > 10 }
            .take(2)
            .joinToString(". ")
            .ifEmpty { "See vision analysis for structural details" }
        
        return ComponentInfo(
            componentName = componentName,
            systemType = determineSystemType(text),
            structuralDescription = structuralDescription,
            structuralMembers = members.ifEmpty { listOf("Structural members identified in vision analysis") },
            geometricComplexity = geometricComplexity
        )
    }
    
    /**
     * Parse connection analysis from Stage 2
     */
    private fun parseConnectionAnalysis(text: String): ConnectionInfo {
        if (text.isBlank() || text.contains("error", ignoreCase = true)) {
            return ConnectionInfo(
                connectionTypes = listOf("Connection analysis incomplete"),
                mountingPoints = emptyList(),
                assemblyComplexity = "Unknown",
                hasContacts = false
            )
        }
        
        val connections = mutableListOf<String>()
        val mountingPoints = mutableListOf<String>()
        
        // Check for specific connection types
        val connectionPatterns = mapOf(
            "bolt" to "Bolted connections",
            "weld" to "Welded joints",
            "pin" to "Pinned connections",
            "clamp" to "Clamped interfaces",
            "rivet" to "Riveted joints",
            "adhesive" to "Adhesive bonds",
            "thread" to "Threaded connections",
            "press" to "Press-fit connections",
            "snap" to "Snap-fit connections"
        )
        
        connectionPatterns.forEach { (keyword, description) ->
            if (text.contains(keyword, ignoreCase = true)) {
                // Try to extract quantity if mentioned
                val quantityPattern = Regex("(\\d+)\\s*$keyword", RegexOption.IGNORE_CASE)
                val match = quantityPattern.find(text)
                if (match != null) {
                    connections.add("$description (${match.groupValues[1]} locations)")
                } else {
                    connections.add(description)
                }
            }
        }
        
        // Extract mounting point information
        val mountingKeywords = listOf("mount", "attach", "fix", "secure", "anchor", "support")
        mountingKeywords.forEach { keyword ->
            if (text.contains(keyword, ignoreCase = true)) {
                val pattern = Regex("$keyword\\w*\\s+(?:to|at|on)?\\s+([^.]+)", RegexOption.IGNORE_CASE)
                pattern.findAll(text).forEach { match ->
                    val location = match.groupValues[1].take(50).trim()
                    if (location.isNotBlank()) {
                        mountingPoints.add(location)
                    }
                }
            }
        }
        
        // Determine assembly complexity
        val assemblyComplexity = when {
            connections.size >= 4 -> "High - Multiple connection types"
            connections.size >= 2 -> "Medium - Mixed connections"
            connections.size == 1 -> "Low - Single connection type"
            else -> "Simple - Minimal connections"
        }
        
        val hasContacts = connections.size > 1 || text.contains("contact", ignoreCase = true)
        
        return ConnectionInfo(
            connectionTypes = connections.ifEmpty { listOf("Connection types per vision analysis") },
            mountingPoints = mountingPoints,
            assemblyComplexity = assemblyComplexity,
            hasContacts = hasContacts
        )
    }
    
    /**
     * Parse stress analysis from Stage 3
     */
    private fun parseStressAnalysis(text: String): StressAnalysisInfo {
        if (text.isBlank() || text.contains("error", ignoreCase = true)) {
            return StressAnalysisInfo(
                stressConcentrations = listOf("Stress analysis incomplete"),
                loadingConditions = listOf("Loading conditions to be determined"),
                criticalAreas = "Critical areas require manual identification",
                primaryLoadType = "Static"
            )
        }
        
        val stressConcentrations = mutableListOf<String>()
        val loadingConditions = mutableListOf<String>()
        
        // Extract stress concentration features
        val stressFeatures = mapOf(
            "hole" to "Holes/openings",
            "corner" to "Sharp corners",
            "notch" to "Notches",
            "groove" to "Grooves",
            "fillet" to "Small radius fillets",
            "thickness" to "Thickness changes",
            "transition" to "Geometric transitions",
            "edge" to "Sharp edges"
        )
        
        stressFeatures.forEach { (keyword, description) ->
            if (text.contains(keyword, ignoreCase = true)) {
                stressConcentrations.add(description)
            }
        }
        
        // Extract loading conditions
        val loadTypes = mapOf(
            "tension" to "Tensile loading",
            "compress" to "Compressive loading",
            "bend" to "Bending moments",
            "torsion" to "Torsional loading",
            "shear" to "Shear forces",
            "vibrat" to "Vibration/dynamic loading",
            "impact" to "Impact loading",
            "fatigue" to "Cyclic/fatigue loading",
            "thermal" to "Thermal loading"
        )
        
        loadTypes.forEach { (keyword, description) ->
            if (text.contains(keyword, ignoreCase = true)) {
                loadingConditions.add(description)
            }
        }
        
        // Extract critical areas description
        val criticalAreas = text
            .split(". ")
            .find { it.contains(Regex("high stress|critical|fail", RegexOption.IGNORE_CASE)) }
            ?.take(200)
            ?: "Stress concentrations at geometric discontinuities"
        
        // Determine primary load type for analysis recommendation
        val primaryLoadType = when {
            loadingConditions.any { it.contains("vibration", ignoreCase = true) } -> "Dynamic"
            loadingConditions.any { it.contains("fatigue", ignoreCase = true) } -> "Fatigue"
            loadingConditions.any { it.contains("thermal", ignoreCase = true) } -> "Thermal-Structural"
            loadingConditions.size > 2 -> "Combined Loading"
            else -> "Static Structural"
        }
        
        return StressAnalysisInfo(
            stressConcentrations = stressConcentrations.ifEmpty { 
                listOf("Geometric discontinuities identified in vision analysis") 
            },
            loadingConditions = loadingConditions.ifEmpty { 
                listOf("Operational loading to be specified") 
            },
            criticalAreas = criticalAreas,
            primaryLoadType = primaryLoadType
        )
    }
    
    // Data classes for structured information
    private data class ComponentInfo(
        val componentName: String,
        val systemType: String,
        val structuralDescription: String,
        val structuralMembers: List<String>,
        val geometricComplexity: String
    )
    
    private data class ConnectionInfo(
        val connectionTypes: List<String>,
        val mountingPoints: List<String>,
        val assemblyComplexity: String,
        val hasContacts: Boolean
    )
    
    private data class StressAnalysisInfo(
        val stressConcentrations: List<String>,
        val loadingConditions: List<String>,
        val criticalAreas: String,
        val primaryLoadType: String
    )
    
    private data class ComplexityAssessment(
        val level: String,
        val setupHours: String,
        val solveHours: String,
        val engineerLevel: String
    )
    
    private data class DataQualityAssessment(
        val overallQuality: String,
        val completeness: Int,
        val missingInfo: List<String>
    )
    
    // Helper methods
    private fun extractComponentName(text: String): String {
        // Remove common AI response patterns
        var cleaned = text
            .replace(Regex("^(This is|I see|It appears to be|This appears to be)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(a|an|the)\\s+", RegexOption.IGNORE_CASE), "")
        
        // Take first noun phrase or sentence fragment
        val firstPart = cleaned.split(Regex("[,.]")).firstOrNull()?.trim() ?: cleaned
        
        // Limit length and capitalize
        return firstPart
            .take(50)
            .trim()
            .replaceFirstChar { it.titlecase() }
            .ifEmpty { "Mechanical Component" }
    }
    
    private fun determineSystemType(text: String): String {
        return when {
            text.contains(Regex("vehicle|automotive|car", RegexOption.IGNORE_CASE)) -> "Automotive System"
            text.contains(Regex("aerospace|aircraft|flight", RegexOption.IGNORE_CASE)) -> "Aerospace System"
            text.contains(Regex("machine|industrial|manufacturing", RegexOption.IGNORE_CASE)) -> "Industrial Machinery"
            text.contains(Regex("consumer|product|device", RegexOption.IGNORE_CASE)) -> "Consumer Product"
            text.contains(Regex("structural|building|construction", RegexOption.IGNORE_CASE)) -> "Structural System"
            else -> "Mechanical System"
        }
    }
    
    private fun determineAnalysisType(stressInfo: StressAnalysisInfo): String {
        return stressInfo.primaryLoadType + " Analysis"
    }
    
    private fun determineMeshSize(componentInfo: ComponentInfo): String {
        return when {
            componentInfo.geometricComplexity.contains("High") -> "Fine (2-5mm typical)"
            componentInfo.geometricComplexity.contains("Low") -> "Coarse (10-20mm typical)"
            else -> "Medium (5-10mm typical)"
        }
    }
    
    private fun determineMaterialRequirements(stressInfo: StressAnalysisInfo): String {
        return when {
            stressInfo.primaryLoadType.contains("Fatigue") -> "Full S-N curve data required"
            stressInfo.primaryLoadType.contains("Dynamic") -> "Damping coefficients and dynamic moduli"
            stressInfo.primaryLoadType.contains("Thermal") -> "Thermal expansion and conductivity"
            else -> "Young's modulus, Poisson's ratio, yield strength"
        }
    }
    
    private fun calculateComplexity(
        componentInfo: ComponentInfo,
        connectionInfo: ConnectionInfo,
        stressInfo: StressAnalysisInfo
    ): ComplexityAssessment {
        val complexityScore = listOf(
            if (componentInfo.geometricComplexity.contains("High")) 3 else if (componentInfo.geometricComplexity.contains("Low")) 1 else 2,
            if (connectionInfo.assemblyComplexity.contains("High")) 3 else if (connectionInfo.assemblyComplexity.contains("Low")) 1 else 2,
            if (stressInfo.loadingConditions.size > 3) 3 else if (stressInfo.loadingConditions.size > 1) 2 else 1
        ).average()
        
        return when {
            complexityScore >= 2.5 -> ComplexityAssessment(
                level = "High Complexity",
                setupHours = "16-24",
                solveHours = "4-8",
                engineerLevel = "Senior FEA Specialist"
            )
            complexityScore >= 1.5 -> ComplexityAssessment(
                level = "Medium Complexity",
                setupHours = "8-16",
                solveHours = "2-4",
                engineerLevel = "FEA Engineer"
            )
            else -> ComplexityAssessment(
                level = "Low Complexity",
                setupHours = "4-8",
                solveHours = "1-2",
                engineerLevel = "Junior FEA Engineer"
            )
        }
    }
    
    private fun assessDataQuality(results: List<MainViewModel.AnalysisResult>): DataQualityAssessment {
        val missingInfo = mutableListOf<String>()
        var completeness = 100
        
        results.forEach { result ->
            if (result.text.contains("error", ignoreCase = true) || 
                result.text.contains("timeout", ignoreCase = true) ||
                result.text.length < 20) {
                missingInfo.add("Stage ${result.step} analysis incomplete")
                completeness -= 33
            }
        }
        
        // Check for specific missing information
        val allText = results.joinToString(" ") { it.text }
        if (!allText.contains(Regex("bolt|weld|pin|clamp|rivet", RegexOption.IGNORE_CASE))) {
            missingInfo.add("Connection types not clearly identified")
            completeness -= 10
        }
        if (!allText.contains(Regex("steel|aluminum|plastic|composite", RegexOption.IGNORE_CASE))) {
            missingInfo.add("Material specification needed")
        }
        if (!allText.contains(Regex("\\d+\\s*(mm|cm|inch|meter)", RegexOption.IGNORE_CASE))) {
            missingInfo.add("Dimensional information needed")
        }
        
        val quality = when {
            completeness >= 90 -> "Excellent"
            completeness >= 70 -> "Good"
            completeness >= 50 -> "Adequate"
            else -> "Limited"
        }
        
        return DataQualityAssessment(
            overallQuality = quality,
            completeness = completeness.coerceAtLeast(0),
            missingInfo = missingInfo
        )
    }
    
    /**
     * Generate unique request ID
     */
    private fun generateRequestId(): String {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val random = Random.nextInt(1000, 9999).toString(16).uppercase()
        return "FEA-$timestamp-$random"
    }
}