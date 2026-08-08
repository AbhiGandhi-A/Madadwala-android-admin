package com.abhi.madadwala_1.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentIcon: ImageVector,
    val color: Color
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Verified Professionals",
            "Get access to 5000+ background-verified experts for all your home needs.",
            Icons.Rounded.VerifiedUser,
            Icons.Rounded.Shield,
            MadadwalaColors.Green
        ),
        OnboardingPage(
            "Instant Booking",
            "Book any service in less than 60 seconds with our streamlined experience.",
            Icons.Rounded.FlashOn,
            Icons.Rounded.Timer,
            MadadwalaColors.Lime
        ),
        OnboardingPage(
            "Live Service Tracking",
            "Real-time GPS tracking to see exactly when your professional will arrive.",
            Icons.Rounded.LocationOn,
            Icons.Rounded.Route,
            MadadwalaColors.Amber
        ),
        OnboardingPage(
            "Secure Digital Payments",
            "Transparent pricing and secure in-app payments with 100% money-back guarantee.",
            Icons.Rounded.AccountBalanceWallet,
            Icons.Rounded.Lock,
            MadadwalaColors.Green
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold(
        containerColor = MadadwalaColors.Cream,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        "Skip",
                        color = MadadwalaColors.Green,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                OnboardingPageView(pages[pageIndex])
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val color = if (isSelected) MadadwalaColors.Green else MadadwalaColors.LightGray
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "IndicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(width = width, height = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Bottom Button
                PrimaryButton(
                    text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Continue",
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingPageView(page: OnboardingPage) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main Illustration Card
        Box(contentAlignment = Alignment.Center) {
            // Decorative background circles
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(page.color.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
            
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale),
                shape = RoundedCornerShape(48.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = page.color
                    )
                    
                    // Floating small icon
                    Icon(
                        imageVector = page.accentIcon,
                        contentDescription = null,
                        tint = page.color.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                color = MadadwalaColors.Green,
                letterSpacing = (-0.5).sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MadadwalaColors.Gray,
                lineHeight = 26.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
