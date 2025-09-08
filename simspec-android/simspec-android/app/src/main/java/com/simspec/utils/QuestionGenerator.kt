package com.simspec.utils

import android.util.Log

/**
 * Engineering question data class
 */
data class EngineeringQuestion(
    val questionText: String,
    val options: List<String>,
    val category: String = "General"
)

/**
 * QuestionGenerator - Generates contextual engineering questions
 * Based on the analysis type determined by ContextInterpreter
 */
object QuestionGenerator {
    
    private const val TAG = "QuestionGenerator"
    
    /**
     * Comprehensive question database mapped to analysis types
     * Questions are designed to gather critical engineering context
     */
    private val questionsDatabase = mapOf(
        
        "Fatigue Analysis, Stress Concentration" to listOf(
            EngineeringQuestion(
                "What is the primary loading condition for these fasteners?",
                listOf("Static Tension", "Cyclic (Vibration)", "Shear", "Combined Loading", "Unknown"),
                "Loading Conditions"
            ),
            EngineeringQuestion(
                "What is the typical operating environment?",
                listOf("Indoor/Controlled", "Outdoor/Weather", "Marine/Corrosive", "High Temperature", "Unknown"),
                "Environment"
            ),
            EngineeringQuestion(
                "How frequently is this component inspected?",
                listOf("Daily", "Weekly", "Monthly", "Annually", "Never/Unknown"),
                "Maintenance"
            )
        ),
        
        "Crack Propagation, Residual Stress Analysis" to listOf(
            EngineeringQuestion(
                "What welding process was likely used?",
                listOf("MIG/GMAW", "TIG/GTAW", "Stick/SMAW", "Submerged Arc", "Unknown"),
                "Welding Process"
            ),
            EngineeringQuestion(
                "Was post-weld heat treatment performed?",
                listOf("Yes, Stress Relief", "Yes, Full Anneal", "No Treatment", "Partial Treatment", "Unknown"),
                "Heat Treatment"
            ),
            EngineeringQuestion(
                "What is the base material type?",
                listOf("Carbon Steel", "Stainless Steel", "Aluminum", "Other Alloy", "Unknown"),
                "Materials"
            )
        ),
        
        "Remaining Life Assessment, Material Degradation Study" to listOf(
            EngineeringQuestion(
                "What is the operational environment?",
                listOf("Dry Indoor", "Humid Outdoor", "Marine/Salt-Spray", "Chemical Exposure", "High Temperature"),
                "Environment"
            ),
            EngineeringQuestion(
                "How long has this component been in service?",
                listOf("< 1 Year", "1-5 Years", "5-10 Years", "10+ Years", "Unknown"),
                "Service Life"
            ),
            EngineeringQuestion(
                "What protective coating was originally applied?",
                listOf("Paint System", "Galvanizing", "Stainless Cladding", "None", "Unknown"),
                "Protection"
            )
        ),
        
        "Fluid Dynamics, Pressure Drop Analysis" to listOf(
            EngineeringQuestion(
                "What is the typical operating pressure?",
                listOf("Low (< 50 psi)", "Medium (50-300 psi)", "High (300-1000 psi)", "Very High (> 1000 psi)", "Unknown"),
                "Operating Conditions"
            ),
            EngineeringQuestion(
                "What type of fluid is being handled?",
                listOf("Water", "Steam", "Oil/Hydrocarbon", "Chemical", "Gas", "Unknown"),
                "Fluid Type"
            ),
            EngineeringQuestion(
                "What is the operating temperature range?",
                listOf("Ambient (< 100°F)", "Medium (100-300°F)", "High (300-600°F)", "Very High (> 600°F)", "Unknown"),
                "Temperature"
            )
        ),
        
        "Structural Load Analysis, Deflection Study" to listOf(
            EngineeringQuestion(
                "What type of loading does this structure experience?",
                listOf("Static Dead Load", "Dynamic Live Load", "Wind/Seismic", "Thermal Expansion", "Unknown"),
                "Loading Type"
            ),
            EngineeringQuestion(
                "What is the structural material?",
                listOf("Steel", "Concrete", "Composite", "Aluminum", "Unknown"),
                "Materials"
            ),
            EngineeringQuestion(
                "Are there any visible deflection or movement concerns?",
                listOf("No Visible Issues", "Slight Movement", "Noticeable Deflection", "Concerning Movement", "Unknown"),
                "Deflection"
            )
        ),
        
        "Mechanical Component Analysis, Vibration Study" to listOf(
            EngineeringQuestion(
                "What is the typical operating RPM or frequency?",
                listOf("Low (< 1000 RPM)", "Medium (1000-3000 RPM)", "High (3000+ RPM)", "Variable Speed", "Unknown"),
                "Operating Speed"
            ),
            EngineeringQuestion(
                "Are there any unusual vibration or noise issues?",
                listOf("Normal Operation", "Slight Vibration", "Noticeable Noise", "Concerning Vibration", "Unknown"),
                "Vibration"
            ),
            EngineeringQuestion(
                "When was the last lubrication maintenance?",
                listOf("Recent (< 1 Month)", "Scheduled (1-6 Months)", "Overdue (> 6 Months)", "Continuous Lube System", "Unknown"),
                "Lubrication"
            )
        ),
        
        "Failure Analysis, Material Property Assessment" to listOf(
            EngineeringQuestion(
                "What type of failure mode is most concerning?",
                listOf("Fatigue Cracking", "Overload Failure", "Corrosion/Degradation", "Wear/Abrasion", "Unknown"),
                "Failure Mode"
            ),
            EngineeringQuestion(
                "Has this component failed before?",
                listOf("No Previous Failures", "Similar Failure History", "Different Failure Mode", "Recurring Issue", "Unknown"),
                "Failure History"
            ),
            EngineeringQuestion(
                "What is the criticality of this component?",
                listOf("Non-Critical", "Important", "Critical", "Safety-Critical", "Unknown"),
                "Criticality"
            )
        ),
        
        "General Structural Analysis" to listOf(
            EngineeringQuestion(
                "What is the primary function of this component?",
                listOf("Load Bearing", "Containment", "Connection", "Protection", "Unknown"),
                "Function"
            ),
            EngineeringQuestion(
                "What industry or application is this component used in?",
                listOf("Oil & Gas", "Manufacturing", "Construction", "Power Generation", "Other/Unknown"),
                "Industry"
            ),
            EngineeringQuestion(
                "What is the inspection priority for this component?",
                listOf("Low Priority", "Routine", "High Priority", "Immediate Attention", "Unknown"),
                "Inspection Priority"
            )
        )
    )
    
