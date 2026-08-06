package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.SupportMessage
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import com.abhi.madadwala_1.ui.viewmodel.SupportViewModel
import com.abhi.madadwala_1.ui.viewmodel.SupportState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    val supportViewModel: SupportViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user
    
    val messages by supportViewModel.messages.collectAsState()
    val isExecutiveJoined by supportViewModel.isExecutiveJoined.collectAsState()
    val supportState by supportViewModel.supportState.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Bot Question State
    var currentBotOptions by remember { mutableStateOf(getInitialBotQuestions()) }
    var botBreadcrumb by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let {
            supportViewModel.loadMessages(it)
            supportViewModel.startPolling(it)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MadadwalaColors.Green.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, null, tint = MadadwalaColors.Green)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Support Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                when(supportState) {
                                    SupportState.Bot -> "Self Help"
                                    SupportState.Waiting -> "Waiting for Agent..."
                                    SupportState.Active -> "Agent Online"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (supportState == SupportState.Active) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MadadwalaColors.Cream
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderUid == user?.uid
                    ChatBubble(message = message, isMe = isMe)
                }

                if (supportState == SupportState.Bot) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (botBreadcrumb.isEmpty()) "How can we help you?" else "Related to ${botBreadcrumb.last()}:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MadadwalaColors.Green
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            currentBotOptions.forEach { option ->
                                Button(
                                    onClick = {
                                        user?.let { u ->
                                            if (option.label == "Chat with agent") {
                                                supportViewModel.requestAgent(u.uid)
                                            } else if (option.subOptions.isNotEmpty()) {
                                                supportViewModel.sendMessage(u.uid, u.name ?: "User", option.label)
                                                currentBotOptions = option.subOptions
                                                botBreadcrumb = botBreadcrumb + option.label
                                            } else {
                                                supportViewModel.sendMessage(u.uid, u.name ?: "User", option.label)
                                                currentBotOptions = listOf(
                                                    BotOption("Chat with agent"),
                                                    BotOption("Go back", isBack = true)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MadadwalaColors.Green.copy(alpha = 0.12f),
                                        contentColor = MadadwalaColors.Green
                                    ),
                                    border = BorderStroke(1.5.dp, MadadwalaColors.Green)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = MadadwalaColors.Green,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                            
                            if (botBreadcrumb.isNotEmpty()) {
                                TextButton(onClick = {
                                    currentBotOptions = getInitialBotQuestions()
                                    botBreadcrumb = emptyList()
                                }) {
                                    Text("Back to main menu", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(tonalElevation = 8.dp, color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type your query...") },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MadadwalaColors.LightGray.copy(alpha = 0.3f),
                            unfocusedContainerColor = MadadwalaColors.LightGray.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                user?.let { u ->
                                    supportViewModel.sendMessage(u.uid, u.name ?: "User", messageText)
                                    if (supportState == SupportState.Bot) supportViewModel.requestAgent(u.uid)
                                    messageText = ""
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp).background(MadadwalaColors.Green, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

data class BotOption(
    val label: String,
    val subOptions: List<BotOption> = emptyList(),
    val isBack: Boolean = false
)

fun getInitialBotQuestions() = listOf(
    BotOption("Where is my booking?", listOf(
        BotOption("Tracking is not moving"),
        BotOption("Provider is late"),
        BotOption("Cancel my booking"),
        BotOption("Chat with agent")
    )),
    BotOption("Payment & Wallet", listOf(
        BotOption("Money not added to wallet"),
        BotOption("Double payment done"),
        BotOption("Refund status"),
        BotOption("Chat with agent")
    )),
    BotOption("Service Issues", listOf(
        BotOption("Work not done properly"),
        BotOption("Provider was rude"),
        BotOption("Safety concern"),
        BotOption("Chat with agent")
    )),
    BotOption("Something else", listOf(
        BotOption("Chat with agent")
    ))
)

@Composable
fun ChatBubble(message: SupportMessage, isMe: Boolean) {
    val isSystem = message.senderUid == "system"
    
    val displayTime = try {
        if (message.timestamp != null) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(message.timestamp)
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
        } else {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        }
    } catch (e: Exception) {
        ""
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSystem) Alignment.CenterHorizontally else if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isSystem) Color.LightGray.copy(0.2f) else if (isMe) MadadwalaColors.Green else Color.White,
            shape = if (isSystem) RoundedCornerShape(8.dp) else RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = if (isSystem) 320.dp else 280.dp)
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isSystem) 6.dp else 10.dp),
                color = if (isMe) Color.White else if (isSystem) Color.Gray else Color.Black,
                style = if (isSystem) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                textAlign = if (isSystem) androidx.compose.ui.text.style.TextAlign.Center else null
            )
        }
        if (!isSystem && displayTime.isNotEmpty()) {
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
