package com.phonecheck.domain.engine

import android.os.Build
import android.os.PowerManager
import com.phonecheck.data.model.PerformanceStabilityResult
import com.phonecheck.data.model.ThermalStatus
import kotlinx.coroutines.delay
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

/**
 * Performance stability test engine.
 * 
 * This runs a controlled CPU workload to measure performance degradation
 * under sustained load. It does NOT claim to measure exact temperatures
 * or wattage - only what Android APIs reliably expose.
 */
class PerformanceEngine(private val context: android.content.Context) {
    
    companion object {
        private const val BASELINE_DURATION_MS = 2000L
        private const val SUSTAINED_LOAD_DURATION_MS = 30000L  // 30 seconds
        private const val POST_LOAD_DURATION_MS = 2000L
        private const val WORKLOAD_ITERATIONS = 50000
    }
    
    private val powerManager by lazy {
        context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
    }
    
    /**
     * Run the full performance stability test.
     * 
     * This measures:
     * 1. Baseline performance (short burst)
     * 2. Sustained load period (generates heat)
     * 3. Post-load performance (measures degradation)
     * 
     * Returns a PerformanceStabilityResult with scores and explanations.
     */
    suspend fun runStabilityTest(): PerformanceStabilityResult {
        return try {
            // Step 1: Measure baseline performance
            val baselineTime = measureBaselinePerformance()
            
            // Step 2: Apply sustained load
            applySustainedLoad()
            
            // Step 3: Measure post-load performance
            val postLoadTime = measurePostLoadPerformance()
            
            // Calculate degradation
            val degradationPercent = calculateDegradation(baselineTime, postLoadTime)
            
            // Get thermal reading if available
            val thermalReading = getThermalReading()
            val thermalStatus = determineThermalStatus(thermalReading)
            
            // Calculate stability score (0-100)
            val stabilityScore = calculateStabilityScore(degradationPercent, thermalStatus)
            
            // Generate explanation
            val explanation = generateExplanation(
                baselineTime,
                postLoadTime,
                degradationPercent,
                thermalStatus
            )
            
            PerformanceStabilityResult(
                baselineScore = normalizeToScore(baselineTime),
                sustainedScore = normalizeToScore(postLoadTime),
                degradationPercent = degradationPercent,
                stabilityScore = stabilityScore,
                thermalReading = thermalReading,
                thermalStatus = thermalStatus,
                explanation = explanation
            )
        } catch (e: Exception) {
            PerformanceStabilityResult(
                baselineScore = 50,
                sustainedScore = 50,
                degradationPercent = 0f,
                stabilityScore = 50,
                thermalReading = null,
                thermalStatus = ThermalStatus.UNKNOWN,
                explanation = "Performance test could not be completed: ${e.message ?: "Unknown error"}"
            )
        }
    }
    
    /**
     * Measure baseline performance with a short CPU workload.
     */
    private suspend fun measureBaselinePerformance(): Long {
        return runCpuWorkload(WORKLOAD_ITERATIONS / 5)
    }
    
    /**
     * Apply sustained CPU load to generate thermal pressure.
     */
    private suspend fun applySustainedLoad() {
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PhoneCheck::PerformanceTest"
        )
        
