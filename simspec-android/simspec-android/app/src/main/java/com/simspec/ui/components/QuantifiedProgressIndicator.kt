package com.simspec.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Quantified progress indicator that shows time-based progress for analysis stages
 * Each stage is estimated at ~40 seconds, so total time is ~120 seconds
 */
@Composable
fun QuantifiedProgressIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    stageTimeSeconds: Int = 40,
    timerActive: Boolean = true,
    analysisStarted: Boolean = false
) {
    // Calculate time-based progress
    val totalTimeSeconds = total * stageTimeSeconds
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    
    // Timer effect - only run when analysis has actually started
    LaunchedEffect(current, timerActive, analysisStarted) {
        if (timerActive && analysisStarted && current >= 0 && current <= total) {
            while (elapsedSeconds < totalTimeSeconds && analysisStarted && timerActive) {
                delay(1000L)
                elapsedSeconds += 1
                
                // Cap elapsed time to not exceed total expected time
                if (elapsedSeconds >= totalTimeSeconds) {
                    elapsedSeconds = totalTimeSeconds
                    break
                }
            }
        } else if (!analysisStarted || !timerActive) {
            // Reset timer when analysis hasn't started or timer is inactive
            elapsedSeconds = 0
        }
    }
    
    // Calculate progress percentage
    val timeProgress = if (current == 0) {
        0f
    } else if (current >= total) {
        1f
    } else if (!timerActive) {
        // For photo capture mode, just show step-based progress
        current.toFloat() / total.toFloat()
    } else {
        // For analysis mode, base progress on stages completed + time in current stage
        val completedStages = current - 1
        val baseProgress = completedStages.toFloat() / total.toFloat()
        val currentStageProgress = minOf(elapsedSeconds % stageTimeSeconds, stageTimeSeconds).toFloat() / stageTimeSeconds.toFloat()
        val currentStageWeight = 1f / total.toFloat()
        baseProgress + (currentStageProgress * currentStageWeight)
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = timeProgress,
        animationSpec = tween(durationMillis = 300),
        label = "timeProgress"
    )
    
    val progressColor by animateColorAsState(
        targetValue = when {
            current == 0 -> MaterialTheme.colorScheme.outline
            current < total -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(durationMillis = 300),
        label = "progressColor"
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Progress text with time estimation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analysis Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (current > 0 && current <= total) {
                        val remainingTime = totalTimeSeconds - elapsedSeconds
                        "${formatTime(remainingTime)} remaining"
                    } else if (current >= total) {
                        "Complete"
                    } else {
                        "${formatTime(totalTimeSeconds)} estimated"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar with time-based fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                // Background
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
                
                // Progress
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (animatedProgress > 0f) {
                        drawLine(
                            color = progressColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width * animatedProgress, size.height / 2),
                            strokeWidth = size.height,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Status text with detailed timing
            Text(
                text = when {
                    current == 0 -> "Ready to analyze • ~${formatTime(totalTimeSeconds)} total"
                    current <= total -> {
                        val currentElapsed = if (current > 1) (current - 1) * stageTimeSeconds + (elapsedSeconds % stageTimeSeconds) else elapsedSeconds
                        "Stage $current • ${formatTime(currentElapsed)} elapsed"
                    }
                    else -> "Analysis complete • All stages finished"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Format seconds into MM:SS format
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}