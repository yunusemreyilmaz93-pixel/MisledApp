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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassageWithProgress
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
    viewModel: MainViewModel,
    onOpenCheckpoint: (String) -> Unit // Navigates to a detailed checkpoint sheet
) {
    val passages by viewModel.passages.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val userIsPremium = stats.isPremium

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unit Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "UNIT 1",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Health & Medicine",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "15 Strategic Reading Passages configured to dissect common English exam trickery.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // Segment 1: Foundation (P01 - P05)
        SectionHeader(title = "Foundation Stage (6 Minute Limits)", description = "Focus on basic causal chains and core vocabulary.")
        passages.take(5).forEachIndexed { index, item ->
            PassageCard(
                item = item,
                index = index + 1,
                userIsPremium = userIsPremium,
                onClick = { viewModel.startPassageStudy(item.passage.id) }
            )
        }

        // Checkpoint 1 Trigger
        val p05Completed = passages.find { it.passage.id == "p05" }?.isCompleted ?: false
        CheckpointCard(
            title = "Checkpoint #1: Foundation Diagnostics",
            description = "Analyze extreme word traps and review medical vocabulary collected from passages P01-P05.",
            isUnlocked = p05Completed,
            onClick = { if (p05Completed) onOpenCheckpoint("checkpoint1") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Segment 2: Intermediate (P06 - P10)
        SectionHeader(title = "Intermediate Stage (8 Minute Limits)", description = "Decipher subtle negation reversals and referential pronoun tricks.")
        passages.drop(5).take(5).forEachIndexed { index, item ->
            PassageCard(
                item = item,
                index = index + 6,
                userIsPremium = userIsPremium,
                onClick = { viewModel.startPassageStudy(item.passage.id) }
            )
        }

        // Checkpoint 2 Trigger
        val p10Completed = passages.find { it.passage.id == "p10" }?.isCompleted ?: false
        CheckpointCard(
            title = "Checkpoint #2: Analytical Deception",
            description = "Dissect cause-effect swaps and attitude-misread distractors in passages P06-P10.",
            isUnlocked = p10Completed,
            onClick = { if (p10Completed) onOpenCheckpoint("checkpoint2") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Segment 3: Advanced (P11 - P15)
        SectionHeader(title = "Advanced Stage (10 Minute Limits)", description = "Academic synthesis, extreme dense structures, and speed-reading challenges.")
        passages.drop(10).take(5).forEachIndexed { index, item ->
            PassageCard(
                item = item,
                index = index + 11,
                userIsPremium = userIsPremium,
                onClick = { viewModel.startPassageStudy(item.passage.id) }
            )
        }

        // Checkpoint 3 / Strategic Closure Trigger
        val p15Completed = passages.find { it.passage.id == "p15" }?.isCompleted ?: false
        CheckpointCard(
            title = "Strategic Closure & Unit Graduation",
            description = "A comprehensive heatmap, red-flag sentence challenge, and completion certificate award.",
            isUnlocked = p15Completed,
            isClosure = true,
            onClick = { if (p15Completed) onOpenCheckpoint("closure") }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionHeader(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
    }
}

@Composable
fun PassageCard(
    item: PassageWithProgress,
    index: Int,
    userIsPremium: Boolean,
    onClick: () -> Unit
) {
    val isLocked = item.isLocked(userIsPremium)
    
    val badgeBg = when (item.passage.level) {
        "Foundation" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        "Intermediate" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
    }

    val badgeText = when (item.passage.level) {
        "Foundation" -> MaterialTheme.colorScheme.primary
        "Intermediate" -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passage_card_$index")
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (item.isCompleted) {
            BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
        } else if (!isLocked) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "P${String.format("%02d", index)}",
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(badgeBg, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.passage.level,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${item.passage.timeLimitMinutes}m",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.passage.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Serif
                )
            }

            // Right status indicator
            Box(modifier = Modifier.padding(start = 8.dp)) {
                if (isLocked) {
                    if (item.isPremiumGated && !userIsPremium) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Premium",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "PREMIUM",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Sequential",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (item.isCompleted) {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "${item.score}/3 Correct",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CheckpointCard(
    title: String,
    description: String,
    isUnlocked: Boolean,
    isClosure: Boolean = false,
    onClick: () -> Unit
) {
    val cardBg = if (isClosure) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    }

    val strokeColor = if (isUnlocked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) cardBg else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, strokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isClosure) Icons.Default.WorkspacePremium else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = title.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
