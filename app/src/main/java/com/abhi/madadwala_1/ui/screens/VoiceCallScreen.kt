package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhi.madadwala_1.ui.viewmodel.CallState
import com.abhi.madadwala_1.ui.viewmodel.CallViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// Color Palette for Premium Mint Theme
object CallColors {
    val PrimaryGreen = Color(0xFF2E7D4F)
    val SecondaryGreen = Color(0xFF43A047)
    val LightGreen = Color(0xFFE8F5EC)
    val BackgroundPaleMint = Color(0xFFF3FAF6)
    val IvoryMint = Color(0xFFF2F7F2)
    val SageMist = Color(0xFFE8F4EA)
    val CardBackground = Color(0xFFF7FCF9)
    val IconCircleBackground = Color(0xFFDFF3E5)
    val DividerColor = Color(0xFFD7E9DD)
    val CharcoalBlack = Color(0xFF1E1E1E)
    val PrimaryText = Color(0xFF1E1E1E)
    val SecondaryText = Color(0xFF757575)
    val CharcoalGray = Color(0xFF333333)
    val SlateGray = Color(0xFF5F6368)
    val AcceptButton = Color(0xFF43A047)
    val DeclineButton = Color(0xFFEF5350)
    val CoralRed = Color(0xFFFF3B30)
    val White = Color(0xFFFFFFFF)
    val WaveAnimationColor = Color(0xFFBFE6C9)
    val SecureBadgeBg = Color(0xFFDFF5E4)
    val RedGlow = Color(0xFFFFD9D6)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceCallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val context = LocalContext.current
    
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
        viewModel.initWebRTC(context)
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Ended) {
            kotlinx.coroutines.delay(1000)
            onDismiss()
        }
    }

    val backgroundColor = if (callState is CallState.Incoming) CallColors.BackgroundPaleMint else CallColors.IvoryMint

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Decorative Background Accents
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = CallColors.SageMist,
                    radius = size.width,
                    center = Offset(size.width / 2, size.height * 1.2f)
                )
                drawCircle(
                    color = CallColors.SageMist.copy(alpha = 0.5f),
                    radius = size.width * 0.4f,
                    center = Offset(0f, 0f)
                )
            }
        }

        when (val state = callState) {
            is CallState.Outgoing -> {
                CallUI(
                    name = state.partnerName,
                    status = "Calling...",
                    isSpeakerOn = isSpeakerOn,
                    isMuted = isMuted,
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleMic = { viewModel.toggleMic() },
                    onEndCall = { viewModel.endCall(state.callId) }
                )
            }
            is CallState.Ringing -> {
                CallUI(
                    name = state.partnerName,
                    status = "Ringing...",
                    isSpeakerOn = isSpeakerOn,
                    isMuted = isMuted,
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleMic = { viewModel.toggleMic() },
                    onEndCall = { viewModel.endCall(state.callId) }
                )
            }
            is CallState.Incoming -> {
                IncomingCallUI(
                    name = state.customerName,
                    image = state.customerImage,
                    serviceName = state.serviceName,
                    location = state.location,
                    bookingTime = state.bookingTime,
                    onAccept = { viewModel.acceptCall(state.callId) },
                    onDecline = { viewModel.endCall(state.callId) }
                )
            }
            is CallState.Connected -> {
                CallUI(
                    name = state.otherPartyName,
                    status = formatDuration(state.duration),
                    isSpeakerOn = isSpeakerOn,
                    isMuted = isMuted,
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleMic = { viewModel.toggleMic() },
                    onEndCall = { viewModel.endCall(state.callId) },
                    isConnected = true
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CallUI(
    name: String,
    status: String,
    isSpeakerOn: Boolean,
    isMuted: Boolean,
    onToggleSpeaker: () -> Unit,
    onToggleMic: () -> Unit,
    onEndCall: () -> Unit,
    isConnected: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 80.dp)
        ) {
            // Profile Section with Waves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaveLines(CallColors.WaveAnimationColor)
                    Spacer(modifier = Modifier.width(140.dp))
                    WaveLines(CallColors.WaveAnimationColor)
                }

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(CallColors.White)
                        .border(1.dp, CallColors.WaveAnimationColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CallColors.PrimaryGreen,
                        modifier = Modifier.size(65.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = name,
                color = CallColors.PrimaryGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                color = CallColors.SlateGray,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secure Badge
            Surface(
                color = CallColors.SecureBadgeBg,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        tint = CallColors.SecondaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calling securely...",
                        color = CallColors.SecondaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallActionBtn(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = isMuted,
                onClick = onToggleMic
            )
            CallActionBtn(
                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                label = "Speaker",
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )
            CallActionBtn(icon = Icons.Default.Bluetooth, label = "Bluetooth", onClick = {})
            
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(CallColors.RedGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = CallColors.CoralRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IncomingCallUI(
    name: String,
    image: String?,
    serviceName: String?,
    location: String?,
    bookingTime: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaveLines(Color(0xFFB7E4C7))
                Spacer(modifier = Modifier.width(140.dp))
                WaveLines(Color(0xFFB7E4C7))
            }

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, Color(0xFFB7E4C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CallColors.PrimaryGreen,
                        modifier = Modifier.size(70.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = name,
            color = CallColors.PrimaryGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Incoming Call",
            color = CallColors.SecondaryGreen,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CallColors.CardBackground),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow(icon = Icons.Default.Work, label = "Service", value = serviceName ?: "Plumbing")
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CallColors.DividerColor)
                DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = location ?: "Ankleshwar, Gujarat")
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CallColors.DividerColor)
                DetailRow(icon = Icons.Default.AccessTime, label = "Booking Time", value = bookingTime ?: "Today, 04:00 PM")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CallActionItem(
                icon = Icons.Default.CallEnd,
                label = "Decline",
                subLabel = "Reject the call",
                color = CallColors.DeclineButton,
                glowColor = Color(0xFFFFE3E3),
                onClick = onDecline
            )

            CallActionItem(
                icon = Icons.Default.Call,
                label = "Accept",
                subLabel = "Answer the call",
                color = CallColors.AcceptButton,
                glowColor = Color(0xFFD8F3DC),
                onClick = onAccept
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun WaveLines(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val heights = listOf(10.dp, 25.dp, 40.dp, 25.dp, 10.dp)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CallColors.IconCircleBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CallColors.PrimaryGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = CallColors.SecondaryText, fontSize = 12.sp)
            Text(text = value, color = CallColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CallActionItem(
    icon: ImageVector,
    label: String,
    subLabel: String,
    color: Color,
    glowColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .background(glowColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = color,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(68.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = label, color = CallColors.PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = subLabel, color = CallColors.SecondaryText, fontSize = 12.sp)
    }
}

@Composable
fun CallActionBtn(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(if (isActive) CallColors.PrimaryGreen else CallColors.White, CircleShape)
                .border(1.dp, CallColors.WaveAnimationColor.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = if (isActive) Color.White else CallColors.PrimaryGreen, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = CallColors.CharcoalGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
