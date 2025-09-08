#!/usr/bin/env python3
"""
SimSpec Test Harness - Desktop Testing for Progressive Analysis System
Simulates the Android app workflow using extracted video frames
"""

import os
import time
import json
from pathlib import Path
from PIL import Image
import cv2

class MockLeapService:
    """
    Mock implementation of the LEAP SDK service for desktop testing
    In the real Android app, this would use the actual LEAP SDK
    """
    
    def __init__(self):
        self.initialized = False
        self.model_load_time = 0
    
    def initialize(self):
        """Simulate model loading"""
        print("🧠 Initializing LEAP SDK (Mock)...")
        start_time = time.time()
        
        # Simulate model loading time (2-5 seconds typical)
        time.sleep(2.5)
        
        self.model_load_time = time.time() - start_time
        self.initialized = True
        print(f"✅ LEAP SDK Initialized in {self.model_load_time:.2f}s")
    
    def analyze_image(self, image_path, prompt):
        """
        Mock AI analysis - returns simulated engineering analysis
        In the real app, this would call the actual LEAP SDK
        """
        if not self.initialized:
            return "Error: LEAP SDK not initialized", 0
        
        start_time = time.time()
        
        # Simulate inference time (1-3 seconds typical for mobile)
        time.sleep(1.5)
        
        inference_time = time.time() - start_time
        
        # Generate mock responses based on prompt type
        mock_response = self._generate_mock_response(prompt, image_path)
        
        return mock_response, inference_time * 1000  # Convert to milliseconds
    
    def _generate_mock_response(self, prompt, image_path):
        """Generate realistic mock responses for different prompt types"""
        responses = {
            "overall": "This image shows a large industrial pipe flange assembly with multiple bolt connections. The system appears to be part of a pressure vessel or piping network with metallic components.",
            
            "component": "The main component is a flanged pipe connection with approximately 8-12 bolts arranged in a circular pattern. The flange appears to be a raised-face type with gasket sealing surface.",
            
            "connections": "The connection points consist of high-strength bolts with hex nuts, likely Grade 8 or similar. The bolts appear to be in tension loading configuration with visible thread engagement.",
            
            "condition": "Surface shows signs of light corrosion and weathering typical of outdoor industrial environments. Some bolt heads show minor rust staining but no significant structural deterioration is visible.",
            
            "summary": "This flanged connection appears to be functioning within normal parameters. The slight surface corrosion suggests routine maintenance inspection is recommended, particularly for gasket integrity and bolt torque verification."
        }
        
        # Map prompt keywords to responses
        if "overall" in prompt.lower() or "system" in prompt.lower():
            return responses["overall"]
        elif "component" in prompt.lower() or "mechanical" in prompt.lower():
            return responses["component"] 
        elif "connection" in prompt.lower() or "bolts" in prompt.lower():
            return responses["connections"]
        elif "surface" in prompt.lower() or "condition" in prompt.lower():
            return responses["condition"]
        elif "summary" in prompt.lower() or "function" in prompt.lower():
            return responses["summary"]
        else:
            return "Unable to analyze this aspect of the component."

class ContextInterpreter:
    """Context interpretation logic from the Android plan"""
    
    @staticmethod
    def get_analysis_type(ai_response):
        lowercased_response = ai_response.lower()
        
        if any(word in lowercased_response for word in ["fastener", "bolt", "screw"]):
            return "Fatigue Analysis, Stress Concentration"
        elif "weld" in lowercased_response:
            return "Crack Propagation, Residual Stress Analysis"
        elif any(word in lowercased_response for word in ["corrosion", "rust"]):
            return "Remaining Life Assessment, Material Degradation Study"
        elif any(word in lowercased_response for word in ["pipe", "flange"]):
            return "Fluid Dynamics, Pressure Drop Analysis"
        else:
            return "General Structural Analysis"

