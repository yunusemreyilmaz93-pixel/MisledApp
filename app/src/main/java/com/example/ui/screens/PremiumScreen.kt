package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.MainViewModel

@Composable
fun PremiumScreen(viewModel: MainViewModel) {
    val stats by viewModel.userStats.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isPremium = stats.isPremium

    var selectedPlanForCheckout by remember { mutableStateOf<CheckoutPlan?>(null) }
    var billingStatusText by remember { mutableStateOf("Billing Client Connected (v6.1.0)") }
    var showRestoreSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER HERO BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MISLED READING LAB PREMIUM",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isPremium) "ACTIVE PREMIUM STRATEGIST" else "Unlock Advanced Reading Science",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // 2. GOOGLE PLAY BILLING SERVICE INDICATOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = billingStatusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                TextButton(
                    onClick = {
                        showRestoreSuccess = true
                        viewModel.purchasePremiumMock() // Restore by activating premium
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore Purchases", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active State Banner
        if (isPremium) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = "Lifetime Access Unlocked",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Text(
                        text = "Your account is synced with Google Play billing database.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF10B981).copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.downgradePremiumMock() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset to Free Tier (For Demo Testing)", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // 3. FEATURES UNLOCKED PANEL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "INCLUDED IN THE LAB SYSTEM:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val features = listOf(
                    "All 15 Unit 1 reading laboratories (P01-P15)",
                    "Complete Analysis Mode (Traps, Synonyms, Matrix)",
                    "Checkpoint diagnostic checklists & mistake trackers",
                    "Spaced repetition Vocabulary Arsenal features",
                    "Advanced translation gym & YDT/YDS core vocabulary lists",
                    "Detailed statistical breakdowns of trap vulnerabilities"
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. PRICING CARDS (prepared for Google Play Billing)
        if (!isPremium) {
            PricingCard(
                planTitle = "Monthly Strategist Pass",
                price = "₺149 / mo",
                billingFrequency = "Renews monthly. Cancel anytime in Play Store.",
                buttonText = "Subscribe via Google Play",
                onPurchase = {
                    selectedPlanForCheckout = CheckoutPlan(
                        title = "Monthly Strategist Pass",
                        price = "₺149.00/mo",
                        description = "Monthly subscription with automated Play Store renewals. Cancel anytime."
                    )
                }
            )

            PricingCard(
                planTitle = "Annual Mastery Pass",
                price = "₺999 / yr",
                billingFrequency = "Billed annually. Save 45% compared to monthly.",
                hasBadge = true,
                badgeText = "BEST VALUE",
                buttonText = "Subscribe via Google Play",
                onPurchase = {
                    selectedPlanForCheckout = CheckoutPlan(
                        title = "Annual Mastery Pass",
                        price = "₺999.00/yr",
                        description = "Annual premium membership. Save 45%. Billed through Google Play."
                    )
                }
            )

            PricingCard(
                planTitle = "Lifetime Founder License",
                price = "₺1499 once",
                billingFrequency = "Unlimited lifetime access. No renewals.",
                hasBadge = true,
                badgeText = "LIMITED SLOTS",
                buttonText = "Purchase via Google Play",
                onPurchase = {
                    selectedPlanForCheckout = CheckoutPlan(
                        title = "Lifetime Founder License",
                        price = "₺1499.00 once",
                        description = "Lifetime license for early adopters. One-time Google Play purchase."
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // 5. RESTORE SUCCESS TOAST/DIALOG
    if (showRestoreSuccess) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccess = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF10B981))
                    Text("Purchases Restored")
                }
            },
            text = {
                Text("Google Play database synced successfully. Your Lifetime Founder License is restored and fully active.")
            },
            confirmButton = {
                Button(onClick = { showRestoreSuccess = false }) {
                    Text("Acknowledged")
                }
            }
        )
    }

    // 6. SIMULATED GOOGLE PLAY BILLING BOTTOM SHEET
    if (selectedPlanForCheckout != null) {
        val plan = selectedPlanForCheckout!!
        AlertDialog(
            onDismissRequest = { selectedPlanForCheckout = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.padding(top = 100.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Custom colorful Google Play style logo mock
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F9D58)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Play",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = userEmail ?: "strategist@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Misled Reading Lab Premium Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = plan.price,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 15.sp
                    )

                    // Payment Method selector mock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Google Play Balance (Visa •••• 4111)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    // Legal Disclosures
                    Text(
                        text = "By tapping '1-Tap Buy', you agree to Google Play Terms of Service. Dynamic billing updates will apply. Cancel anytime in Google Play Subscriptions panel.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        lineHeight = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purchasePremiumMock()
                        selectedPlanForCheckout = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1-Tap Buy", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedPlanForCheckout = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

data class CheckoutPlan(
    val title: String,
    val price: String,
    val description: String
)

@Composable
fun PricingCard(
    planTitle: String,
    price: String,
    billingFrequency: String,
    hasBadge: Boolean = false,
    badgeText: String = "",
    buttonText: String,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (hasBadge) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = price,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = billingFrequency,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("pricing_buy_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasBadge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    contentColor = if (hasBadge) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
