package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassageWithProgress
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckpointScreen(
    viewModel: MainViewModel,
    checkpointId: String,
    onBack: () -> Unit
) {
    val passages by viewModel.passages.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    
    var reflectionNote by remember { mutableStateOf("") }
    
    // Filter passages belonging to this specific checkpoint
    val targetPassages = when (checkpointId) {
        "checkpoint1" -> passages.take(5)
        "checkpoint2" -> passages.drop(5).take(5)
        else -> passages // closure uses all 15
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (checkpointId) {
                            "checkpoint1" -> "Foundation Checkpoint"
                            "checkpoint2" -> "Intermediate Checkpoint"
                            else -> "STRATEGIC CLOSURE"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            if (checkpointId == "closure") {
                // Strategic Closure / Graduation view
                ClosureView(
                    passages = passages,
                    analytics = analytics,
                    reflectionNote = reflectionNote,
                    onNoteChange = { reflectionNote = it }
                )
            } else {
                // Standard Checkpoint 1 & 2 view
                CheckpointView(
                    checkpointTitle = if (checkpointId == "checkpoint1") "Checkpoint #1" else "Checkpoint #2",
                    targetPassages = targetPassages,
                    analytics = analytics,
                    reflectionNote = reflectionNote,
                    onNoteChange = { reflectionNote = it }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CheckpointView(
    checkpointTitle: String,
    targetPassages: List<PassageWithProgress>,
    analytics: com.example.data.ReadingAnalytics,
    reflectionNote: String,
    onNoteChange: (String) -> Unit
) {
    // 1. Deception Log
    Text(
        text = "DECEPTION LOG",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            targetPassages.forEachIndexed { idx, p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = p.passage.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Score: ${p.score}/3 Questions Correct",
                            fontSize = 11.sp,
                            color = if (p.score == 3) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (p.score == 3) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (p.score == 3) "Flawless" else "Dissected",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (p.score == 3) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (idx < targetPassages.size - 1) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }

    // 2. Mistake Pattern Tracker
    Text(
        text = "MISTAKE PATTERN TRACKER",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val trackerList = remember(targetPassages) {
                val trapWrong = mutableMapOf<String, Int>()
                val trapTotal = mutableMapOf<String, Int>()
                
                val standardTraps = listOf(
                    "Extreme words",
                    "Cause-effect confusion",
                    "Reference/pronoun trap",
                    "Detail vs Main Idea",
                    "Tone/Attitude misread",
                    "Reversal/Negation miss"
                )
                
                standardTraps.forEach { trap ->
                    trapWrong[trap] = 0
                    trapTotal[trap] = 0
                }

                targetPassages.forEach { p ->
                    val answers = p.selectedAnswers
                    p.passage.questions.forEachIndexed { qIdx, question ->
                        val trap = when (question.trapType.lowercase()) {
                            "extreme words", "extreme word" -> "Extreme words"
                            "cause-effect confusion", "cause effect" -> "Cause-effect confusion"
                            "reference/pronoun trap", "reference trap" -> "Reference/pronoun trap"
                            "detail vs main idea", "main idea" -> "Detail vs Main Idea"
                            "tone/attitude misread", "tone misread" -> "Tone/Attitude misread"
                            "reversal/negation miss", "negation miss", "reversal" -> "Reversal/Negation miss"
                            else -> question.trapType
                        }
                        trapTotal[trap] = (trapTotal[trap] ?: 0) + 1
                        if (p.isCompleted && qIdx < answers.size) {
                            val studentAns = answers[qIdx]
                            if (studentAns != question.correctAnswer) {
                                trapWrong[trap] = (trapWrong[trap] ?: 0) + 1
                            }
                        }
                    }
                }
                
                trapTotal.keys.filter { (trapTotal[it] ?: 0) > 0 }.map { trap ->
                    val total = trapTotal[trap] ?: 0
                    val wrong = trapWrong[trap] ?: 0
                    val ratio = if (total > 0) wrong.toFloat() / total.toFloat() else 0.0f
                    trap to ratio
                }
            }

            trackerList.forEach { (trap, ratio) ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trap,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(ratio * 100).toInt()}% Susceptibility",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (ratio > 0.5f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }

    // 3. Vocabulary Inventory
    Text(
        text = "VOCABULARY INVENTORY CHECKLIST",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val vocabList = remember(targetPassages) {
                targetPassages.flatMap { it.passage.vocabulary }
                    .distinctBy { it.word.lowercase() }
                    .map { it.word to it.meaning }
            }

            vocabList.forEach { (word, meaning) ->
                var isChecked by remember { mutableStateOf(true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isChecked = !isChecked }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )
                    Column {
                        Text(
                            text = word,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Turkish: $meaning",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    // 4. Reflection Note
    Text(
        text = "STRATEGIST REFLECTION NOTE",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    OutlinedTextField(
        value = reflectionNote,
        onValueChange = onNoteChange,
        placeholder = { Text("What mistake patterns did you notice? Write your personalized strategy override rule here...") },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    )
}

@Composable
fun ClosureView(
    passages: List<PassageWithProgress>,
    analytics: com.example.data.ReadingAnalytics,
    reflectionNote: String,
    onNoteChange: (String) -> Unit
) {
    val nonPlaceholderPassages = remember(passages) { passages.filter { !it.passage.isPlaceholder } }
    val totalNonPlaceholder = nonPlaceholderPassages.size
    val completedNonPlaceholder = nonPlaceholderPassages.count { it.isCompleted }
    val completionPercent = if (totalNonPlaceholder > 0) (completedNonPlaceholder * 100) / totalNonPlaceholder else 0

    val completedWithTime = remember(passages) { passages.filter { it.isCompleted && !it.passage.isPlaceholder && it.completionTimeSeconds > 0 } }
    val averagePaceText = if (completedWithTime.isNotEmpty()) {
        val avgSeconds = completedWithTime.map { it.completionTimeSeconds }.average()
        val avgMinutes = avgSeconds / 60.0
        String.format(java.util.Locale.US, "%.1f Mins", avgMinutes)
    } else {
        "0.0 Mins"
    }

    // 1. Completion Diploma / Certificate Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF131A2A),
                            Color(0xFF090D16)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DIPLOMA OF STRATEGIC MASTERY",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Misled Reading Lab",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val certText = if (completionPercent >= 100) {
                    "This certifies that the candidate has systematically completed all $totalNonPlaceholder available reading laboratories under Unit 1 (Health & Medicine), demonstrating proficient identification of extreme qualifiers, cause-effect distortions, and referential deceptions."
                } else {
                    "Diploma In Progress: Systematic completion of all $totalNonPlaceholder active reading laboratories under Unit 1 (Health & Medicine) is required to unlock this diploma. Currently at $completedNonPlaceholder of $totalNonPlaceholder completed."
                }

                Text(
                    text = certText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Accuracy Index", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${analytics.accuracy}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unit Completed", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("$completionPercent% Mastered", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }

    // 2. Trap Matrix Grid
    Text(
        text = "THE DECEPTION TRAP MATRIX",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val traps = listOf(
                "Extreme Words (all/only/never)" to "Exploits generalist intuition",
                "Cause-Effect Distortion" to "Flips dependent relationships",
                "Reference / Pronoun Shifting" to "Attributes actions to wrong actors",
                "Detail vs Main Idea" to "Presents truths out of relevance scope",
                "Reversal / Negation" to "Hides facts inside double negatives"
            )

            traps.forEach { (trap, note) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = trap,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mastered",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }

    // 3. Timekeeper's Log
    Text(
        text = "TIMEKEEPER'S LOG",
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Average Analytical Pace",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Calculated across completed exam attempts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = averagePaceText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // 4. Red Flag Challenge
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Dangerous, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "RED FLAG CLAUSE CHALLENGE",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"Hardly had the researchers initialized the genetic splicing than they realized that none but the most resilient vectors could thrive, yet they refrained from abandoning the project altogether.\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Translation Hack: 'Hardly... than' (olur olmaz/yapmasıyla yapması bir oldu), 'none but' (yalnızca/sadece), 'refrained from' (kaçındı/vazgeçmedi). Şıklarda 'hemen vazgeçtiler' veya 'herkes yapabilirdi' çeldiricilerine dikkat edin!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
