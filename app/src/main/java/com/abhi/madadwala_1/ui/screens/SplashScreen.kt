package com.abhi.madadwala_1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.abhi.madadwala_1.R
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authState: AuthState = AuthState.Idle,
    onNavigate: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var showWordmark by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    LaunchedEffect(authState) {
        startAnimation = true
        if (!showWordmark) {
            delay(200)
            showWordmark = true
            delay(300)
            showTagline = true
        }
        
        // Wait at least 2 seconds for branding
        delay(1500)

        when (authState) {
            is AuthState.Idle, is AuthState.Error, is AuthState.NewUser, is AuthState.Authenticated -> {
                onNavigate()
            }
            AuthState.Loading -> {
                // Keep waiting
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MadadwalaColors.Teal, MadadwalaColors.TealDarker)
                )
            )
            .clickable { onNavigate() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Mark
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wordmark
            AnimatedVisibility(
                visible = showWordmark,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
            ) {
                Text(
                    text = "madadwala",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn()
            ) {
                Text(
                    text = "trusted help, on time",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Loading Spinner at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
