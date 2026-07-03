package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ReadingRepository(
    private val context: Context,
    private val vocabularyDao: VocabularyDao,
    private val passageProgressDao: PassageProgressDao,
    private val userStatsDao: UserStatsDao
) {
    // 1. Static seed passages loaded from JSON assets
    private val staticPassages: List<Passage> by lazy {
        loadPassagesFromAssets()
    }

    private fun loadPassagesFromAssets(): List<Passage> {
        return try {
            val jsonString = context.assets.open("passages.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, Passage::class.java)
            val adapter = moshi.adapter<List<Passage>>(listType)
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Combine static passages with Room progress database
    val passagesWithProgressFlow: Flow<List<PassageWithProgress>> = passageProgressDao.getAllProgress()
        .map { progressList ->
            val progressMap = progressList.associateBy { it.passageId }
            staticPassages.mapIndexed { index, passage ->
                val progress = progressMap[passage.id]
                
                // Determine if locked
                // P01-P03 are free. P04+ are premium.
                val isPremiumGated = index >= 3
                
                // Also, sequential progression: lock if previous passage is not completed
                val isPriorUncompleted = if (index > 0) {
                    val prevPassageId = staticPassages[index - 1].id
                    val prevProgress = progressMap[prevPassageId]
                    prevProgress?.isCompleted != true
                } else {
                    false
                }

                PassageWithProgress(
                    passage = passage,
                    isCompleted = progress?.isCompleted ?: false,
                    score = progress?.score ?: 0,
                    selectedAnswers = progress?.selectedAnswers?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                    isPremiumGated = isPremiumGated,
                    isPriorLocked = isPriorUncompleted,
                    completionTimeSeconds = progress?.completionTimeSeconds ?: 0
                )
            }
        }

    // 3. Vocab and Stats Flows
    val allVocabulary: Flow<List<DatabaseVocabularyItem>> = vocabularyDao.getAllVocabulary()
    
    val userStats: Flow<DatabaseUserStats> = userStatsDao.getUserStatsFlow()
        .map { it ?: DatabaseUserStats() } // Default values if null

    // 4. Analytics
    val analyticsFlow: Flow<ReadingAnalytics> = passagesWithProgressFlow.combine(allVocabulary) { passages, vocab ->
        val completed = passages.filter { it.isCompleted }
        val completedCount = completed.size
        
        // Calculate accuracy
        var totalQuestionsAnswered = 0
        var totalCorrect = 0
        val trapWrongCounts = mutableMapOf<String, Int>()

        completed.forEach { p ->
            val qCount = p.passage.questions.size
            totalQuestionsAnswered += qCount
            totalCorrect += p.score
            
            // Track mistakes by trap type
            val answers = p.selectedAnswers
            p.passage.questions.forEachIndexed { qIdx, question ->
                if (qIdx < answers.size) {
                    val studentAns = answers[qIdx]
                    if (studentAns != question.correctAnswer) {
                        trapWrongCounts[question.trapType] = (trapWrongCounts[question.trapType] ?: 0) + 1
                    }
                }
            }
        }

        val accuracy = if (totalQuestionsAnswered > 0) {
            (totalCorrect.toFloat() / totalQuestionsAnswered.toFloat() * 100).toInt()
        } else {
            0
        }

        // Find weakest trap type
        val weakestTrap = trapWrongCounts.maxByOrNull { it.value }?.key ?: "None Detected"

        ReadingAnalytics(
            completedPassagesCount = completedCount,
            accuracy = accuracy,
            weakestTrapType = weakestTrap,
            totalVocabularyAdded = vocab.size,
            masteredVocabularyCount = vocab.count { it.status == "Mastered" },
            trapMistakeDistribution = trapWrongCounts
        )
    }

    // 5. Database Actions
    suspend fun completePassage(passageId: String, score: Int, selectedAnswers: List<String>, timeSeconds: Int) {
        val progress = DatabasePassageProgress(
            passageId = passageId,
            isCompleted = true,
            score = score,
            selectedAnswers = selectedAnswers.joinToString(","),
            completionTimeSeconds = timeSeconds
        )
        passageProgressDao.insertProgress(progress)

        // Award XP and maintain streak
        val currentStats = userStatsDao.getUserStats() ?: DatabaseUserStats()
        val xpGain = 100 + (score * 50) // 100 for completion, 50 per correct answer
        
        val updatedStats = currentStats.copy(
            xp = currentStats.xp + xpGain,
            streak = if (System.currentTimeMillis() - currentStats.lastActiveTimestamp < 86400000 * 2) {
                currentStats.streak + 1
            } else {
                1
            },
            lastActiveTimestamp = System.currentTimeMillis()
        )
        userStatsDao.insertUserStats(updatedStats)
    }

    suspend fun addWordToArsenal(word: DatabaseVocabularyItem) {
        vocabularyDao.insertVocabulary(word)
    }

    suspend fun isWordInArsenal(word: String): Boolean {
        return vocabularyDao.isWordInArsenal(word)
    }

    suspend fun updateWordStatus(word: String, status: String) {
        vocabularyDao.updateStatus(word, status)
    }

    suspend fun deleteWordFromArsenal(item: DatabaseVocabularyItem) {
        vocabularyDao.deleteVocabulary(item)
    }

    suspend fun updatePremiumStatus(isPremium: Boolean) {
        val currentStats = userStatsDao.getUserStats() ?: DatabaseUserStats()
        userStatsDao.insertUserStats(currentStats.copy(isPremium = isPremium))
    }

    suspend fun updateTargetExam(exam: String) {
        val currentStats = userStatsDao.getUserStats() ?: DatabaseUserStats()
        userStatsDao.insertUserStats(currentStats.copy(targetExam = exam))
    }

    suspend fun updateDailyGoal(minutes: Int) {
        val currentStats = userStatsDao.getUserStats() ?: DatabaseUserStats()
        userStatsDao.insertUserStats(currentStats.copy(dailyGoalMinutes = minutes))
    }

    suspend fun initializeStatsIfEmpty() {
        val stats = userStatsDao.getUserStats()
        if (stats == null) {
            userStatsDao.insertUserStats(DatabaseUserStats(streak = 3, xp = 350)) // Seed some mock initial state
        }
    }
}

// Helper models for Repository output
data class PassageWithProgress(
    val passage: Passage,
    val isCompleted: Boolean,
    val score: Int,
    val selectedAnswers: List<String>,
    val isPremiumGated: Boolean,
    val isPriorLocked: Boolean,
    val completionTimeSeconds: Int
) {
    fun isLocked(userIsPremium: Boolean): Boolean {
        if (isPremiumGated && !userIsPremium) return true
        if (isPriorLocked) return true
        return false
    }
}

data class ReadingAnalytics(
    val completedPassagesCount: Int,
    val accuracy: Int,
    val weakestTrapType: String,
    val totalVocabularyAdded: Int,
    val masteredVocabularyCount: Int,
    val trapMistakeDistribution: Map<String, Int>
)
