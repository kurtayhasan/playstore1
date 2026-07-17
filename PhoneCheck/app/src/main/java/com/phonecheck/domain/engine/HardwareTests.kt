package com.phonecheck.domain.engine

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.phonecheck.data.model.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * Display test - checks basic display functionality
 */
class DisplayTest(private val context: Context) : DeviceTest {
    override val id = "display_basic"
    override val category = TestCategory.DISPLAY
    override val name = "Display Test"
    override val description = "Checks display rendering and basic integrity"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 2000L

    override fun isSupported(): Boolean = true

    override suspend fun run(): TestResult {
        return try {
            // Basic display capability check
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay
            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels
            val density = context.resources.displayMetrics.densityDpi
            
            if (width > 0 && height > 0) {
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Display is functioning normally",
                    explanation = "Display resolution: ${width}x${height}, Density: $density DPI",
                    details = mapOf(
                        "width" to width.toString(),
                        "height" to height.toString(),
                        "density" to density.toString()
                    ),
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.ATTENTION,
                    confidence = ConfidenceLevel.LOW,
                    message = "Could not verify display parameters",
                    explanation = "Display metrics returned unexpected values",
                    verificationType = VerificationType.AUTOMATIC
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Display test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Touch test - checks touchscreen responsiveness
 */
class TouchTest(private val context: Context) : DeviceTest {
    override val id = "touch_screen"
    override val category = TestCategory.TOUCH
    override val name = "Touch Screen Test"
    override val description = "Verifies touchscreen responsiveness"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1500L

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)

    override suspend fun run(): TestResult {
        return try {
            val hasTouchscreen = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_TOUCHSCREEN
            )
            val hasMultitouch = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH
            )
            
            if (hasTouchscreen) {
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Touchscreen is responsive",
                    explanation = if (hasMultitouch) "Multi-touch supported" else "Basic touch support",
                    details = mapOf(
                        "touchscreen" to hasTouchscreen.toString(),
                        "multitouch" to hasMultitouch.toString()
                    ),
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No touchscreen detected",
                    explanation = "This device does not have a touchscreen",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Touch test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Speaker test - checks audio output capability
 */
class SpeakerTest(private val context: Context) : DeviceTest {
    override val id = "speaker"
    override val category = TestCategory.AUDIO
    override val name = "Speaker Test"
    override val description = "Checks speaker functionality"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUDIO_OUTPUT)

    override suspend fun run(): TestResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val hasAudioOutput = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_AUDIO_OUTPUT
            )
            
            if (hasAudioOutput && audioManager != null) {
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.MEDIUM,
                    message = "Speaker is available",
                    explanation = "Audio output supported, Volume: $currentVolume/$maxVolume",
                    details = mapOf(
                        "max_volume" to maxVolume.toString(),
                        "current_volume" to currentVolume.toString()
                    ),
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.ATTENTION,
                    confidence = ConfidenceLevel.LOW,
                    message = "Speaker status unclear",
                    explanation = "Could not verify speaker functionality",
                    verificationType = VerificationType.AUTOMATIC
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Speaker test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Microphone test - checks audio input capability
 */
class MicrophoneTest(private val context: Context) : DeviceTest {
    override val id = "microphone"
    override val category = TestCategory.AUDIO
    override val name = "Microphone Test"
    override val description = "Checks microphone availability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_MICROPHONE)

    override suspend fun run(): TestResult {
        return try {
            val hasMic = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_MICROPHONE
            )
            
            if (hasMic) {
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Microphone is available",
                    explanation = "Audio input hardware detected",
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No microphone detected",
                    explanation = "This device does not have a microphone",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Microphone test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Camera test - checks camera availability
 */
class CameraTest(private val context: Context) : DeviceTest {
    override val id = "camera"
    override val category = TestCategory.CAMERA
    override val name = "Camera Test"
    override val description = "Checks camera availability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1500L

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)

    override suspend fun run(): TestResult {
        return try {
            val packageManager = context.packageManager
            val hasBackCamera = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA)
            val hasFrontCamera = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FRONT)
            
            if (hasBackCamera || hasFrontCamera) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
                
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Camera(s) available",
                    explanation = buildString {
                        if (hasBackCamera) append("Rear camera. ")
                        if (hasFrontCamera) append("Front camera. ")
                        append("Total cameras: ${cameraIds.size}")
                    },
                    details = mapOf(
                        "rear_camera" to hasBackCamera.toString(),
                        "front_camera" to hasFrontCamera.toString(),
                        "total_cameras" to cameraIds.size.toString()
                    ),
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No camera detected",
                    explanation = "This device does not have a camera",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Camera test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Sensors test - checks available sensors
 */
class SensorsTest(private val context: Context) : DeviceTest {
    override val id = "sensors"
    override val category = TestCategory.SENSORS
    override val name = "Sensors Test"
    override val description = "Checks accelerometer, gyroscope, proximity, and light sensors"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 2000L

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    override fun isSupported(): Boolean = sensorManager != null

    override suspend fun run(): TestResult {
        return try {
            val sm = sensorManager ?: run {
                return TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Sensor service unavailable",
                    explanation = "Could not access sensor manager",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
            
            val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            val light = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            
            val sensorList = listOf(
                "Accelerometer" to accelerometer,
                "Gyroscope" to gyroscope,
                "Proximity" to proximity,
                "Light" to light
            )
            
            val presentSensors = sensorList.filter { it.second != null }.map { it.first }
            val missingSensors = sensorList.filter { it.second == null }.map { it.first }
            
            val status = if (presentSensors.isNotEmpty()) {
                TestResultStatus.PASS
            } else {
                TestResultStatus.ATTENTION
            }
            
            TestResult(
                testId = id,
                status = status,
                confidence = ConfidenceLevel.HIGH,
                message = if (presentSensors.size >= 3) "All major sensors present" else "Some sensors available",
                explanation = "Present: ${presentSensors.joinToString()}. Missing: ${missingSensors.joinToString()}",
                details = mapOf(
                    "accelerometer" to (accelerometer != null).toString(),
                    "gyroscope" to (gyroscope != null).toString(),
                    "proximity" to (proximity != null).toString(),
                    "light" to (light != null).toString()
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Sensor test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Vibration test - checks vibrator capability
 */
class VibrationTest(private val context: Context) : DeviceTest {
    override val id = "vibration"
    override val category = TestCategory.SYSTEM
    override val name = "Vibration Test"
    override val description = "Checks vibration motor availability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun isSupported(): Boolean = vibrator?.hasVibrator() == true

    override suspend fun run(): TestResult {
        return try {
            val v = vibrator
            val hasVibrator = v?.hasVibrator() == true
            
            if (hasVibrator) {
                // Brief test vibration (10ms - barely noticeable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(10)
                }
                
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Vibration motor working",
                    explanation = "Vibrator hardware present and responsive",
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No vibration motor",
                    explanation = "This device does not have a vibration motor",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Vibration test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Flashlight test - checks camera flash/LED availability
 */
class FlashlightTest(private val context: Context) : DeviceTest {
    override val id = "flashlight"
    override val category = TestCategory.SYSTEM
    override val name = "Flashlight Test"
    override val description = "Checks flashlight/LED availability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
    }

    override fun isSupported(): Boolean {
        return try {
            val cm = cameraManager ?: return false
            val cameraId = cm.cameraIdList.firstOrNull { 
                cm.getCameraCharacteristics(it)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun run(): TestResult {
        return try {
            val cm = cameraManager
            val hasFlash = cm?.cameraIdList?.any { cameraId ->
                try {
                    cm.getCameraCharacteristics(cameraId)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (e: Exception) {
                    false
                }
            } == true
            
            if (hasFlash) {
                TestResult(
                    testId = id,
                    status = TestResultStatus.PASS,
                    confidence = ConfidenceLevel.HIGH,
                    message = "Flashlight available",
                    explanation = "Device has LED flash/torch capability",
                    verificationType = VerificationType.AUTOMATIC
                )
            } else {
                TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No flashlight detected",
                    explanation = "This device does not have an LED flash",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Flashlight test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Bluetooth test - checks Bluetooth capability
 */
class BluetoothTest(private val context: Context) : DeviceTest {
    override val id = "bluetooth"
    override val category = TestCategory.CONNECTIVITY
    override val name = "Bluetooth Test"
    override val description = "Checks Bluetooth capability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    }

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)

    override suspend fun run(): TestResult {
        return try {
            val hasBt = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_BLUETOOTH
            )
            
            if (!hasBt) {
                return TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No Bluetooth",
                    explanation = "This device does not have Bluetooth",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
            
            val bt = bluetoothAdapter
            val isEnabled = bt?.isEnabled == true
            val state = bt?.state ?: android.bluetooth.BluetoothAdapter.STATE_OFF
            
            TestResult(
                testId = id,
                status = TestResultStatus.PASS,
                confidence = ConfidenceLevel.HIGH,
                message = "Bluetooth available",
                explanation = "Bluetooth hardware present. ${if (isEnabled) "Enabled" else "Disabled"}",
                details = mapOf(
                    "enabled" to isEnabled.toString(),
                    "state" to state.toString()
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Bluetooth test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Wi-Fi test - checks Wi-Fi capability and connection
 */
class WifiTest(private val context: Context) : DeviceTest {
    override val id = "wifi"
    override val category = TestCategory.CONNECTIVITY
    override val name = "Wi-Fi Test"
    override val description = "Checks Wi-Fi capability and connection status"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    }

    override fun isSupported(): Boolean = 
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI)

    override suspend fun run(): TestResult {
        return try {
            val hasWifi = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_WIFI
            )
            
            if (!hasWifi) {
                return TestResult(
                    testId = id,
                    status = TestResultStatus.UNSUPPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    message = "No Wi-Fi",
                    explanation = "This device does not have Wi-Fi",
                    verificationType = VerificationType.NOT_AVAILABLE
                )
            }
            
            val wm = wifiManager
            val isWifiEnabled = wm?.isWifiEnabled == true
            val connectionInfo = wm?.connectionInfo
            val ssid = connectionInfo?.ssid ?: "Not connected"
            val rssi = connectionInfo?.rssi ?: 0
            
            TestResult(
                testId = id,
                status = if (isWifiEnabled) TestResultStatus.PASS else TestResultStatus.ATTENTION,
                confidence = ConfidenceLevel.HIGH,
                message = if (isWifiEnabled) "Wi-Fi available" else "Wi-Fi disabled",
                explanation = "Wi-Fi hardware present. ${if (isWifiEnabled) "Connected to: $ssid (RSSI: $rssi)" else "Turned off"}",
                details = mapOf(
                    "enabled" to isWifiEnabled.toString(),
                    "ssid" to ssid,
                    "rssi" to rssi.toString()
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Wi-Fi test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Battery test - checks battery status and information
 */
class BatteryTest(private val context: Context) : DeviceTest {
    override val id = "battery"
    override val category = TestCategory.BATTERY
    override val name = "Battery Test"
    override val description = "Checks battery status, level, and temperature"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 1000L

    override fun isSupported(): Boolean = true

    override suspend fun run(): TestResult {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.intent.action.BATTERY_CHANGED))
            
            if (intent == null || batteryManager == null) {
                return TestResult(
                    testId = id,
                    status = TestResultStatus.NOT_VERIFIED,
                    confidence = ConfidenceLevel.LOW,
                    message = "Battery info unavailable",
                    explanation = "Could not retrieve battery information",
                    verificationType = VerificationType.AUTOMATIC
                )
            }
            
            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
            
            val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == android.os.BatteryManager.BATTERY_STATUS_FULL
            
            val health = intent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)
            val voltage = intent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, -1)
            val temperature = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)
            
            val healthStatus = when (health) {
                android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
                android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
            
            val tempCelsius = if (temperature > 0) temperature / 10.0 else null
            
            TestResult(
                testId = id,
                status = if (batteryPct > 10 && health == android.os.BatteryManager.BATTERY_HEALTH_GOOD) 
                    TestResultStatus.PASS else TestResultStatus.ATTENTION,
                confidence = ConfidenceLevel.MEDIUM,
                message = "Battery: ${batteryPct}%, ${healthStatus}",
                explanation = buildString {
                    append("Level: ${batteryPct}%. ")
                    append("Status: ${if (isCharging) "Charging" else "Discharging"}. ")
                    append("Health: $healthStatus. ")
                    if (tempCelsius != null) append("Temp: ${tempCelsius}°C. ")
                    if (voltage > 0) append("Voltage: ${voltage}mV.")
                },
                details = mapOf(
                    "level" to batteryPct.toString(),
                    "charging" to isCharging.toString(),
                    "health" to healthStatus,
                    "temperature" to (tempCelsius?.toString() ?: "N/A"),
                    "voltage" to voltage.toString()
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Battery test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * Storage test - checks storage read/write capability
 */
class StorageTest(private val context: Context) : DeviceTest {
    override val id = "storage"
    override val category = TestCategory.STORAGE
    override val name = "Storage Test"
    override val description = "Checks internal storage read/write capability"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 2000L

    override fun isSupported(): Boolean = true

    override suspend fun run(): TestResult {
        return try {
            val internalDir = context.filesDir
            val totalSpace = internalDir.totalSpace
            val freeSpace = internalDir.freeSpace
            val usedSpace = totalSpace - freeSpace
            
            // Simple write/read test
            val testFile = java.io.File(internalDir, ".phonecheck_storage_test")
            val testData = "PhoneCheck storage test data ${System.currentTimeMillis()}"
            
            var writeSuccess = false
            var readSuccess = false
            var readData = ""
            
            try {
                testFile.writeText(testData)
                writeSuccess = true
                readData = testFile.readText()
                readSuccess = readData == testData
            } finally {
                testFile.delete()
            }
            
            val status = when {
                writeSuccess && readSuccess -> TestResultStatus.PASS
                writeSuccess -> TestResultStatus.ATTENTION
                else -> TestResultStatus.NOT_VERIFIED
            }
            
            val totalGB = totalSpace / (1024.0 * 1024.0 * 1024.0)
            val freeGB = freeSpace / (1024.0 * 1024.0 * 1024.0)
            
            TestResult(
                testId = id,
                status = status,
                confidence = ConfidenceLevel.HIGH,
                message = "Storage: ${freeGB.toString().take(4)}GB free of ${totalGB.toString().take(4)}GB",
                explanation = if (writeSuccess && readSuccess) {
                    "Read/write test passed successfully"
                } else if (writeSuccess) {
                    "Write succeeded but read verification failed"
                } else {
                    "Storage write test failed"
                },
                details = mapOf(
                    "total_space_gb" to totalGB.toString().take(6),
                    "free_space_gb" to freeGB.toString().take(6),
                    "used_space_percent" to ((usedSpace.toDouble() / totalSpace) * 100).toInt().toString(),
                    "write_test" to writeSuccess.toString(),
                    "read_test" to readSuccess.toString()
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "Storage test failed",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}

/**
 * System info test - collects basic system information
 */
class SystemInfoTest(private val context: Context) : DeviceTest {
    override val id = "system_info"
    override val category = TestCategory.SYSTEM
    override val name = "System Info"
    override val description = "Collects Android version and device model"
    override val requiresUserInteraction = false
    override val estimatedDurationMs = 500L

    override fun isSupported(): Boolean = true

    override suspend fun run(): TestResult {
        return try {
            val deviceModel = Build.MODEL ?: "Unknown"
            val manufacturer = Build.MANUFACTURER ?: "Unknown"
            val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
            val sdkLevel = Build.VERSION.SDK_INT
            val brand = Build.BRAND ?: "Unknown"
            
            TestResult(
                testId = id,
                status = TestResultStatus.PASS,
                confidence = ConfidenceLevel.HIGH,
                message = "$manufacturer $deviceModel",
                explanation = "Android $androidVersion (API $sdkLevel)",
                details = mapOf(
                    "manufacturer" to manufacturer,
                    "model" to deviceModel,
                    "brand" to brand,
                    "android_version" to androidVersion,
                    "sdk_level" to sdkLevel.toString(),
                    "product" to (Build.PRODUCT ?: "Unknown"),
                    "device" to (Build.DEVICE ?: "Unknown")
                ),
                verificationType = VerificationType.AUTOMATIC
            )
        } catch (e: Exception) {
            TestResult(
                testId = id,
                status = TestResultStatus.NOT_VERIFIED,
                confidence = ConfidenceLevel.NONE,
                message = "System info unavailable",
                explanation = e.message ?: "Unknown error",
                verificationType = VerificationType.AUTOMATIC
            )
        }
    }
}
