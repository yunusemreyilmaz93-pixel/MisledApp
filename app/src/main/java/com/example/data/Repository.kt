package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Content Data Source abstraction to prepare for future Firebase/Firestore integration
interface ContentDataSource {
    suspend fun getUnit(unitId: String): com.example.data.Unit?
    suspend fun getTrapIndex(): List<TrapType>
    suspend fun getVocabularyArsenalUnit1(): List<VocabularyItem>
}

// Local asset-based content data source (current default)
class AssetContentDataSource(private val context: Context) : ContentDataSource {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override suspend fun getUnit(unitId: String): com.example.data.Unit? {
        return try {
            val filename = when (unitId) {
                "unit_1_health_medicine" -> "content/unit_1_health_medicine.json"
                else -> "content/unit_1_health_medicine.json"
            }
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val adapter = moshi.adapter(com.example.data.Unit::class.java)
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getTrapIndex(): List<TrapType> {
        return try {
            val jsonString = context.assets.open("content/trap_index.json").bufferedReader().use { it.readText() }
            val listType = Types.newParameterizedType(List::class.java, TrapType::class.java)
            val adapter = moshi.adapter<List<TrapType>>(listType)
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getVocabularyArsenalUnit1(): List<VocabularyItem> {
        return try {
            val jsonString = context.assets.open("content/vocabulary_arsenal_unit_1.json").bufferedReader().use { it.readText() }
            val listType = Types.newParameterizedType(List::class.java, VocabularyItem::class.java)
            val adapter = moshi.adapter<List<VocabularyItem>>(listType)
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

class ReadingRepository(
    private val context: Context,
    private val vocabularyDao: VocabularyDao,
    private val passageProgressDao: PassageProgressDao,
    private val userStatsDao: UserStatsDao,
    private val contentDataSource: ContentDataSource = AssetContentDataSource(context)
) {
    // Load full unit model lazily from data source
    val fullUnit: com.example.data.Unit? by lazy {
        try {
            runBlocking {
                contentDataSource.getUnit("unit_1_health_medicine")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 1. Static seed passages loaded from assets
    private val staticPassages: List<Passage> by lazy {
        fullUnit?.passages ?: emptyList()
    }

    fun getCheckpoints(): List<Checkpoint> {
        return fullUnit?.checkpoints ?: emptyList()
    }

    fun getStrategicClosure(): StrategicClosure? {
        return fullUnit?.strategicClosure
    }

    fun getTrapMasteryMatrix(): List<TrapMasteryItem> {
        return fullUnit?.trapMasteryMatrix ?: emptyList()
    }

    // 2. Combine static passages with Room progress database
    val passagesWithProgressFlow: Flow<List<PassageWithProgress>> = passageProgressDao.getAllProgress()
        .map { progressList ->
            val progressMap = progressList.associateBy { it.passageId }
            staticPassages.mapIndexed { index, passage ->
                val progress = progressMap[passage.id]
                
                // Determine if locked
                // P01-P03 are free. P04+ are premium, but placeholder is never available premium content.
                val isPremiumGated = index >= 3 && !passage.isPlaceholder
                
                // Also, sequential progression: lock if previous passage is not completed.
                // Ignore previous if previous is a placeholder.
                val isPriorUncompleted = if (index > 0) {
                    val prevPassage = staticPassages[index - 1]
                    val prevProgress = progressMap[prevPassage.id]
                    if (!prevPassage.isPlaceholder) {
                        prevProgress?.isCompleted != true
                    } else {
                        false
                    }
                } else {
                    false
                }

                PassageWithProgress(
                    passage = passage,
                    isCompleted = (progress?.isCompleted ?: false) && !passage.isPlaceholder,
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

    // Expose contentDataSource methods
    suspend fun getTrapIndex(): List<TrapType> {
        return contentDataSource.getTrapIndex()
    }

    suspend fun getVocabularyArsenalUnit1(): List<VocabularyItem> {
        return contentDataSource.getVocabularyArsenalUnit1()
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

    suspend fun reviewVocabularyItem(word: String, quality: Int) {
        val currentItem = vocabularyDao.getVocabularyItem(word) ?: return
        
        // SM-2 algorithm calculations (quality range is 0..5)
        val q = quality.coerceIn(0, 5)
        val newReviewCount = if (q >= 3) currentItem.reviewCount + 1 else 0
        val newWrongCount = if (q < 3) currentItem.wrongCount + 1 else currentItem.wrongCount
        
        val newInterval = when {
            q < 3 -> 1
            newReviewCount == 1 -> 1
            newReviewCount == 2 -> 6
            else -> Math.round(currentItem.interval * currentItem.easeFactor).toInt().coerceAtLeast(1)
        }
        
        // SM-2 EF formulation: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val rawEF = currentItem.easeFactor + (0.1f - (5f - q) * (0.08f + (5f - q) * 0.02f))
        val newEF = rawEF.coerceAtLeast(1.3f)
        
        val newStatus = when {
            q < 3 -> "Difficult"
            q >= 4 -> "Mastered"
            else -> "Learning"
        }
        
        val nextDueDate = System.currentTimeMillis() + (newInterval.toLong() * 24L * 60L * 60L * 1000L)
        
        val updatedItem = currentItem.copy(
            reviewCount = newReviewCount,
            wrongCount = newWrongCount,
            interval = newInterval,
            easeFactor = newEF,
            status = newStatus,
            dueDate = nextDueDate
        )
        vocabularyDao.insertVocabulary(updatedItem)
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
            userStatsDao.insertUserStats(DatabaseUserStats(streak = 0, xp = 0)) // New users start with 0 streak and 0 XP
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
