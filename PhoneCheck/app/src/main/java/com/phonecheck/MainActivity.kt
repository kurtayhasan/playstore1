package com.phonecheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phonecheck.data.repository.PhoneCheckDatabase
import com.phonecheck.data.repository.TestSessionRepository
import com.phonecheck.domain.engine.PerformanceEngine
import com.phonecheck.domain.engine.TestEngine
import com.phonecheck.domain.score.ScoreEngine
import com.phonecheck.ui.screens.home.HomeScreen
import com.phonecheck.ui.screens.result.ResultScreen
import com.phonecheck.ui.screens.test.TestRunningScreen
import com.phonecheck.ui.theme.PhoneCheckTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var testEngine: TestEngine
    private lateinit var performanceEngine: PerformanceEngine
    private lateinit var scoreEngine: ScoreEngine
    private lateinit var sessionRepository: TestSessionRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize engines and repository
        performanceEngine = PerformanceEngine(this)
        testEngine = TestEngine(this, performanceEngine)
        scoreEngine = ScoreEngine()
        
        val database = PhoneCheckDatabase.getDatabase(this)
        sessionRepository = TestSessionRepository(database.testSessionDao())
        
        setContent {
            PhoneCheckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onRunCheckClicked = {
                                    navController.navigate("running")
                                },
                                onUsedPhoneModeClicked = {
                                    navController.navigate("running?usedPhone=true")
                                },
                                onViewHistoryClicked = {
                                    navController.navigate("history")
                                }
                            )
                        }
                        
                        composable("running") { backStackEntry ->
                            val isUsedPhoneMode = backStackEntry.arguments?.getString("usedPhone") == "true"
                            TestRunningScreenWrapper(
                                testEngine = testEngine,
                                scoreEngine = scoreEngine,
                                sessionRepository = sessionRepository,
                                isUsedPhoneMode = isUsedPhoneMode,
                                onComplete = { score, status, testResults, performanceResult, durationMs ->
                                    // Navigate to result screen with data
                                    navController.navigate(
                                        "result/${score.overall}/${status.name}/$durationMs/$isUsedPhoneMode"
                                    )
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = "result/{score}/{status}/{durationMs}/{isUsedPhoneMode}",
                        ) { backStackEntry ->
                            val scoreValue = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 50
                            val statusName = backStackEntry.arguments?.getString("status") ?: "GOOD"
                            val durationMs = backStackEntry.arguments?.getString("durationMs")?.toLongOrNull() ?: 0L
                            val isUsedPhoneMode = backStackEntry.arguments?.getString("isUsedPhoneMode") == "true"
                            
                            val status = com.phonecheck.data.model.DeviceStatus.valueOf(statusName)
                            val score = testEngine.getPerformanceResult()?.let { perfResult ->
                                scoreEngine.calculateScore(testEngine.testResults.value, perfResult)
                            } ?: DeviceScore(
                                overall = scoreValue,
                                hardwareCheck = 50,
                                performanceStability = 50,
                                batteryStatus = 50,
                                systemReadiness = 50,
                                breakdown = ScoreBreakdown(0, 0, 0, 0, 0f, "")
                            )
                            
                            ResultScreen(
                                score = score,
                                status = status,
                                testResults = testEngine.testResults.value,
                                performanceDegradation = testEngine.getPerformanceResult()?.degradationPercent ?: 0f,
                                durationMs = durationMs,
                                isUsedPhoneMode = isUsedPhoneMode,
                                onBackClicked = {
                                    navController.popBackStack("home", inclusive = false)
                                },
                                onViewDetailsClicked = {
                                    // Show technical details (could navigate to another screen)
                                },
                                onShareClicked = {
                                    // Share result functionality
                                },
                                onRunAgainClicked = {
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }
                        
                        composable("history") {
                            HistoryScreen(
                                repository = sessionRepository,
                                onBackClicked = {
                                    navController.popBackStack()
                                },
                                onSessionClicked = { sessionId ->
                                    // Open session details
                                },
                                onDeleteAllClicked = {
                                    // Delete all history
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
