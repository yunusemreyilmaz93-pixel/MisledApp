package com.example.data

// Professional exam-prep content models for the Misled Reading Lab system

data class Unit(
    val id: String,
    val title: String,
    val description: String,
    val passages: List<Passage>
)

data class Passage(
    val id: String,
    val unitId: String,
    val passageNumber: Int,
    val title: String,
    val level: String, // "Foundation", "Intermediate", "Advanced"
    val timeLimitMinutes: Int,
    val strategy: String,
    val passageText: String,
    val questions: List<Question>,
    val vocabulary: List<VocabularyItem>,
    val masterTechnique: String,
    val sentenceAutopsy: SentenceAutopsy,
    val isPlaceholder: Boolean = false
)

data class Question(
    val id: String,
    val questionNumber: Int,
    val prompt: String,
    val options: List<Option>,
    val correctAnswer: String, // "A", "B", "C", "D", "E"
    val primaryDistractor: String, // "A", "B", "C", "D", "E"
    val explanation: String,
    val trapType: String,
    val evidenceSentence: String
)

data class Option(
    val letter: String, // "A", "B", "C", "D", "E"
    val text: String
) {
    fun toUiString(): String = "$letter) $text"
}

data class QuestionAnalysis(
    val questionId: String,
    val chosenAnswer: String,
    val isCorrect: Boolean,
    val primaryDistractorMatched: Boolean,
    val trapTriggered: String
)

data class VocabularyItem(
    val word: String,
    val partOfSpeech: String,
    val meaning: String, // Turkish
    val synonym: String,
    val example: String,
    val initialStatus: String = "Learning"
)

data class SentenceAutopsy(
    val sentence: String,
    val focus: List<String>
)

data class TrapType(
    val name: String,
    val description: String
)

data class Checkpoint(
    val id: String,
    val title: String,
    val requiredPassageIds: List<String>,
    val description: String
)

data class StrategicClosure(
    val graduationTitle: String,
    val totalRequirementsMet: Boolean,
    val finalReflection: String
)
