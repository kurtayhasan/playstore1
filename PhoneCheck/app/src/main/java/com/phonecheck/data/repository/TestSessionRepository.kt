package com.phonecheck.data.repository

import android.content.Context
import androidx.room.*
import com.phonecheck.data.model.DeviceStatus
import com.phonecheck.data.model.TestResult
import com.phonecheck.data.model.TestSession
import kotlinx.coroutines.flow.Flow

/**
 * Entity for Room database - stores test session history
 */
@Entity(tableName = "test_sessions")
data class TestSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    
    @ColumnInfo(name = "overall_score")
    val overallScore: Int,
    
    @ColumnInfo(name = "device_status")
    val deviceStatus: String,
    
    @ColumnInfo(name = "passed_tests")
    val passedTests: Int,
    
    @ColumnInfo(name = "total_tests")
    val totalTests: Int,
    
    @ColumnInfo(name = "device_model")
    val deviceModel: String,
    
    @ColumnInfo(name = "android_version")
    val androidVersion: String,
    
    @ColumnInfo(name = "is_used_phone_mode")
    val isUsedPhoneMode: Boolean = false
)

/**
 * DAO for test sessions
 */
@Dao
interface TestSessionDao {
    @Query("SELECT * FROM test_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<TestSessionEntity>>
    
    @Query("SELECT * FROM test_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): TestSessionEntity?
    
    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TestSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TestSessionEntity): Long
    
    @Query("DELETE FROM test_sessions")
    suspend fun deleteAllSessions()
    
    @Query("SELECT COUNT(*) FROM test_sessions")
    suspend fun getSessionCount(): Int
}

/**
 * Room Database
 */
@Database(entities = [TestSessionEntity::class], version = 1)
abstract class PhoneCheckDatabase : RoomDatabase() {
    abstract fun testSessionDao(): TestSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: PhoneCheckDatabase? = null
        
        fun getDatabase(context: Context): PhoneCheckDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhoneCheckDatabase::class.java,
                    "phonecheck_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Repository for test sessions
 */
class TestSessionRepository(private val dao: TestSessionDao) {
    
    val allSessions: Flow<List<TestSessionEntity>> = dao.getAllSessions()
    
    suspend fun saveSession(session: TestSessionEntity): Long {
        return dao.insertSession(session)
    }
    
    suspend fun getLatestSession(): TestSessionEntity? {
        return dao.getLatestSession()
    }
    
    suspend fun getSessionCount(): Int {
        return dao.getSessionCount()
    }
    
    suspend fun deleteAllSessions() {
        dao.deleteAllSessions()
    }
}

/**
 * Convert TestSession to entity and back
 */
fun TestSession.toEntity(): TestSessionEntity {
    return TestSessionEntity(
        id = id,
        timestamp = timestamp,
        durationMs = durationMs,
        overallScore = overallScore,
        deviceStatus = deviceStatus.name,
        passedTests = passedTests,
        totalTests = totalTests,
        deviceModel = deviceModel,
        androidVersion = androidVersion,
        isUsedPhoneMode = isUsedPhoneMode
    )
}

fun TestSessionEntity.toDomain(): TestSession {
    return TestSession(
        id = id,
        timestamp = timestamp,
        durationMs = durationMs,
        overallScore = overallScore,
        deviceStatus = DeviceStatus.valueOf(deviceStatus),
        passedTests = passedTests,
        totalTests = totalTests,
        deviceModel = deviceModel,
        androidVersion = androidVersion,
        isUsedPhoneMode = isUsedPhoneMode
    )
}
