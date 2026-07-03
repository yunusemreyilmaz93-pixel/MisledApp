package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReadingAnalytics
import com.example.data.PassageWithProgress
import com.example.ui.MainViewModel

@Composable
fun ProgressScreen(viewModel: MainViewModel) {
    val analytics by viewModel.analytics.collectAsState()
    val passages by viewModel.passages.collectAsState()
    val userStats by viewModel.userStats.collectAsState()

    var selectedTrack by remember { mutableStateOf("YDT") }
    val completedList = passages.filter { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN TITLE
        Text(
            text = "PERFORMANCE ANALYTICS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 1. STATS GRID BANNERS (ACCURACY & STUDY TIME)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accuracy Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Accuracy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${analytics.accuracy}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: 85% for Exam Readiness",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Study Time Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Study Time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        // Calculate mock minutes based on completed passages
                        val minutesSpent = completedList.sumOf { it.completionTimeSeconds } / 60 + 5
                        Text(
                            text = "${minutesSpent}m",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily Goal: ${userStats.dailyGoalMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // 2. ACCURACY TREND LINE CHART
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACCURACY TREND",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Custom Line Drawing based strictly on actual completed passages
                val points = remember(completedList) {
                    val list = completedList.map { p ->
                        val totalQuestions = p.passage.questions.size
                        val ratio = if (totalQuestions > 0) p.score.toFloat() / totalQuestions.toFloat() else 0f
                        ratio * 100f
                    }
                    if (list.isEmpty()) listOf(0f) else list
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val strokeColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val maxVal = 100f
                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        val path = Path()
                        points.forEachIndexed { idx, point ->
                            val x = idx * stepX
                            val y = height - (point / maxVal * height)
                            if (idx == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            drawCircle(
                                color = strokeColor,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Baseline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Active Level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Ready", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // 3. WEAKEST TRAP & STRATEGY ENGINE
        val targetTrap = analytics.weakestTrapType
        if (targetTrap != "None Detected" && targetTrap != "None") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "WEAKEST TRAP: ${targetTrap.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = when (targetTrap) {
                                "Extreme Words" -> "ÖSYM'nin en popüler tuzaklarından biridir. Metinde 'bazen' veya 'çoğunlukla' geçen ifadeleri, şıklarda 'her zaman', 'sadece', 'asla' gibi kesinlik bildiren kelimelerle değiştirirler. Çözüm: Şıklardaki 'all, only, must, solely, never' gibi kelimeleri elerken iki kez düşünün!"
                                "Cause-Effect Confusion" -> "Nedeni sonuçla, sonucu da neden ile karıştırma tuzağıdır. Metinde A, B'ye yol açtı derken şıkta B, A'ya yol açtı der. Çözüm: Bağlaçların (because, lead to, result in) yönünü oklarla çizerek takip edin."
                                "Reversal" -> "Metindeki gerçeği tamamen tersine çevirme veya olumsuzlaştırma tuzağıdır. Çözüm: Fiillere ve sıfatlara dikkat edin. Antonym (zıt anlamlı) kelime oyunlarını fark edin."
                                "Detail vs Main Idea" -> "Metinde geçen ve tamamen doğru olan teknik bir detayı, paragrafın ana fikrini soran soruya cevap olarak koyma tuzağıdır. Çözüm: Soru kökünün 'mainly' veya 'primarily' isteyip istemediğini kontrol edin."
                                "Reference Trap" -> "Zamirlerin referans noktalarını kafa karıştıracak şekilde eşleştirme tuzağıdır. Çözüm: Zamirin (it, they, this) metindeki gerçek öznesini bulup yerine koyarak cümleyi okuyun."
                                else -> "Tuzak analiziniz güncelleniyor. Yanlış cevap verdiğiniz sorular üzerinden en çok düştüğünüz çeldirici yöntemleri tespit ederek size özel taktikler üretiyoruz."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. ROADMAPS & EXAM TRACKS
        Text(
            text = "EXAM ROADMAPS",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp)
        )

        // Track Selector Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("YDT", "YDS", "YÖKDİL").forEach { track ->
                val active = selectedTrack == track
                val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                val fg = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable { selectedTrack = track }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = track,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg
                    )
                }
            }
        }

        // Selected Track Roadmap Timeline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "$selectedTrack STRATEGIC PATHWAY",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                val roadmapSteps = when (selectedTrack) {
                    "YDT" -> listOf(
                        RoadmapStep("Reading Foundations", "Master sentence-level syntax, tenses, and simple causal chains in under 6 minutes.", true, "Milestone 1"),
                        RoadmapStep("Trap Filtration", "Identify 'Extreme Words' and 'Reversals' in science and social passages.", completedList.size >= 3, "Milestone 2"),
                        RoadmapStep("Speed & Precision", "Optimize timing limits down to 8 minutes for intermediate-level texts.", completedList.size >= 6, "Milestone 3"),
                        RoadmapStep("Mock Strategy Integration", "Conduct comprehensive strategic autopsies of full-length paragraphs.", completedList.size >= 10, "Milestone 4")
                    )
                    "YDS" -> listOf(
                        RoadmapStep("Academic Lexicon", "Build core academic vocabulary across social, health, and science areas.", true, "Milestone 1"),
                        RoadmapStep("Complex Syntactic Parsing", "Analyze nested clauses, relative clauses, and participial phrases.", completedList.size >= 4, "Milestone 2"),
                        RoadmapStep("Extreme Detail Extraction", "Isolate subtle modifiers and primary distractors in dense prose.", completedList.size >= 8, "Milestone 3"),
                        RoadmapStep("Dual-Passage Mastery", "Compare opposing viewpoints and synthesize main arguments dynamically.", completedList.size >= 12, "Milestone 4")
                    )
                    else -> listOf(
                        RoadmapStep("Discipline Categorization", "Segment training into social, health, and science vocab blocks.", true, "Milestone 1"),
                        RoadmapStep("Structural Connectors", "Study contrast and causal sentence transition markers closely.", completedList.size >= 3, "Milestone 2"),
                        RoadmapStep("Paragraph Completion", "Master insertion strategies and structural cohesiveness indicators.", completedList.size >= 7, "Milestone 3"),
                        RoadmapStep("Speed Diagnostics", "Simulate actual exam conditions under highly rigorous timing limits.", completedList.size >= 11, "Milestone 4")
                    )
                }

                // Vertical Timeline Drawing
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    roadmapSteps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Circle Indicator and vertical line
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (step.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (step.completed) Icons.Default.Check else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (step.completed) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                if (index < roadmapSteps.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(44.dp)
                                            .background(
                                                if (step.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Content Card
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = step.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (step.completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = step.milestone,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (step.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. VOCABULARY RETENTION CHART
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "VOCABULARY RETENTION",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { if (analytics.totalVocabularyAdded > 0) analytics.masteredVocabularyCount.toFloat() / analytics.totalVocabularyAdded.toFloat() else 0.75f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = if (analytics.totalVocabularyAdded > 0) "${(analytics.masteredVocabularyCount.toFloat() / analytics.totalVocabularyAdded.toFloat() * 100).toInt()}%" else "75%",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Words in Arsenal: ${analytics.totalVocabularyAdded}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mastered Status: ${analytics.masteredVocabularyCount} words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Spaced Repetition due list is synchronized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. HISTORIC COMPLETED PASSAGES LIST
        if (completedList.isNotEmpty()) {
            Text(
                text = "TRAINING LOGS",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )

            completedList.forEach { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = p.passage.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.passage.level,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${p.completionTimeSeconds / 60}m ${p.completionTimeSeconds % 60}s",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Score Badge
                        val isPerfect = p.score == p.passage.questions.size
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isPerfect) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${p.score}/${p.passage.questions.size}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (isPerfect) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

data class RoadmapStep(
    val title: String,
    val description: String,
    val completed: Boolean,
    val milestone: String
)
