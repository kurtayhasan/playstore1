package com.phonecheck.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phonecheck.data.model.DeviceScore
import com.phonecheck.data.model.DeviceStatus
import com.phonecheck.data.model.PerformanceStabilityResult
import com.phonecheck.data.model.TestResult
import com.phonecheck.data.model.TestSession
import com.phonecheck.data.repository.TestSessionRepository
import com.phonecheck.data.repository.toEntity
import com.phonecheck.domain.engine.TestEngine
import com.phonecheck.domain.score.ScoreEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestRunningScreen(
    currentTest: StateFlow<String?>,
    progress: StateFlow<Float>,
    testResults: StateFlow<List<TestResult>>,
    isRunning: StateFlow<Boolean>,
    modifier: Modifier = Modifier
) {
    val currentTestValue by currentTest.collectAsState()
    val progressValue by progress.collectAsState()
    val results by testResults.collectAsState()
    val running by isRunning.collectAsState()
    
    // Infinite rotation animation for the progress indicator
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Running Tests...") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Background circle
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp
                )
                
                // Progress circle
                CircularProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp
                )
                
                // Percentage text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(progressValue * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Current test name
            Text(
                text = currentTestValue ?: "Preparing...",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (running) "Running diagnostic tests..." else "Completing...",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Completed tests list
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tests Completed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (results.isEmpty()) {
                        Text(
                            text = "Starting tests...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        results.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = result.testId.replace("_", " ").capitalize(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                StatusBadge(status = result.status)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Please keep the screen on during testing. The full check takes about 5 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: com.phonecheck.data.model.TestResultStatus) {
    val (text, color) = when (status) {
        com.phonecheck.data.model.TestResultStatus.PASS -> "✓" to MaterialTheme.colorScheme.primary
        com.phonecheck.data.model.TestResultStatus.ATTENTION -> "!" to MaterialTheme.colorScheme.error
        com.phonecheck.data.model.TestResultStatus.NOT_VERIFIED -> "?" to MaterialTheme.colorScheme.outline
        com.phonecheck.data.model.TestResultStatus.UNSUPPORTED -> "-" to MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        com.phonecheck.data.model.TestResultStatus.USER_CONFIRMED -> "✓" to MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun String.capitalize(): String = 
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }


/**
 * Wrapper that handles the test execution logic and navigation callbacks
 */
@Composable
fun TestRunningScreenWrapper(
    testEngine: TestEngine,
    scoreEngine: ScoreEngine,
    sessionRepository: TestSessionRepository,
    isUsedPhoneMode: Boolean = false,
    onComplete: (DeviceScore, DeviceStatus, List<TestResult>, PerformanceStabilityResult?, Long) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var completed by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        startTime = System.currentTimeMillis()
        
        // Run the full check
        testEngine.runFullCheck()
        
        // Calculate score
        val performanceResult = testEngine.getPerformanceResult()
        val score = scoreEngine.calculateScore(testEngine.testResults.value, performanceResult)
        val status = scoreEngine.scoreToStatus(score.overall)
        
        val durationMs = System.currentTimeMillis() - startTime
        
        // Save session to Room
        val session = TestSession(
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            overallScore = score.overall,
            deviceStatus = status,
            passedTests = score.breakdown.passedTests,
            totalTests = score.breakdown.totalTests,
            deviceModel = android.os.Build.MODEL ?: "Unknown",
            androidVersion = android.os.Build.VERSION.RELEASE ?: "Unknown",
            isUsedPhoneMode = isUsedPhoneMode,
            testResults = testEngine.testResults.value
        )
        
        sessionRepository.saveSession(session.toEntity())
        
        completed = true
        onComplete(score, status, testEngine.testResults.value, performanceResult, durationMs)
    }
    
    TestRunningScreen(
        currentTest = testEngine.currentTest,
        progress = testEngine.progress,
        testResults = testEngine.testResults,
        isRunning = testEngine.isRunning
    )
}
