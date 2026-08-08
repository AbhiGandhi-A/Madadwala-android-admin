package com.abhi.madadwala_1.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.data.remote.UserResponse
import com.abhi.madadwala_1.ui.components.GhostButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@Composable
fun VerificationPendingScreen(
    user: UserResponse? = null,
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulsing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MadadwalaColors.Cream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(if (user?.kycRejected == true) 1f else pulseScale)
                .clip(CircleShape)
                .background((if (user?.kycRejected == true) MadadwalaColors.Red else MadadwalaColors.Teal).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (user?.kycRejected == true) Icons.Default.Cancel else Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (user?.kycRejected == true) MadadwalaColors.Red else MadadwalaColors.Teal,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (user?.kycRejected == true) "Application Rejected" else "Your application is under review",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (user?.kycRejected == true) MadadwalaColors.Red else MadadwalaColors.Ink
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (user?.kycRejected == true) 
                "Reason: ${user.kycRejectionReason ?: "Information provided was incorrect."}\nPlease go to your profile to update details."
                else "We're verifying your details. This usually takes 24-48 hours. You'll be notified once approved.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MadadwalaColors.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Summary card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SUBMITTED FOR",
                    style = MaterialTheme.typography.labelMedium.copy(color = MadadwalaColors.Gray)
                )
                Text(
                    text = "${user?.category ?: "Provider"} Services",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (user?.email != null) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(color = MadadwalaColors.Gray)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        GhostButton(
            text = if (user?.kycRejected == true) "Go to Dashboard" else "Refresh status",
            onClick = onRefresh
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onLogout) {
            Text(
                "Log out",
                color = MadadwalaColors.Red,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
