package com.abhi.madadwala_1.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusRequester
import com.abhi.madadwala_1.R
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(onSendOtp: (String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var startAnimation by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // ---------- Card slide-up ----------
    val cardOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else screenHeight,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "CardSlide"
    )

    // ---------- Header fade/scale-in ----------
    val headerEntrance by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "HeaderEntrance"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "LogoScale"
    )

    // ---------- Input focus glow ----------
    val fieldScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "FieldScale"
    )

    // ---------- Button ready pulse ----------
    val isReady = phoneNumber.length == 10

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(800)
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MadadwalaColors.Green,
                        MadadwalaColors.Green.copy(alpha = 0.9f)
                    )
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ==================== TOP HERO SECTION ====================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    alpha = headerEntrance
                    translationY = (1f - headerEntrance) * -24f
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { /* Help */ }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "Help",
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Secure Login",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.45f))

            // App Logo in a soft white badge
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Madadwala Logo",
                    modifier = Modifier.size(104.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Madadwala",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            )
            Text(
                text = "Join 10,000+ happy households",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // ==================== LOGIN CARD ====================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffset)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Small drag-handle style accent for a modern bottom-sheet feel
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MadadwalaColors.LightGray.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MadadwalaColors.Green
                    )
                )
                Text(
                    text = "Enter your mobile number to get started",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MadadwalaColors.Gray
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Phone Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 10) phoneNumber = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(fieldScale)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .shadow(
                            elevation = if (isFocused) 6.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MadadwalaColors.Green.copy(alpha = 0.3f)
                        ),
                    prefix = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+91", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MadadwalaColors.LightGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = null,
                            tint = if (isFocused) MadadwalaColors.Green else MadadwalaColors.Gray
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = isReady,
                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = MadadwalaColors.Green
                            )
                        }
                    },
                    placeholder = { Text("00000 00000", color = MadadwalaColors.LightGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MadadwalaColors.Green,
                        unfocusedBorderColor = MadadwalaColors.LightGray,
                        cursorColor = MadadwalaColors.Green,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Send OTP",
                    onClick = { onSendOtp(phoneNumber) },
                    enabled = isReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isReady) 10.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MadadwalaColors.Green.copy(alpha = 0.4f)
                        )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy. Standard operator charges may apply.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MadadwalaColors.Gray,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}