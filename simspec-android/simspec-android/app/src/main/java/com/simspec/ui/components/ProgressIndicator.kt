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
 * Custom progress indicator for the 3-step analysis process
 */
@Composable
fun ProgressIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = current.toFloat() / total.toFloat(),
        animationSpec = tween(durationMillis = 500),
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
                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
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
                    if (progress > 0f) {
                        drawLine(
                            color = progressColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width * progress, size.height / 2),
                            strokeWidth = size.height,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Step indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(total) { index ->
                    val stepNumber = index + 1
                    val isCompleted = stepNumber <= current
                    val isCurrent = stepNumber == current + 1
                    
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when {
                            isCompleted -> progressColor
                            isCurrent -> progressColor.copy(alpha = 0.5f)
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted || isCurrent) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Status text
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    current == 0 -> "Ready to analyze"
                    current < total -> "Step $current completed"
                    else -> "Analysis complete"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}