    /**
     * Generate appropriate engineering question based on analysis type and AI response
     * 
     * @param analysisType The analysis type from ContextInterpreter
     * @param aiResponse The original AI response to assess confidence
     * @return EngineeringQuestion or null if no match found
     */
    fun generateQuestion(analysisType: String, aiResponse: String = ""): EngineeringQuestion? {
        Log.d(TAG, "Generating question for analysis type: $analysisType")
        
        // Get confidence level from AI response
        val confidence = if (aiResponse.isNotBlank()) {
            ContextInterpreter.getConfidenceLevel(aiResponse)
        } else {
            "MEDIUM"
        }
        
        Log.d(TAG, "AI response confidence level: $confidence")
        
        // For low confidence, ask clarification questions first
        if (confidence == "LOW") {
            return EngineeringQuestion(
                "The component identification is unclear from the photos. What type of component is this?",
                listOf(
                    "Mechanical Machine Part",
                    "Structural Component", 
                    "Piping/Pressure System",
                    "Electrical Equipment",
                    "Consumer Product (Non-Engineering)",
                    "Unsure - Need Better Photo"
                ),
                "Component Identification"
            )
        }
        
        // Check if this looks like a non-engineering item that shouldn't have engineering analysis
        if (isNonEngineeringItem(analysisType, aiResponse)) {
            return EngineeringQuestion(
                "This appears to be a consumer/furniture item. Is engineering analysis needed?",
                listOf(
                    "No - This is furniture/consumer product",
                    "Yes - It's an engineered component", 
                    "Yes - It has structural engineering significance",
                    "Unsure - Need expert evaluation"
                ),
                "Engineering Relevance"
            )
        }
        
        // Find matching questions by checking if analysis type contains key components
        val matchingQuestions = questionsDatabase.entries.find { (key, _) ->
            val keyWords = key.split(",", " ").map { it.trim().lowercase() }
            val analysisWords = analysisType.lowercase()
            keyWords.any { keyword -> analysisWords.contains(keyword) }
        }?.value
        
        if (matchingQuestions.isNullOrEmpty()) {
            Log.w(TAG, "No questions found for analysis type: $analysisType")
            // Return a general question as fallback with uncertainty option
            return EngineeringQuestion(
                "What is the primary function of this component?",
                listOf(
                    "Load Bearing Structure", 
                    "Mechanical Component", 
                    "Pressure/Fluid System", 
                    "Electrical/Control System",
                    "Non-Critical/Decorative",
                    "Uncertain from Photos"
                ),
                "Function"
            )
        }
        
        // Modify the selected question based on confidence level
        val baseQuestion = matchingQuestions.first()
        val selectedQuestion = if (confidence == "MEDIUM") {
            // Add uncertainty option for medium confidence
            val uncertainOptions = baseQuestion.options.toMutableList()
            if (!uncertainOptions.any { it.contains("Uncertain") || it.contains("Unknown") }) {
                uncertainOptions.add("Uncertain from Photos")
            }
            baseQuestion.copy(options = uncertainOptions)
        } else {
            baseQuestion
        }
        
        Log.d(TAG, "Generated question: ${selectedQuestion.questionText}")
        
        return selectedQuestion
    }
    