class QuestionGenerator:
    """Engineering question generation logic from the Android plan"""
    
    questions_map = {
        "Fatigue Analysis, Stress Concentration": {
            "question": "What is the primary loading condition for these fasteners?",
            "options": ["Static Tension", "Cyclic (Vibration)", "Shear", "Unknown"]
        },
        "Crack Propagation, Residual Stress Analysis": {
            "question": "What welding process was likely used?", 
            "options": ["MIG/GMAW", "TIG/GTAW", "Stick/SMAW", "Unknown"]
        },
        "Remaining Life Assessment, Material Degradation Study": {
            "question": "What is the operational environment?",
            "options": ["Dry, Indoor", "Humid, Outdoor", "Marine/Salt-Spray", "Chemical Exposure"]
        },
        "Fluid Dynamics, Pressure Drop Analysis": {
            "question": "What is the typical operating pressure?",
            "options": ["Low (< 50 psi)", "Medium (50-500 psi)", "High (> 500 psi)", "Unknown"]
        }
    }
    
    @staticmethod
    def generate_question(analysis_type):
        # Find matching question by checking if analysis type contains key
        for key, question_data in QuestionGenerator.questions_map.items():
            if key.split(",")[0].strip() in analysis_type:
                return question_data
        return None

def run_progressive_analysis_test():
    """Main test function - simulates the full Android workflow"""
    
    print("SimSpec Progressive Analysis Test Harness")
    print("=" * 50)
    
    # Initialize mock LEAP service
    leap_service = MockLeapService()
    leap_service.initialize()
    
    # Progressive prompts from the Android plan
    prompts = [
        "Describe the overall system or machine in this image.",
        "Identify the main mechanical component in the center of the image.",
        "Focus on the connection points. Are there bolts, welds, or clamps?", 
        "Describe the surface condition. Is there evidence of wear, corrosion, or damage?",
        "Provide a summary of the component's likely function and condition."
    ]
    
    # Get extracted frames
    frames_dir = "extracted_frames"
    if not os.path.exists(frames_dir):
        print(f"Error: {frames_dir} directory not found. Run extract_frames.py first.")
        return
    
    frame_files = sorted([f for f in os.listdir(frames_dir) if f.endswith('.jpg')])
    if not frame_files:
        print(f"Error: No frame files found in {frames_dir}")
        return
    
    results = []
    
    # Test each frame with progressive analysis
    for frame_file in frame_files:
        frame_path = os.path.join(frames_dir, frame_file)
        timestamp = frame_file.split('_t')[1].split('s.jpg')[0] + 's'
        
        print(f"\n🎯 Analyzing Frame: {frame_file} (Timestamp: {timestamp})")
        print("-" * 40)
        
        frame_results = {
            'frame': frame_file,
            'timestamp': timestamp,
            'analyses': []
        }
        
        # Run all 5 progressive prompts on this frame
        for i, prompt in enumerate(prompts):
            print(f"\nStep {i+1}/5: {prompt}")
            
            response, inference_time = leap_service.analyze_image(frame_path, prompt)
            analysis_type = ContextInterpreter.get_analysis_type(response)
            question_data = QuestionGenerator.generate_question(analysis_type)
            
            analysis_result = {
                'step': i + 1,
                'prompt': prompt,
                'response': response,
                'inference_time_ms': inference_time,
                'analysis_type': analysis_type,
                'generated_question': question_data
            }
            
            frame_results['analyses'].append(analysis_result)
            
            print(f"Response: {response}")
            print(f"⏱️ Inference time: {inference_time:.0f}ms")
            print(f"🔍 Analysis type: {analysis_type}")
            if question_data:
                print(f"❓ Generated question: {question_data['question']}")
                print(f"   Options: {', '.join(question_data['options'])}")
        
        results.append(frame_results)
    
    # Save results to JSON for analysis
    results_file = "simspec_test_results.json"
    with open(results_file, 'w') as f:
        json.dump({
            'model_load_time_s': leap_service.model_load_time,
            'total_frames_analyzed': len(frame_files),
            'prompts_per_frame': len(prompts),
            'results': results
        }, f, indent=2)
    
    print(f"\n📊 Test Results Summary")
    print("=" * 30)
    print(f"Model load time: {leap_service.model_load_time:.2f}s")
    print(f"Frames analyzed: {len(frame_files)}")
    print(f"Total analyses: {len(frame_files) * len(prompts)}")
    print(f"Results saved to: {results_file}")
    
    return results

if __name__ == "__main__":
    run_progressive_analysis_test()