package com.phonecheck.domain.score

import com.phonecheck.data.model.*

/**
 * ScoreEngine calculates the transparent Device Check Score.
 * 
 * The score is composed of visible sub-scores:
 * - Hardware Check (tests passed)
 * - Performance Stability (from PerformanceEngine)
 * - Battery Status (battery health indicators)
 * - System Readiness (system info, storage)
 * 
 * All scoring is transparent and explainable.
 */
class ScoreEngine {
    
    companion object {
        // Weight distribution for overall score
        private const val WEIGHT_HARDWARE = 0.40f      // 40%
        private const val WEIGHT_PERFORMANCE = 0.30f   // 30%
        private const val WEIGHT_BATTERY = 0.15f       // 15%
        private const val WEIGHT_SYSTEM = 0.15f        // 15%
    }
    
    /**
     * Calculate the overall device score from test results.
     */
    fun calculateScore(
        testResults: List<TestResult>,
        performanceResult: PerformanceStabilityResult? = null
    ): DeviceScore {
        val hardwareScore = calculateHardwareScore(testResults)
        val performanceScore = performanceResult?.stabilityScore ?: 50
        val batteryScore = calculateBatteryScore(testResults)
        val systemScore = calculateSystemScore(testResults)
        
        // Calculate weighted overall score
        val overall = (
            hardwareScore * WEIGHT_HARDWARE +
            performanceScore * WEIGHT_PERFORMANCE +
            batteryScore * WEIGHT_BATTERY +
            systemScore * WEIGHT_SYSTEM
        ).toInt().coerceIn(0, 100)
        
        // Calculate breakdown statistics
        val passedTests = testResults.count { it.status == TestResultStatus.PASS }
        val attentionTests = testResults.count { it.status == TestResultStatus.ATTENTION }
        val unsupportedTests = testResults.count { it.status == TestResultStatus.UNSUPPORTED }
        val totalRelevantTests = testResults.filter { 
            it.status != TestResultStatus.UNSUPPORTED 
        }.size
        
        // Get battery info string
        val batteryResult = testResults.find { it.id == "battery" }
        val batteryInfo = batteryResult?.message ?: "Battery info unavailable"
        
        val breakdown = ScoreBreakdown(
            passedTests = passedTests,
            totalTests = totalRelevantTests,
            attentionTests = attentionTests,
            unsupportedTests = unsupportedTests,
            performanceDegradation = performanceResult?.degradationPercent ?: 0f,
            batteryInfo = batteryInfo
        )
        
        return DeviceScore(
            overall = overall,
            hardwareCheck = hardwareScore,
            performanceStability = performanceScore,
            batteryStatus = batteryScore,
            systemReadiness = systemScore,
            breakdown = breakdown
        )
    }
    
    /**
     * Calculate hardware check score based on test results.
     */
    private fun calculateHardwareScore(testResults: List<TestResult>): Int {
        val hardwareCategories = setOf(
            TestCategory.DISPLAY,
            TestCategory.TOUCH,
            TestCategory.AUDIO,
            TestCategory.CAMERA,
            TestCategory.SENSORS,
            TestCategory.CONNECTIVITY,
            TestCategory.SYSTEM
        )
        
        val hardwareTests = testResults.filter { it.category in hardwareCategories }
        
        if (hardwareTests.isEmpty()) return 50
        
        var totalPoints = 0
        var earnedPoints = 0
        
        for (test in hardwareTests) {
            val points = when (test.status) {
                TestResultStatus.PASS -> 10
                TestResultStatus.USER_CONFIRMED -> 8
                TestResultStatus.ATTENTION -> 4
                TestResultStatus.NOT_VERIFIED -> 2
                TestResultStatus.UNSUPPORTED -> 0  // Don't penalize for missing hardware
            }
            
            // Only count supported tests in total
            if (test.status != TestResultStatus.UNSUPPORTED) {
                totalPoints += 10
                earnedPoints += points
            }
        }
        
        return if (totalPoints > 0) {
            ((earnedPoints.toFloat() / totalPoints) * 100).toInt().coerceIn(0, 100)
        } else {
            50
        }
    }
    
    /**
     * Calculate battery status score.
     */
    private fun calculateBatteryScore(testResults: List<TestResult>): Int {
        val batteryResult = testResults.find { it.id == "battery" }
        
        return when {
            batteryResult == null -> 50  // No data
            batteryResult.status == TestResultStatus.PASS -> {
                // Extract battery level for fine-tuning
                val levelStr = batteryResult.details["level"] ?: "50"
                val level = levelStr.toIntOrNull() ?: 50
                
                when {
                    level >= 80 -> 95
                    level >= 60 -> 85
                    level >= 40 -> 70
                    level >= 20 -> 55
                    else -> 40
                }
            }
            batteryResult.status == TestResultStatus.ATTENTION -> 50
            batteryResult.status == TestResultStatus.NOT_VERIFIED -> 40
            else -> 50
        }
    }
    
    /**
     * Calculate system readiness score.
     */
    private fun calculateSystemScore(testResults: List<TestResult>): Int {
        val systemTests = testResults.filter { 
            it.category == TestCategory.SYSTEM || it.category == TestCategory.STORAGE 
        }
        
        if (systemTests.isEmpty()) return 50
        
        var totalPoints = 0
        var earnedPoints = 0
        
        for (test in systemTests) {
            val points = when (test.status) {
                TestResultStatus.PASS -> 10
                TestResultStatus.USER_CONFIRMED -> 8
                TestResultStatus.ATTENTION -> 5
                TestResultStatus.NOT_VERIFIED -> 3
                TestResultStatus.UNSUPPORTED -> 0
            }
            
            if (test.status != TestResultStatus.UNSUPPORTED) {
                totalPoints += 10
                earnedPoints += points
            }
        }
        
        return if (totalPoints > 0) {
            ((earnedPoints.toFloat() / totalPoints) * 100).toInt().coerceIn(0, 100)
        } else {
            50
        }
    }
    
    /**
     * Convert numeric score to human-readable status.
     */
    fun scoreToStatus(score: Int): DeviceStatus {
        return when {
            score >= 90 -> DeviceStatus.EXCELLENT
            score >= 70 -> DeviceStatus.GOOD
            score >= 40 -> DeviceStatus.ATTENTION
            else -> DeviceStatus.CHECK_NEEDED
        }
    }
    
    /**
     * Get status display string.
     */
    fun statusToString(status: DeviceStatus): String {
        return when (status) {
            DeviceStatus.EXCELLENT -> "Excellent"
            DeviceStatus.GOOD -> "Good"
            DeviceStatus.ATTENTION -> "Attention"
            DeviceStatus.CHECK_NEEDED -> "Check Needed"
        }
    }
    
    /**
     * Generate summary message for the user.
     */
    fun generateSummary(score: DeviceScore, status: DeviceStatus): String {
        return when (status) {
            DeviceStatus.EXCELLENT -> "Your device is performing excellently across all tested areas."
            DeviceStatus.GOOD -> "Your device is in good condition with no major issues detected."
            DeviceStatus.ATTENTION -> "Some aspects of your device need attention. Review the details below."
            DeviceStatus.CHECK_NEEDED -> "Several issues were detected. Consider reviewing your device condition."
        }
    }
}
