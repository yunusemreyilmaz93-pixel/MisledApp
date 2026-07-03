package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReadingRepository(
        application,
        database.vocabularyDao(),
        database.passageProgressDao(),
        database.userStatsDao()
    )

    // 1. Core Flows from Repository
    val passages = repository.passagesWithProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DatabaseUserStats())

    val vocabulary = repository.allVocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics = repository.analyticsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingAnalytics(0, 0, "None", 0, 0, emptyMap()))

    // 2. Navigation & Local UI State
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _activePassageId = MutableStateFlow<String?>(null)
    val activePassageId: StateFlow<String?> = _activePassageId.asStateFlow()

    // Combined active passage with database status
    val activePassage: StateFlow<PassageWithProgress?> = combine(passages, _activePassageId) { list, activeId ->
        list.find { it.passage.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 3. Authentication & User Profile State
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    // 4. Active Passage Test Taking State
    private val _examModeActive = MutableStateFlow(true)
    val examModeActive: StateFlow<Boolean> = _examModeActive.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, String>>(emptyMap()) // index -> option letter
    val selectedAnswers: StateFlow<Map<Int, String>> = _selectedAnswers.asStateFlow()

    private val _timerSecondsLeft = MutableStateFlow(0)
    val timerSecondsLeft: StateFlow<Int> = _timerSecondsLeft.asStateFlow()

    private var timerJob: Job? = null

    // 5. Vocabulary Tab Filters
    val vocabSearchQuery = MutableStateFlow("")
    val vocabStatusFilter = MutableStateFlow("All") // "All", "Learning", "Mastered", "Difficult"

    // Filtered vocabulary list
    val filteredVocabulary: StateFlow<List<DatabaseVocabularyItem>> = combine(
        vocabulary, vocabSearchQuery, vocabStatusFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchQuery = item.word.contains(query, ignoreCase = true) || 
                             item.meaning.contains(query, ignoreCase = true)
            val matchFilter = when (filter) {
                "All" -> true
                else -> item.status.equals(filter, ignoreCase = true)
            }
            matchQuery && matchFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeStatsIfEmpty()
        }
    }

    // Navigation triggers
    fun setTab(tab: String) {
        _currentTab.value = tab
        _activePassageId.value = null
    }

    // Authentication actions
    fun loginWithEmail(email: String) {
        _userEmail.value = email
        _isGuest.value = false
    }

    fun loginAsGuest() {
        _userEmail.value = null
        _isGuest.value = true
    }

    fun logout() {
        _userEmail.value = null
        _isGuest.value = false
    }

    // Passage study cycle actions
    fun startPassageStudy(passageId: String) {
        viewModelScope.launch {
            val pWithProg = passages.value.find { it.passage.id == passageId } ?: return@launch
            _activePassageId.value = passageId
            _selectedAnswers.value = emptyMap()
            
            if (pWithProg.isCompleted) {
                // If already completed, directly open Analysis Mode
                _examModeActive.value = false
                _timerSecondsLeft.value = 0
                // Populate past selected answers
                val pastAnswers = pWithProg.selectedAnswers
                val ansMap = mutableMapOf<Int, String>()
                pastAnswers.forEachIndexed { index, ans ->
                    ansMap[index] = ans
                }
                _selectedAnswers.value = ansMap
            } else {
                // Start a fresh Exam Mode attempt with countdown timer
                _examModeActive.value = true
                val totalSeconds = pWithProg.passage.timeLimitMinutes * 60
                _timerSecondsLeft.value = totalSeconds
                startCountdownTimer()
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSecondsLeft.value > 0 && _examModeActive.value) {
                delay(1000)
                _timerSecondsLeft.value = _timerSecondsLeft.value - 1
            }
            if (_timerSecondsLeft.value == 0 && _examModeActive.value) {
                // Time's up! Force submit empty answers or current selections
                submitExamAttempt()
            }
        }
    }

    fun selectAnswer(questionIndex: Int, letter: String) {
        if (!_examModeActive.value) return // Block modifications in analysis mode
        val current = _selectedAnswers.value.toMutableMap()
        current[questionIndex] = letter
        _selectedAnswers.value = current
    }

    fun submitExamAttempt() {
        timerJob?.cancel()
        val pId = _activePassageId.value ?: return
        val passageItem = activePassage.value?.passage ?: return
        
        viewModelScope.launch {
            // Calculate final score
            val answersList = (0 until passageItem.questions.size).map { idx ->
                _selectedAnswers.value[idx] ?: ""
            }
            
            var score = 0
            passageItem.questions.forEachIndexed { index, question ->
                if (index < answersList.size && answersList[index] == question.correctAnswer) {
                    score++
                }
            }

            val elapsedSeconds = (passageItem.timeLimitMinutes * 60) - _timerSecondsLeft.value
            
            // Save to database via repository
            repository.completePassage(
                passageId = pId,
                score = score,
                selectedAnswers = answersList,
                timeSeconds = elapsedSeconds
            )
            
            // Toggle to Analysis Mode for deep learning
            _examModeActive.value = false
        }
    }

    fun exitPassageStudy() {
        timerJob?.cancel()
        _activePassageId.value = null
        _selectedAnswers.value = emptyMap()
    }

    // Vocabulary Actions
    fun addWordToArsenal(word: String, partOfSpeech: String, meaning: String, synonym: String, example: String) {
        viewModelScope.launch {
            val item = DatabaseVocabularyItem(
                word = word.trim(),
                partOfSpeech = partOfSpeech.trim(),
                meaning = meaning.trim(),
                synonym = synonym.trim(),
                example = example.trim(),
                status = "Learning"
            )
            repository.addWordToArsenal(item)
        }
    }

    fun updateWordStatus(word: String, status: String) {
        viewModelScope.launch {
            repository.updateWordStatus(word, status)
        }
    }

    fun reviewVocabularyItem(word: String, quality: Int) {
        viewModelScope.launch {
            repository.reviewVocabularyItem(word, quality)
        }
    }

    fun removeWordFromArsenal(item: DatabaseVocabularyItem) {
        viewModelScope.launch {
            repository.deleteWordFromArsenal(item)
        }
    }

    // Profile updates
    fun updateTargetExam(exam: String) {
        viewModelScope.launch {
            repository.updateTargetExam(exam)
        }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(minutes)
        }
    }

    fun purchasePremiumMock() {
        viewModelScope.launch {
            repository.updatePremiumStatus(true)
        }
    }

    fun downgradePremiumMock() {
        viewModelScope.launch {
            repository.updatePremiumStatus(false)
        }
    }

    fun getCheckpoints(): List<Checkpoint> = repository.getCheckpoints()
    fun getStrategicClosure(): StrategicClosure? = repository.getStrategicClosure()
    fun getTrapMasteryMatrix(): List<TrapMasteryItem> = repository.getTrapMasteryMatrix()
}
