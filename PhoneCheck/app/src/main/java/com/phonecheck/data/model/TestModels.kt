package com.phonecheck.data.model

/**
 * Represents the result status of a diagnostic test
 */
enum class TestResultStatus {
    PASS,           // Test completed successfully
    ATTENTION,      // Test completed but found issues
    NOT_VERIFIED,   // Test could not be completed
    UNSUPPORTED,    // Feature not available on this device
    USER_CONFIRMED  // User manually confirmed the result
}

/**
 * Represents confidence level in test results
 */
enum class ConfidenceLevel {
    HIGH,       // Reliable API-based measurement
    MEDIUM,     // Indirect measurement or estimation
    LOW,        // Limited data available
    NONE        // No reliable data available
}

/**
 * Base interface for all diagnostic tests
 */
interface DeviceTest {
    val id: String
    val category: TestCategory
    val name: String
    val description: String
    
    /**
     * Check if this test can run on the current device
     */
    fun isSupported(): Boolean
    
    /**
     * Run the test and return result
     */
    suspend fun run(): TestResult
    
    /**
     * Whether this test requires user interaction
     */
    val requiresUserInteraction: Boolean get() = false
    
    /**
     * Estimated duration in milliseconds
     */
    val estimatedDurationMs: Long get() = 1000L
}

/**
 * Result of a diagnostic test
 */
data class TestResult(
    val testId: String,
    val status: TestResultStatus,
    val confidence: ConfidenceLevel,
    val message: String,
    val explanation: String,
    val details: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val verificationType: VerificationType = VerificationType.AUTOMATIC
)

enum class VerificationType {
    AUTOMATIC,
    USER_CONFIRMED,
    NOT_AVAILABLE
}

/**
 * Categories for organizing tests
 */
enum class TestCategory {
    DISPLAY,
    TOUCH,
    AUDIO,
    CAMERA,
    SENSORS,
    CONNECTIVITY,
    BATTERY,
    PERFORMANCE,
    STORAGE,
    SYSTEM
}

/**
 * Aggregated score components
 */
data class DeviceScore(
    val overall: Int,
    val hardwareCheck: Int,
    val performanceStability: Int,
    val batteryStatus: Int,
    val systemReadiness: Int,
    val breakdown: ScoreBreakdown
)

data class ScoreBreakdown(
    val passedTests: Int,
    val totalTests: Int,
    val attentionTests: Int,
    val unsupportedTests: Int,
    val performanceDegradation: Float,
    val batteryInfo: String
)

/**
 * Overall device status based on score
 */
enum class DeviceStatus {
    EXCELLENT,    // 90-100
    GOOD,         // 70-89
    ATTENTION,    // 40-69
    CHECK_NEEDED  // 0-39
}

/**
 * Test session record for history
 */
data class TestSession(
    val id: Long = 0,
    val timestamp: Long,
    val durationMs: Long,
    val overallScore: Int,
    val deviceStatus: DeviceStatus,
    val passedTests: Int,
    val totalTests: Int,
    val deviceModel: String,
    val androidVersion: String,
    val isUsedPhoneMode: Boolean = false,
    val testResults: List<TestResult> = emptyList()
)

/**
 * Performance stability test result
 */
data class PerformanceStabilityResult(
    val baselineScore: Int,
    val sustainedScore: Int,
    val degradationPercent: Float,
    val stabilityScore: Int,
    val thermalReading: String?,
    val thermalStatus: ThermalStatus,
    val explanation: String
)

enum class ThermalStatus {
    COOL,
    MODERATE,
    WARM,
    HOT,
    UNKNOWN
}
