package com.phonecheck.domain.engine

import android.content.Context
import com.phonecheck.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TestEngine orchestrates all device tests.
 * 
 * It manages the test list, runs tests sequentially or in parallel where safe,
 * and aggregates results for the ScoreEngine.
 */
class TestEngine(
    private val context: Context,
    private val performanceEngine: PerformanceEngine
) {
    
    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults: StateFlow<List<TestResult>> = _testResults.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentTest = MutableStateFlow<String?>(null)
    val currentTest: StateFlow<String?> = _currentTest.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private var _performanceResult: PerformanceStabilityResult? = null
    val performanceResult: PerformanceStabilityResult? get() = _performanceResult
    
    // All available tests
    private val allTests: List<DeviceTest> by lazy {
        listOf(
            DisplayTest(context),
            TouchTest(context),
            SpeakerTest(context),
            MicrophoneTest(context),
            CameraTest(context),
            SensorsTest(context),
            VibrationTest(context),
            FlashlightTest(context),
            BluetoothTest(context),
            WifiTest(context),
            BatteryTest(context),
            StorageTest(context),
            SystemInfoTest(context)
        )
    }
    
    /**
     * Get all tests that are supported on this device.
     */
    fun getSupportedTests(): List<DeviceTest> {
        return allTests.filter { it.isSupported() }
    }
    
    /**
     * Run the full 5-minute health check.
     * Includes hardware tests + performance stability test.
     */
    suspend fun runFullCheck(): List<TestResult> {
        val hardwareResults = runTests(allTests)
        
        // Run performance stability test after hardware checks
        _currentTest.value = "Performance Stability"
        _performanceResult = performanceEngine.runStabilityTest()
        
        return hardwareResults
    }
    
    /**
     * Run only hardware-related tests (for quick checks).
     */
    suspend fun runHardwareCheck(): List<TestResult> {
        val hardwareTests = allTests.filter { 
            it.category != TestCategory.SYSTEM && it.category != TestCategory.STORAGE
        }
        return runTests(hardwareTests)
    }
    
    /**
     * Run a specific set of tests.
     */
    private suspend fun runTests(tests: List<DeviceTest>): List<TestResult> {
        _isRunning.value = true
        _testResults.value = emptyList()
        
        val results = mutableListOf<TestResult>()
        val totalTests = tests.size
        var completedTests = 0
        
        try {
            for (test in tests) {
                _currentTest.value = test.name
                
                val result = try {
                    test.run()
                } catch (e: Exception) {
                    TestResult(
                        testId = test.id,
                        status = TestResultStatus.NOT_VERIFIED,
                        confidence = ConfidenceLevel.NONE,
                        message = "Test failed",
                        explanation = e.message ?: "Unknown error",
                        verificationType = VerificationType.AUTOMATIC
                    )
                }
                
                results.add(result)
                _testResults.value = results.toList()
                
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Small delay between tests for UI updates
                kotlinx.coroutines.delay(100)
            }
        } finally {
            _isRunning.value = false
            _currentTest.value = null
        }
        
        return results
    }
    
    /**
     * Run a single test by ID.
     */
    suspend fun runSingleTest(testId: String): TestResult? {
        val test = allTests.find { it.id == testId } ?: return null
        
        if (!test.isSupported()) {
            return TestResult(
                testId = testId,
                status = TestResultStatus.UNSUPPORTED,
                confidence = ConfidenceLevel.HIGH,
                message = "Not supported",
                explanation = "This test is not available on your device",
                verificationType = VerificationType.NOT_AVAILABLE
            )
        }
        
        return try {
            test.run()
        } catch (e: Exception) {
            TestResult(
                testId = testId,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
    
    /**
     * Reset all test results.
     */
    fun reset() {
        _testResults.value = emptyList()
        _progress.value = 0f
        _currentTest.value = null
        _performanceResult = null
    }
    
    /**
     * Get the performance stability result after running full check.
     */
    fun getPerformanceResult(): PerformanceStabilityResult? = _performanceResult
    
    /**
     * Get estimated total duration for all tests.
     */
    fun getEstimatedDuration(): Long {
        return allTests.sumOf { it.estimatedDurationMs }
    }
}
