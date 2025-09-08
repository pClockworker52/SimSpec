package com.simspec.utils

import android.util.Log

/**
 * ContextInterpreter - Maps AI analysis results to engineering analysis types
 * Based on keyword detection and engineering domain knowledge
 */
object ContextInterpreter {
    
    private const val TAG = "ContextInterpreter"
    
    /**
     * Analyze AI response and determine the most appropriate engineering analysis type
     * 
     * @param aiResponse The text response from the AI analysis
     * @return Engineering analysis type string
     */
    fun getAnalysisType(aiResponse: String): String {
        val lowercaseResponse = aiResponse.lowercase()
        
        Log.d(TAG, "Interpreting response: ${aiResponse.take(100)}...")
        
        val analysisType = when {
            // Fastener-related keywords
            containsAny(lowercaseResponse, listOf("fastener", "bolt", "screw", "nut", "hex")) ->
                "Fatigue Analysis, Stress Concentration"
            
            // Welding-related keywords  
            containsAny(lowercaseResponse, listOf("weld", "welded", "welding", "fusion", "joint")) ->
                "Crack Propagation, Residual Stress Analysis"
            
            // Corrosion-related keywords
            containsAny(lowercaseResponse, listOf("corrosion", "rust", "oxidation", "deterioration", "weathering")) ->
                "Remaining Life Assessment, Material Degradation Study"
            
            // Piping/fluid system keywords
            containsAny(lowercaseResponse, listOf("pipe", "flange", "gasket", "pressure", "vessel")) ->
                "Fluid Dynamics, Pressure Drop Analysis"
            
            // Structural keywords
            containsAny(lowercaseResponse, listOf("beam", "column", "truss", "frame", "support")) ->
                "Structural Load Analysis, Deflection Study"
            
            // Mechanical component keywords
            containsAny(lowercaseResponse, listOf("gear", "bearing", "shaft", "coupling", "motor")) ->
                "Mechanical Component Analysis, Vibration Study"
            
            // Material keywords
            containsAny(lowercaseResponse, listOf("crack", "fracture", "failure", "damage", "wear")) ->
                "Failure Analysis, Material Property Assessment"
            
            // Default case
            else -> "General Structural Analysis"
        }
        
        Log.d(TAG, "Determined analysis type: $analysisType")
        return analysisType
    }
    
    /**
     * Get severity assessment based on keywords in the AI response
     * 
     * @param aiResponse The AI analysis response
     * @return Severity level: LOW, MEDIUM, HIGH, CRITICAL
     */
    fun getSeverityAssessment(aiResponse: String): String {
        val lowercaseResponse = aiResponse.lowercase()
        
        return when {
            // Critical indicators
            containsAny(lowercaseResponse, listOf(
                "critical", "severe", "extensive", "significant damage", "structural failure",
                "immediate", "unsafe", "dangerous", "major crack"
            )) -> "CRITICAL"
            
            // High severity indicators
            containsAny(lowercaseResponse, listOf(
                "advanced", "progressive", "substantial", "concerning", "deteriorated",
                "widespread", "moderate damage", "inspection required"
            )) -> "HIGH"
            
            // Medium severity indicators  
            containsAny(lowercaseResponse, listOf(
                "minor", "slight", "limited", "localized", "surface", "light",
                "routine maintenance", "normal wear"
            )) -> "MEDIUM"
            
            // Low severity (good condition)
            containsAny(lowercaseResponse, listOf(
                "good", "excellent", "normal", "acceptable", "within spec",
                "no significant", "minimal", "functioning"
            )) -> "LOW"
            
            else -> "MEDIUM" // Default to medium if unclear
        }
    }
    
    /**
     * Extract maintenance recommendations from AI response
     * 
     * @param aiResponse The AI analysis response
     * @return List of maintenance recommendations
     */
    fun extractMaintenanceRecommendations(aiResponse: String): List<String> {
        val lowercaseResponse = aiResponse.lowercase()
        val recommendations = mutableListOf<String>()
        
        // Inspection recommendations
        if (containsAny(lowercaseResponse, listOf("inspect", "inspection", "check", "examine", "monitor"))) {
            recommendations.add("Schedule detailed inspection")
        }
        
        // Cleaning recommendations
        if (containsAny(lowercaseResponse, listOf("corrosion", "rust", "dirt", "debris", "clean"))) {
            recommendations.add("Clean and treat corroded areas")
        }
        
        // Torque/tightness recommendations
        if (containsAny(lowercaseResponse, listOf("bolt", "fastener", "loose", "torque", "tighten"))) {
            recommendations.add("Verify fastener torque specifications")
        }
        
        // Lubrication recommendations
        if (containsAny(lowercaseResponse, listOf("bearing", "gear", "shaft", "lubrication", "oil"))) {
            recommendations.add("Check lubrication levels and condition")
        }
        
        // Replacement recommendations
        if (containsAny(lowercaseResponse, listOf("replace", "replacement", "worn", "damaged", "failed"))) {
            recommendations.add("Consider component replacement")
        }
        
        // Gasket/seal recommendations
        if (containsAny(lowercaseResponse, listOf("gasket", "seal", "leak", "flange"))) {
            recommendations.add("Inspect gasket integrity and sealing")
        }
        
        return recommendations
    }
    
    /**
     * Helper function to check if any of the keywords exist in the text
     */
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
    
    /**
     * Get confidence level of the analysis based on specific terminology used
     */
    fun getConfidenceLevel(aiResponse: String): String {
        val lowercaseResponse = aiResponse.lowercase()
        
        return when {
            containsAny(lowercaseResponse, listOf("appears", "seems", "possibly", "might", "could", "likely")) ->
                "MEDIUM"
            
            containsAny(lowercaseResponse, listOf("unclear", "unable", "difficult", "uncertain", "unknown")) ->
                "LOW"
            
            containsAny(lowercaseResponse, listOf("clearly", "definitely", "precisely", "exactly", "confirmed")) ->
                "HIGH"
            
            else -> "MEDIUM"
        }
    }
}