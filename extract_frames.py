#!/usr/bin/env python3
"""
Frame extraction script for SimSpec video testing
Extracts frames from the reference video at specified intervals
"""

import cv2
import os
import time
from pathlib import Path

def extract_frames_from_video(video_path, output_dir, interval_seconds=3):
    """
    Extract frames from video at specified intervals
    
    Args:
        video_path: Path to the input video file
        output_dir: Directory to save extracted frames
        interval_seconds: Extract one frame every N seconds
    """
    # Create output directory
    Path(output_dir).mkdir(exist_ok=True)
    
    # Open video
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"Error: Could not open video {video_path}")
        return []
    
    # Get video properties
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = total_frames / fps
    
    print(f"Video info:")
    print(f"  FPS: {fps}")
    print(f"  Total frames: {total_frames}")
    print(f"  Duration: {duration:.2f} seconds")
    print(f"  Extracting every {interval_seconds} seconds")
    
    frame_interval = int(fps * interval_seconds)
    extracted_frames = []
    frame_count = 0
    extracted_count = 0
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        # Extract frame at intervals
        if frame_count % frame_interval == 0:
            timestamp = frame_count / fps
            filename = f"frame_{extracted_count:03d}_t{timestamp:.1f}s.jpg"
            filepath = os.path.join(output_dir, filename)
            
            # Resize frame to match Android processing (512x512 as per plan)
            resized_frame = cv2.resize(frame, (512, 512))
            
            cv2.imwrite(filepath, resized_frame)
            extracted_frames.append({
                'filepath': filepath,
                'timestamp': timestamp,
                'frame_number': frame_count
            })
            print(f"Extracted: {filename} (t={timestamp:.1f}s)")
            extracted_count += 1
            
        frame_count += 1
    
    cap.release()
    print(f"\nExtracted {extracted_count} frames to {output_dir}")
    return extracted_frames

if __name__ == "__main__":
    video_path = "videos/PXL_20250905_170538833.LS.mp4"
    output_dir = "extracted_frames"
    
    print("SimSpec Frame Extraction")
    print("=" * 40)
    
    if not os.path.exists(video_path):
        print(f"Error: Video file {video_path} not found")
        exit(1)
    
    # Extract frames every 3 seconds (matching Android throttling)
    frames = extract_frames_from_video(video_path, output_dir, interval_seconds=3)
    
    print(f"\nReady for AI analysis testing with {len(frames)} frames")