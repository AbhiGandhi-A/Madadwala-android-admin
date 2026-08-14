package com.abhi.madadwala_1.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.BookingMessageResponse
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.ProviderResponse
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.BookingChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingChatBottomSheet(
    bookingId: String,
    onDismiss: () -> Unit,
    onViewBookingDetails: (String) -> Unit = {},
    onViewPartnerProfile: (String) -> Unit = {},
    chatViewModel: BookingChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val bookingDetails by chatViewModel.bookingDetails.collectAsState()
    val partnerProfile by chatViewModel.partnerProfile.collectAsState()
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            chatViewModel.sendMessage(bookingId, "", it, context)
        }
    }

    LaunchedEffect(bookingId) {
        chatViewModel.initChat(bookingId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF7F7F7),
        dragHandle = null
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    bookingDetails = bookingDetails,
                    partnerProfile = partnerProfile,
                    currentUserUid = currentUserUid,
                    onBack = onDismiss
                )
            },
            bottomBar = {
                ChatInputBar(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(bookingId, messageText)
                            messageText = ""
                        }
                    },
                    onAttachClick = { imagePickerLauncher.launch("image/*") }
                )
            },
            containerColor = Color(0xFFF7F7F7)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                bookingDetails?.let {
                    val isProvider = currentUserUid == it.providerUid
                    BookingInfoCard(
                        booking = it,
                        isProvider = isProvider,
                        onViewDetails = { onViewBookingDetails(it._id) },
                        onViewProfile = { it.providerUid.let { uid -> onViewPartnerProfile(uid) } }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading && messages.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MadadwalaColors.Green
                        )
                    } else if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No messages yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Start a conversation", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                    } else {
                        val groupedMessages = groupMessagesByDate(messages)
                        
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            groupedMessages.forEach { (date, msgs) ->
                                item {
                                    DateSeparator(date)
                                }
                                items(msgs) { msg ->
                                    val isMe = msg.senderUid == currentUserUid
                                    ChatBubble(msg, isMe, bookingDetails)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(
    bookingDetails: BookingResponse?,
    partnerProfile: ProviderResponse?,
    currentUserUid: String,
    onBack: () -> Unit
) {
    val isProvider = currentUserUid == bookingDetails?.providerUid
    val chatPartnerName = if (isProvider) bookingDetails?.customerName else bookingDetails?.providerName
    val chatPartnerImage = if (isProvider) bookingDetails?.customerImage else bookingDetails?.providerImage

    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Green)
            }
            
            AsyncImage(
                model = chatPartnerImage ?: "https://via.placeholder.com/150",
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chatPartnerName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    val isShowingProvider = !isProvider
                    if (isShowingProvider && (partnerProfile?.isVerified == true || bookingDetails?.providerName != null)) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val isTrulyOnline = if (!isProvider) partnerProfile?.isAvailable == true && partnerProfile?.isOnline == true else false
                if (!isProvider) {
                    Text(
                        text = if (isTrulyOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTrulyOnline) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
            
            IconButton(onClick = { /* Call logic handled by caller */ }) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = MadadwalaColors.Green)
            }
        }
    }
}

@Composable
fun BookingInfoCard(
    booking: BookingResponse,
    isProvider: Boolean,
    onViewDetails: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9)
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = MadadwalaColors.Green,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = booking.serviceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Booking ID: #${booking._id.takeLast(7).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                Surface(
                    color = Color(0xFFF1F8E9),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = booking.status.replace("_", " ").uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(18.dp), tint = MadadwalaColors.Green)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Booking Details", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
                
                if (!isProvider) {
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEEEEEE)))
                    TextButton(
                        onClick = onViewProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = MadadwalaColors.Green)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Partner Profile", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: BookingMessageResponse, isMe: Boolean, bookingDetails: BookingResponse?) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(msg.timestamp)
        timeFormat.format(date!!)
    } catch (e: Exception) {
        ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            val senderImage = if (msg.senderUid == bookingDetails?.providerUid) {
                bookingDetails?.providerImage
            } else {
                bookingDetails?.customerImage
            }
            AsyncImage(
                model = senderImage ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isMe) MadadwalaColors.Green else Color.White,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 0.dp,
                    bottomEnd = if (isMe) 0.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (!msg.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = msg.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (msg.message.isNotEmpty()) {
                        Text(
                            text = msg.message,
                            color = if (isMe) Color.White else Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFFEEEEEE),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF9F9F9)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = onAttachClick) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray)
                    }
                    
                    TextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", fontSize = 14.sp, color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    
                    IconButton(onClick = { /* Emoji */ }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = Color.Gray)
                    }
                }
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MadadwalaColors.Green
            ) {
                IconButton(onClick = onSend) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun groupMessagesByDate(messages: List<BookingMessageResponse>): Map<String, List<BookingMessageResponse>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val today = outputFormat.format(Date())
    
    return messages.groupBy { msg ->
        try {
            val date = sdf.parse(msg.timestamp)
            val formatted = outputFormat.format(date!!)
            if (formatted == today) "Today" else formatted
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
