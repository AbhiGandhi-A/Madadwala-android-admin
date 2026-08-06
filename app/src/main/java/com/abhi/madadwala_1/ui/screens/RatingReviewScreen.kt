package com.abhi.madadwala_1.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.ReviewRequest
import com.abhi.madadwala_1.ui.components.FormTextField
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import kotlinx.coroutines.launch

@Composable
fun RatingReviewScreen(
    bookingId: String,
    providerUid: String,
    onComplete: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var providerName by remember { mutableStateOf("the Partner") }
    val scope = rememberCoroutineScope()
    
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(providerUid) {
        try {
            val response = RetrofitClient.apiService.getProviderDetails(providerUid)
            if (response.isSuccessful) {
                providerName = response.body()?.provider?.name ?: "the Partner"
            }
        } catch (e: Exception) {}
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("How was your service?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Rate your experience with $providerName", color = MadadwalaColors.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { index ->
                val starIndex = index + 1
                val isSelected = starIndex <= rating
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "StarScale"
                )

                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFFFB400) else MadadwalaColors.LightGray,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                        .clickable { rating = starIndex }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        FormTextField(
            value = comment,
            onValueChange = { comment = it },
            label = "Tell us more (Optional)",
            modifier = Modifier.height(150.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Submit Review",
            onClick = {
                scope.launch {
                    isSubmitting = true
                    try {
                        val response = RetrofitClient.apiService.submitReview(
                            ReviewRequest(
                                bookingId = bookingId,
                                providerUid = providerUid,
                                customerUid = user?.uid ?: "",
                                customerName = user?.name ?: "User",
                                rating = rating,
                                comment = comment
                            )
                        )
                        if (response.isSuccessful) {
                            onComplete()
                        } else {
                            // Even if it fails, we move on for better UX, but maybe log it
                            onComplete()
                        }
                    } catch (e: Exception) {
                        onComplete()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            enabled = rating > 0,
            isLoading = isSubmitting
        )
        
        TextButton(onClick = onComplete) {
            Text("Skip", color = MadadwalaColors.Gray)
        }
    }
}
