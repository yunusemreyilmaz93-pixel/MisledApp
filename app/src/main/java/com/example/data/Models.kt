package com.example.data

data class UnitData(
    val id: String,
    val title: String,
    val description: String,
    val passages: List<Passage>
)

data class Passage(
    val id: String,
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
    val prompt: String,
    val options: List<String>, // Option strings including letters (e.g. "A) ...")
    val correctAnswer: String, // "A", "B", "C", "D", "E"
    val primaryDistractor: String, // "A", "B", "C", "D", "E"
    val explanation: String,
    val trapType: String // e.g. "Extreme Words", "Cause-Effect Confusion", "Reference Trap", "Detail vs Main Idea", "Tone Misread", "Reversal"
)

data class VocabularyItem(
    val word: String,
    val partOfSpeech: String,
    val meaning: String, // Turkish
    val synonym: String,
    val example: String,
    val initialStatus: String = "Learning" // "Learning", "Mastered", "Difficult"
)

data class SentenceAutopsy(
    val sentence: String,
    val focus: List<String>
)

object SeedData {
    val trapTypesInfo = mapOf(
        "Extreme Words" to "Look for limiting words like 'all', 'only', 'never', 'solely' that make a choice too restrictive.",
        "Cause-Effect Confusion" to "Mistaking the result for the cause, or reversing the causal relationship specified in the text.",
        "Reference Trap" to "Using a pronoun or detail incorrectly to refer to a noun that wasn't the original actor.",
        "Detail vs Main Idea" to "Selecting a detail that is technically true but doesn't answer the main-idea question.",
        "Tone Misread" to "Overstating the author's neutral explanation as highly critical or overly positive.",
        "Reversal" to "Directly contradicting the facts in the passage, often using double negations or subtle antonyms."
    )
}
