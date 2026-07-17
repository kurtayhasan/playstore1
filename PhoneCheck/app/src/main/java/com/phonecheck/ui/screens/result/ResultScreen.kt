package com.phonecheck.ui.screens.result

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phonecheck.data.model.DeviceScore
import com.phonecheck.data.model.DeviceStatus
import com.phonecheck.data.model.TestResult
import com.phonecheck.ui.theme.getScoreColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    score: DeviceScore,
    status: DeviceStatus,
    testResults: List<TestResult>,
    performanceDegradation: Float,
    durationMs: Long,
    isUsedPhoneMode: Boolean = false,
    onBackClicked: () -> Unit,
    onViewDetailsClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onRunAgainClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scoreColor = getScoreColor(score.overall)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUsedPhoneMode) "Used Phone Check Result" else "Test Result") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Score header card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large score display
                    Text(
                        text = "${score.overall}",
                        style = MaterialTheme.typography.displayLarge,
                        color = scoreColor
                    )
                    
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Status badge
                    StatusChip(status = status)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = getStatusMessage(status),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sub-scores breakdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Score Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SubScoreRow(
                        label = "Hardware Check",
                        score = score.hardwareCheck,
                        icon = Icons.Default.Devices
                    )
                    
                    SubScoreRow(
                        label = "Performance Stability",
                        score = score.performanceStability,
                        icon = Icons.Default.Speed
                    )
                    
                    SubScoreRow(
                        label = "Battery Status",
                        score = score.batteryStatus,
                        icon = Icons.Default.BatteryFull
                    )
                    
                    SubScoreRow(
                        label = "System Readiness",
                        score = score.systemReadiness,
                        icon = Icons.Default.Info
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // What We Found section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "What We Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Key findings
                    testResults.take(5).forEach { result ->
                        FindingItem(result = result)
                    }
                    
                    if (performanceDegradation > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FindingItem(
                            icon = Icons.Default.TrendingDown,
                            title = "Performance Degradation",
                            message = "Lost ${performanceDegradation.toInt()}% performance under load",
                            status = if (performanceDegradation < 20) com.phonecheck.data.model.TestResultStatus.PASS else com.phonecheck.data.model.TestResultStatus.ATTENTION
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // What This Means section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "What This Means",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = getExplanationForStatus(status, performanceDegradation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRunAgainClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Again")
                }
                
                OutlinedButton(
                    onClick = onShareClicked,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test info footer
            Text(
                text = "Test duration: ${durationMs / 1000}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tap Details to see technical information for all tests.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusChip(status: DeviceStatus) {
    val (label, color) = when (status) {
        DeviceStatus.EXCELLENT -> "Excellent" to getScoreColor(90)
        DeviceStatus.GOOD -> "Good" to getScoreColor(75)
        DeviceStatus.ATTENTION -> "Attention" to getScoreColor(50)
        DeviceStatus.CHECK_NEEDED -> "Check Needed" to getScoreColor(30)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SubScoreRow(label: String, score: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = "$score",
            style = MaterialTheme.typography.titleMedium,
            color = getScoreColor(score)
        )
    }
}

@Composable
private fun FindingItem(result: TestResult) {
    FindingItem(
        icon = when {
            result.status == com.phonecheck.data.model.TestResultStatus.PASS -> Icons.Default.CheckCircle
            result.status == com.phonecheck.data.model.TestResultStatus.ATTENTION -> Icons.Default.Warning
            else -> Icons.Default.Info
        },
        title = result.testId.replace("_", " ").replaceFirstChar { it.uppercase() },
        message = result.message,
        status = result.status
    )
}

@Composable
private fun FindingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    status: com.phonecheck.data.model.TestResultStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when (status) {
                com.phonecheck.data.model.TestResultStatus.PASS -> MaterialTheme.colorScheme.primary
                com.phonecheck.data.model.TestResultStatus.ATTENTION -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun getStatusMessage(status: DeviceStatus): String {
    return when (status) {
        DeviceStatus.EXCELLENT -> "Your device is performing excellently"
        DeviceStatus.GOOD -> "Your device is in good condition"
        DeviceStatus.ATTENTION -> "Some aspects need attention"
        DeviceStatus.CHECK_NEEDED -> "Several issues detected"
    }
}

private fun getExplanationForStatus(status: DeviceStatus, degradation: Float): String {
    return buildString {
        append(when (status) {
            DeviceStatus.EXCELLENT -> "Your phone should handle daily tasks and demanding apps without issues."
            DeviceStatus.GOOD -> "Your phone is working well. Minor wear is normal."
            DeviceStatus.ATTENTION -> "Consider monitoring the flagged areas. Performance may vary under heavy use."
            DeviceStatus.CHECK_NEEDED -> "Review the detailed results. Some components may need attention."
        })
        
        if (degradation > 30) {
            append(" Your phone loses significant performance when warm - avoid extended gaming or heavy multitasking.")
        } else if (degradation > 15) {
            append(" Normal thermal behavior observed during testing.")
        }
    }
}
