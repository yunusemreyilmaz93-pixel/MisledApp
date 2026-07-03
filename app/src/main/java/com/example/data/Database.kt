package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "vocabulary_arsenal")
data class DatabaseVocabularyItem(
    @PrimaryKey val word: String,
    val partOfSpeech: String,
    val meaning: String,
    val synonym: String,
    val example: String,
    val status: String = "Learning", // "Learning", "Mastered", "Difficult"
    val addedTimestamp: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis(),
    val interval: Int = 1, // in days
    val easeFactor: Float = 2.5f,
    val reviewCount: Int = 0,
    val wrongCount: Int = 0
)

@Entity(tableName = "passage_progress")
data class DatabasePassageProgress(
    @PrimaryKey val passageId: String,
    val isCompleted: Boolean = false,
    val score: Int = 0,
    val selectedAnswers: String = "", // Comma-separated answers (e.g. "A,B,C")
    val completionTimeSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class DatabaseUserStats(
    @PrimaryKey val id: Int = 1,
    val streak: Int = 0,
    val targetExam: String = "YDT", // "YDT", "YDS", "Other"
    val dailyGoalMinutes: Int = 20,
    val xp: Int = 0,
    val isPremium: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

// 2. DAOs
@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_arsenal ORDER BY addedTimestamp DESC")
    fun getAllVocabulary(): Flow<List<DatabaseVocabularyItem>>

    @Query("SELECT * FROM vocabulary_arsenal WHERE word = :word LIMIT 1")
    suspend fun getVocabularyItem(word: String): DatabaseVocabularyItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(item: DatabaseVocabularyItem)

    @Delete
    suspend fun deleteVocabulary(item: DatabaseVocabularyItem)

    @Query("UPDATE vocabulary_arsenal SET status = :status WHERE word = :word")
    suspend fun updateStatus(word: String, status: String)

    @Query("SELECT EXISTS(SELECT 1 FROM vocabulary_arsenal WHERE word = :word)")
    suspend fun isWordInArsenal(word: String): Boolean
}

@Dao
interface PassageProgressDao {
    @Query("SELECT * FROM passage_progress")
    fun getAllProgress(): Flow<List<DatabasePassageProgress>>

    @Query("SELECT * FROM passage_progress WHERE passageId = :passageId LIMIT 1")
    suspend fun getProgressForPassage(passageId: String): DatabasePassageProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DatabasePassageProgress)

    @Query("DELETE FROM passage_progress")
    suspend fun clearAllProgress()
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<DatabaseUserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): DatabaseUserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: DatabaseUserStats)
}

// 3. Database
@Database(
    entities = [DatabaseVocabularyItem::class, DatabasePassageProgress::class, DatabaseUserStats::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun passageProgressDao(): PassageProgressDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "misled_reading_lab_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
