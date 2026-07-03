package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassageWithProgress
import com.example.data.Question
import com.example.data.VocabularyItem
import com.example.ui.MainViewModel

@Composable
fun HighlightedPassageText(passageText: String, phrasesToHighlight: List<String>) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val textLength = passageText.length
        
        // Find all matches of any of the phrases
        val matches = mutableListOf<Pair<Int, Int>>() // start index to end index
        phrasesToHighlight.filter { it.length > 3 }.forEach { phrase ->
            var index = passageText.indexOf(phrase, ignoreCase = true)
            while (index != -1) {
                matches.add(Pair(index, index + phrase.length))
                index = passageText.indexOf(phrase, index + 1, ignoreCase = true)
            }
        }
        
        // Sort matches by start index, merging overlapping matches
        val sortedMerged = mutableListOf<Pair<Int, Int>>()
        if (matches.isNotEmpty()) {
            val sorted = matches.sortedBy { it.first }
            var current = sorted[0]
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.first <= current.second) {
                    // Merge
                    current = Pair(current.first, maxOf(current.second, next.second))
                } else {
                    sortedMerged.add(current)
                    current = next
                }
            }
            sortedMerged.add(current)
        }
        
        // Build the styled string
        sortedMerged.forEach { match ->
            if (match.first > currentIndex) {
                append(passageText.substring(currentIndex, match.first))
            }
            withStyle(
                style = SpanStyle(
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(passageText.substring(match.first, match.second))
            }
            currentIndex = match.second
        }
        
        if (currentIndex < textLength) {
            append(passageText.substring(currentIndex, textLength))
        }
    }
    
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            lineHeight = 26.sp,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Justify
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageScreen(viewModel: MainViewModel) {
    val activePassageWrapper by viewModel.activePassage.collectAsState()
    val examModeActive by viewModel.examModeActive.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val vocabularyInArsenal by viewModel.vocabulary.collectAsState()

    val pWrapper = activePassageWrapper ?: return

    val passage = pWrapper.passage

    if (passage.isPlaceholder) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Content being prepared",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "This passage (\"${passage.title}\") is currently being prepared for the upcoming Misled edition. It will soon contain premium analytical reading drills, sentence autopsies, and strategic traps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = { viewModel.exitPassageStudy() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Lab", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        return
    }
    
    val phrasesToHighlight = remember(passage, examModeActive) {
        if (examModeActive) {
            emptyList()
        } else {
            val list = mutableListOf<String>()
            passage.questions.forEach { q ->
                val pattern = "['\"](.*?)['\"]".toRegex()
                pattern.findAll(q.explanation).forEach { match ->
                    list.add(match.groupValues[1])
                }
            }
            list
        }
    }

    var showStrategyHint by remember { mutableStateOf(false) }

    // Safe scrolling state for the entire reading container
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = passage.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitPassageStudy() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (examModeActive) {
                        // Timer Indicator
                        val minutes = timerSecondsLeft / 60
                        val seconds = timerSecondsLeft % 60
                        val timerColor = if (timerSecondsLeft < 60) Color.Red else MaterialTheme.colorScheme.primary
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(timerColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = timerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                fontWeight = FontWeight.Bold,
                                color = timerColor,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Analysis Badge
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ANALYSIS MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Level Indicator & Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = passage.level.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Target Time: ${passage.timeLimitMinutes} mins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Strategy Hint:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = passage.strategy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }

            // 2. Reading Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PASSAGE READING",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    HighlightedPassageText(passage.passageText, phrasesToHighlight)
                }
            }

            // Dynamic Divider
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            if (passage.questions.isEmpty()) {
                // Handle placeholders nicely
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Placeholder Lab Module",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "This passage is configured in the Unit structure but lacks active exam seed content. Full analysis and questions will unlock in subsequent content revisions.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // 3. Question Blocks
                Text(
                    text = "STRATEGIC QUESTIONS",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                passage.questions.forEachIndexed { qIdx, question ->
                    QuestionBlock(
                        question = question,
                        qIdx = qIdx,
                        examModeActive = examModeActive,
                        selectedAnswers = selectedAnswers,
                        onSelectAnswer = { index, letter -> viewModel.selectAnswer(index, letter) }
                    )
                }

                // 4. Action / Submission Area
                if (examModeActive) {
                    Button(
                        onClick = { viewModel.submitExamAttempt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_exam_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(
                            text = "Submit Answers for Strategic Analysis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    // Analysis Mode Extra Sections: Vocabulary Matrix, Sentence Autopsy, Master Technique
                    AnalysisExtraSections(
                        passage = passage,
                        vocabularyInArsenal = vocabularyInArsenal,
                        onAddWord = { vocabItem -> viewModel.addWordToArsenal(vocabItem.word, vocabItem.partOfSpeech, vocabItem.meaning, vocabItem.synonym, vocabItem.example) },
                        onRemoveWord = { vocabItem -> 
                            val existing = vocabularyInArsenal.find { it.word == vocabItem.word }
                            if (existing != null) viewModel.removeWordFromArsenal(existing)
                        }
                    )

                    Button(
                        onClick = { viewModel.exitPassageStudy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("exit_passage_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(
                            text = "Return to Unit Curriculum",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun QuestionBlock(
    question: Question,
    qIdx: Int,
    examModeActive: Boolean,
    selectedAnswers: Map<Int, String>,
    onSelectAnswer: (Int, String) -> Unit
) {
    val selectedLetter = selectedAnswers[qIdx] ?: ""
    val isCorrect = selectedLetter == question.correctAnswer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Question Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUESTION ${qIdx + 1}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!examModeActive) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCorrect) "CORRECT" else "DECEIVED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.prompt,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Multiple Choice Options (A - E)
            question.options.forEach { option ->
                val optionLetter = option.letter
                val isSelected = selectedLetter == optionLetter
                
                // Color codes for options based on Exam Mode vs Analysis Mode
                val optionBorder = when {
                    examModeActive && isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    examModeActive -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    // Analysis Mode: Highlight correct and student's selections
                    optionLetter == question.correctAnswer -> BorderStroke(1.5.dp, Color(0xFF10B981))
                    isSelected && optionLetter != question.correctAnswer -> BorderStroke(1.5.dp, Color(0xFFEF4444))
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }

                val optionBg = when {
                    examModeActive && isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    examModeActive -> MaterialTheme.colorScheme.surface
                    // Analysis Mode: soft colors
                    optionLetter == question.correctAnswer -> Color(0xFF10B981).copy(alpha = 0.08f)
                    isSelected && optionLetter != question.correctAnswer -> Color(0xFFEF4444).copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = examModeActive) { onSelectAnswer(qIdx, optionLetter) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = optionBg),
                    border = optionBorder
                ) {
                    Text(
                        text = option.toUiString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Analysis Mode details
            if (!examModeActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Detail Analysis Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisBadgeColumn(
                        label = "YOUR CHOICE",
                        value = if (selectedLetter.isEmpty()) "Skipped" else selectedLetter,
                        color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    AnalysisBadgeColumn(
                        label = "CORRECT ANS",
                        value = question.correctAnswer,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    AnalysisBadgeColumn(
                        label = "PRIMARY DISTRACTOR",
                        value = question.primaryDistractor,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Evidence Section
                val evidence = remember(question) {
                    val pattern = "['\"](.*?)['\"]".toRegex()
                    val matches = pattern.findAll(question.explanation).map { it.groupValues[1] }.toList()
                    if (matches.isNotEmpty()) matches.joinToString("\n• ", prefix = "• ") else "Refer to the bolded terms in the passage reading card above."
                }

                Text(
                    text = "EVIDENCE FROM PASSAGE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = evidence,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .padding(bottom = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Explanations Card
                Text(
                    text = "STRATEGIC RATIONALE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Trap Diagnostics banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "TRAP DETECTED: ${question.trapType}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Distractor ${question.primaryDistractor} functions as a highly misleading choice. This trap exploits students who skip syntax flow and rely on simple word matches.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisExtraSections(
    passage: com.example.data.Passage,
    vocabularyInArsenal: List<com.example.data.DatabaseVocabularyItem>,
    onAddWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        // 1. Vocabulary Matrix
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "VOCABULARY MATRIX",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                passage.vocabulary.forEach { item ->
                    val isSaved = vocabularyInArsenal.any { it.word == item.word }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.word,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${item.partOfSpeech.lowercase()})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "Meaning: ${item.meaning}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Synonyms: ${item.synonym}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Ex: ${item.example}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isSaved) onRemoveWord(item) else onAddWord(item)
                            }
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove" else "Add",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }

        // 2. Sentence Autopsy
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SENTENCE AUTOPSY",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = passage.sentenceAutopsy.sentence,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CRITICAL SYNTACTIC ANATOMY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )

                passage.sentenceAutopsy.focus.forEach { focusItem ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = focusItem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 3. Master Technique Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MASTER TECHNIQUE",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = passage.masterTechnique,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AnalysisBadgeColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
