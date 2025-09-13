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

/**
 * Simple progress indicator for analysis stages - no timer complexity
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
    // Simple stage-based progress
    val progress = current.toFloat() / total.toFloat()
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
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
            // Progress text
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
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = when {
                        current >= total -> "Complete"
                        analysisStarted && current > 0 -> "Analyzing..."
                        current > 0 -> "Stage $current/$total"
                        else -> "Ready"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
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
            
            // Simple status text
            Text(
                text = when {
                    current == 0 -> "Ready to analyze"
                    current >= total -> "Analysis complete"
                    analysisStarted -> "AI inference in progress • Stage $current of $total"
                    else -> "Stage $current completed"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}