        try {
            wakeLock?.acquire(60_000)  // Max 60 seconds
            
            // Run continuous workloads for the sustained period
            val endTime = System.currentTimeMillis() + SUSTAINED_LOAD_DURATION_MS
            
            while (System.currentTimeMillis() < endTime) {
                runCpuWorkload(WORKLOAD_ITERATIONS)
                delay(10)  // Brief pause to prevent complete lockup
            }
        } finally {
            wakeLock?.release()
        }
    }
    
    /**
     * Measure post-load performance.
     */
    private suspend fun measurePostLoadPerformance(): Long {
        // Small delay to allow any immediate thermal throttling to take effect
        delay(100)
        return runCpuWorkload(WORKLOAD_ITERATIONS / 5)
    }
    
    /**
     * Run a CPU-intensive workload and return execution time.
     */
    private suspend fun runCpuWorkload(iterations: Int): Long {
        var result = 0L
        return measureTimeMillis {
            // Prime number calculation - CPU intensive but safe
            var count = 0
            var num = 2
            while (count < iterations) {
                var isPrime = true
                for (i in 2..Math.sqrt(num.toDouble()).toInt()) {
                    if (num % i == 0) {
                        isPrime = false
                        break
                    }
                }
                if (isPrime) {
                    result += num
                    count++
                }
                num++
            }
        }
    }
    
    /**
     * Calculate performance degradation percentage.
     */
    private fun calculateDegradation(baselineTime: Long, postLoadTime: Long): Float {
        if (baselineTime <= 0) return 0f
        
        val degradation = ((postLoadTime - baselineTime).toFloat() / baselineTime.toFloat()) * 100f
        return max(0f, min(100f, degradation))  // Clamp between 0-100%
    }
    
    /**
     * Get thermal reading from available Android APIs.
     * 
     * Note: Android does not provide reliable CPU temperature on most devices.
     * We use battery temperature as a proxy, which is what most OEMs expose.
     */
    private fun getThermalReading(): String? {
        return try {
            val intent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.intent.action.BATTERY_CHANGED)
            )
            
            val temperature = intent?.getIntExtra(
                android.os.BatteryManager.EXTRA_TEMPERATURE,
                -1
            )
            
            if (temperature > 0) {
                "${temperature / 10.0}°C"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Determine thermal status based on temperature reading.
     */
    private fun determineThermalStatus(thermalReading: String?): ThermalStatus {
        if (thermalReading == null) return ThermalStatus.UNKNOWN
        
        return try {
            val temp = thermalReading.replace("°C", "").toFloat()
            when {
                temp < 30 -> ThermalStatus.COOL
                temp < 40 -> ThermalStatus.MODERATE
                temp < 45 -> ThermalStatus.WARM
                temp < 50 -> ThermalStatus.HOT
                else -> ThermalStatus.HOT
            }
        } catch (e: Exception) {
            ThermalStatus.UNKNOWN
        }
    }
    
    /**
     * Calculate stability score (0-100).
     * 
     * Lower degradation = higher score.
     * Higher temperature = lower score.
     */
    private fun calculateStabilityScore(degradationPercent: Float, thermalStatus: ThermalStatus): Int {
        // Base score from degradation (70% weight)
        val degradationScore = (100 - degradationPercent).toInt().coerceIn(0, 100)
        
        // Thermal penalty (30% weight)
        val thermalPenalty = when (thermalStatus) {
            ThermalStatus.COOL -> 0
            ThermalStatus.MODERATE -> 5
            ThermalStatus.WARM -> 15
            ThermalStatus.HOT -> 30
            ThermalStatus.UNKNOWN -> 10
        }
        
        val baseScore = (degradationScore * 0.7).toInt()
        val thermalAdjusted = ((100 - thermalPenalty) * 0.3).toInt()
        
        return (baseScore + thermalAdjusted).coerceIn(0, 100)
    }
    
    /**
     * Normalize execution time to a rough performance score.
     */
    private fun normalizeToScore(timeMs: Long): Int {
        // Faster = higher score
        // Assume 100ms is excellent, 1000ms is poor
        return when {
            timeMs < 100 -> 95
            timeMs < 200 -> 85
            timeMs < 300 -> 75
            timeMs < 500 -> 65
            timeMs < 700 -> 55
            timeMs < 1000 -> 45
            else -> 35
        }
    }
    
    /**
     * Generate human-readable explanation of results.
     */
    private fun generateExplanation(
        baselineTime: Long,
        postLoadTime: Long,
        degradationPercent: Float,
        thermalStatus: ThermalStatus
    ): String {
        val degradationDesc = when {
            degradationPercent < 10 -> "minimal"
            degradationPercent < 20 -> "moderate"
            degradationPercent < 30 -> "noticeable"
            degradationPercent < 50 -> "significant"
            else -> "severe"
        }
        
        val thermalDesc = when (thermalStatus) {
            ThermalStatus.COOL -> "remained cool"
            ThermalStatus.MODERATE -> "warmed moderately"
            ThermalStatus.WARM -> "became warm"
            ThermalStatus.HOT -> "became hot"
            ThermalStatus.UNKNOWN -> "temperature unknown"
        }
        
        return buildString {
            append("Performance dropped ${degradationPercent.toInt()}% after sustained load. ")
            append("Device $thermalDesc during testing. ")
            
            when {
                degradationPercent < 15 -> append("Good thermal management.")
                degradationPercent < 30 -> append("Normal behavior under load.")
                degradationPercent < 50 -> append("Consider avoiding extended heavy tasks.")
                else -> append("Performance significantly affected by heat.")
            }
        }
    }
}
