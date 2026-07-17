package com.phonecheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.phonecheck.data.repository.PhoneCheckDatabase
import com.phonecheck.data.repository.TestSessionRepository
import com.phonecheck.domain.engine.PerformanceEngine
import com.phonecheck.domain.engine.TestEngine
import com.phonecheck.domain.score.ScoreEngine
import com.phonecheck.ui.screens.home.HomeScreen
import com.phonecheck.ui.theme.PhoneCheckTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var testEngine: TestEngine
    private lateinit var performanceEngine: PerformanceEngine
    private lateinit var scoreEngine: ScoreEngine
    private lateinit var sessionRepository: TestSessionRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize engines and repository
        testEngine = TestEngine(this)
        performanceEngine = PerformanceEngine(this)
        scoreEngine = ScoreEngine()
        
        val database = PhoneCheckDatabase.getDatabase(this)
        sessionRepository = TestSessionRepository(database.testSessionDao())
        
        setContent {
            PhoneCheckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        testEngine = testEngine,
                        scoreEngine = scoreEngine,
                        onRunCheckClicked = {
                            // Navigate to test running screen (placeholder for now)
                        },
                        onUsedPhoneModeClicked = {
                            // Navigate to used phone mode (placeholder for now)
                        },
                        onViewHistoryClicked = {
                            // Navigate to history (placeholder for now)
                        }
                    )
                }
            }
        }
    }
}
