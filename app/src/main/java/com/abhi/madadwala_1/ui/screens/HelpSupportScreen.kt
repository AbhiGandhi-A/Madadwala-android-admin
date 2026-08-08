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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Help & Support", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MadadwalaColors.Cream
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = MadadwalaColors.Green,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MadadwalaColors.Green
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("FAQs", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Chat Support", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (selectedTabIndex == 0) {
                FAQContent()
            } else {
                ChatSupportContent(
                    supportViewModel = supportViewModel,
                    user = user,
                    messages = messages,
                    supportState = supportState,
                    listState = listState
                )
            }
        }
    }
}

@Composable
fun FAQContent() {
    val faqs = listOf(
        FAQItem("How to book a service?", "You can book a service by selecting a category from the home screen, choosing a provider, and following the 4-step booking flow."),
        FAQItem("What are the payment methods?", "We support Online Payment (UPI, Cards, NetBanking), Wallet, and Cash after service."),
        FAQItem("How can I cancel my booking?", "Go to your active booking details and click on the 'Cancel' button. Refunds for paid bookings are processed to your wallet instantly."),
        FAQItem("How do I contact the provider?", "Once a booking is accepted, you can use the 'Call' or 'Chat' buttons in the booking details to reach the provider."),
        FAQItem("Is Madadwala safe?", "Yes, all our service providers are background-verified and we provide 24/7 support for any concerns."),
        FAQItem("How to add money to wallet?", "Go to the 'Wallet' tab and click 'Add Money'. You can use UPI or any other online method to top up your wallet."),
        FAQItem("What if the work is not done properly?", "You can raise a concern via the 'Chat Support' tab or report the provider from their profile. We'll ensure it's resolved.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MadadwalaColors.Green,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(faqs) { faq ->
            FAQExpandableCard(faq)
        }
    }
}

@Composable
fun FAQExpandableCard(faq: FAQItem) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MadadwalaColors.Ink,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MadadwalaColors.Green
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MadadwalaColors.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

data class FAQItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSupportContent(
    supportViewModel: SupportViewModel,
    user: com.abhi.madadwala_1.data.remote.UserResponse?,
    messages: List<SupportMessage>,
    supportState: SupportState,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    var messageText by remember { mutableStateOf("") }
    var currentBotOptions by remember { mutableStateOf(getInitialBotQuestions()) }
    var botBreadcrumb by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Support Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MadadwalaColors.Green.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SupportAgent, null, tint = MadadwalaColors.Green)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Executive Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        when(supportState) {
                            SupportState.Bot -> "Self Help Bot"
                            SupportState.Waiting -> "Connecting to Agent..."
                            SupportState.Active -> "Agent Online"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (supportState == SupportState.Active) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
        }

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
                            text = if (botBreadcrumb.isEmpty()) "Quick Help" else "Related to ${botBreadcrumb.last()}:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MadadwalaColors.Green
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        currentBotOptions.forEach { option ->
                            Button(
                                onClick = {
                                    user?.let { u ->
                                        if (option.label == "Raise a Ticket" || option.label == "Chat with agent") {
                                            supportViewModel.requestAgent(u.uid)
                                        } else if (option.subOptions.isNotEmpty()) {
                                            supportViewModel.sendMessage(u.uid, u.name ?: "User", option.label)
                                            currentBotOptions = option.subOptions
                                            botBreadcrumb = botBreadcrumb + option.label
                                        } else {
                                            supportViewModel.sendMessage(u.uid, u.name ?: "User", option.label)
                                            currentBotOptions = listOf(
                                                BotOption("Raise a Ticket"),
                                                BotOption("Go back", isBack = true)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.85f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MadadwalaColors.Green
                                ),
                                border = BorderStroke(1.dp, MadadwalaColors.Green.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MadadwalaColors.Green
                                )
                            }
                        }
                        
                        if (botBreadcrumb.isNotEmpty()) {
                            TextButton(onClick = {
                                currentBotOptions = getInitialBotQuestions()
                                botBreadcrumb = emptyList()
                            }) {
                                Text("Back to main menu", color = Color.Gray, fontSize = 12.sp)
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
                    placeholder = { Text("Describe your issue...") },
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
        BotOption("Raise a Ticket")
    )),
    BotOption("Payment & Wallet", listOf(
        BotOption("Money not added to wallet"),
        BotOption("Double payment done"),
        BotOption("Refund status"),
        BotOption("Raise a Ticket")
    )),
    BotOption("Service Issues", listOf(
        BotOption("Work not done properly"),
        BotOption("Provider was rude"),
        BotOption("Safety concern"),
        BotOption("Raise a Ticket")
    )),
    BotOption("Something else", listOf(
        BotOption("Raise a Ticket")
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
