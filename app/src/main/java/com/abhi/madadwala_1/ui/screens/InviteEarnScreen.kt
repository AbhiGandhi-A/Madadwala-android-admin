package com.abhi.madadwala_1.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.data.remote.UserResponse
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteEarnScreen(
    user: UserResponse?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Generate referral code: MW + last 6 chars of UID
    val referralCode = remember(user?.uid) {
        if (user != null) {
            "MW${user.uid.takeLast(6).uppercase()}"
        } else {
            "MWXXXXXX"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite & Earn", fontWeight = FontWeight.Bold, color = MadadwalaColors.Green) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9F9F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon / Illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MadadwalaColors.Green.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MadadwalaColors.Green,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Refer Friends & Earn Money",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Invite your friends to Madadwala. They get ₹50 discount on their first booking, and you get ₹10 in your wallet!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Referral Code Box
            Text(
                text = "YOUR REFERRAL CODE",
                style = MaterialTheme.typography.labelLarge,
                color = MadadwalaColors.Green,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, MadadwalaColors.Green.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = referralCode,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MadadwalaColors.Green,
                    letterSpacing = 4.sp
                )

                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(referralCode))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MadadwalaColors.Green)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action Buttons
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val shareMessage = """
                            Hey! Download Madadwala for all your home service needs. 
                            Use my referral code $referralCode during signup and get ₹50 discount on your first booking!
                            Download now: https://madadwala.com/download
                        """.trimIndent()
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Share Invite Link", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // How it works
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    HowItWorksItem(
                        number = "1",
                        title = "Share your code",
                        subtitle = "Send your referral code to your friends and family."
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HowItWorksItem(
                        number = "2",
                        title = "Friend signs up",
                        subtitle = "Your friend enters your code while creating their account."
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HowItWorksItem(
                        number = "3",
                        title = "Both get rewarded",
                        subtitle = "They get ₹50 off on first booking and you get ₹10 instantly in your Madadwala wallet!"
                    )
                }
            }
        }
    }
}

@Composable
fun HowItWorksItem(number: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = MadadwalaColors.Green.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = MadadwalaColors.Green,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