    /**
     * Check if the AI response suggests this is a non-engineering item
     */
    private fun isNonEngineeringItem(analysisType: String, aiResponse: String): Boolean {
        val response = aiResponse.lowercase()
        val nonEngineeringKeywords = listOf(
            "chair", "furniture", "desk", "table", "shelf", "cabinet",
            "decoration", "ornament", "art", "toy", "consumer product",
            "household", "office furniture", "seating", "decorative"
        )
        
        return nonEngineeringKeywords.any { keyword -> response.contains(keyword) }
    }
    
    /**
     * Generate multiple questions for comprehensive analysis
     * 
     * @param analysisType The analysis type from ContextInterpreter
     * @param maxQuestions Maximum number of questions to return
     * @return List of EngineeringQuestion
     */
    fun generateMultipleQuestions(analysisType: String, maxQuestions: Int = 3): List<EngineeringQuestion> {
        Log.d(TAG, "Generating up to $maxQuestions questions for: $analysisType")
        
        val matchingQuestions = questionsDatabase.entries.find { (key, _) ->
            val keyWords = key.split(",", " ").map { it.trim().lowercase() }
            val analysisWords = analysisType.lowercase()
            keyWords.any { keyword -> analysisWords.contains(keyword) }
        }?.value
        
        return if (matchingQuestions.isNullOrEmpty()) {
            questionsDatabase["General Structural Analysis"]?.take(maxQuestions) ?: emptyList()
        } else {
            matchingQuestions.take(maxQuestions)
        }
    }
    
    /**
     * Get all available analysis types that have associated questions
     */
    fun getAvailableAnalysisTypes(): List<String> {
        return questionsDatabase.keys.toList()
    }
    
    /**
     * Get questions by category for a given analysis type
     */
    fun getQuestionsByCategory(analysisType: String): Map<String, List<EngineeringQuestion>> {
        val questions = questionsDatabase[analysisType] ?: return emptyMap()
        return questions.groupBy { it.category }
    }
}