package com.abhi.madadwala_1.ui.screens.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.ui.components.FormTextField
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

data class ServiceCategory(
    val id: String,
    val name: String,
    val icon: ImageVector
)

@Composable
fun ProviderStep1(
    phoneNumber: String,
    onNext: (category: String, name: String, email: String, experience: String, address: String) -> Unit
) {
    val categories = listOf(
        ServiceCategory("1", "Cleaning", Icons.Default.CleaningServices),
        ServiceCategory("2", "Plumbing", Icons.Default.Build),
        ServiceCategory("3", "Electrical", Icons.Default.Bolt),
        ServiceCategory("4", "Painting", Icons.Default.ColorLens),
        ServiceCategory("5", "Carpentry", Icons.Default.Carpenter),
        ServiceCategory("0", "Other", Icons.Default.MoreHoriz)
    )

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Service & Personal Details",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Select Category", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(categories) { category ->
                CompactCategoryCard(
                    category = category,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FormTextField(value = name, onValueChange = { name = it }, label = "Full Name")
        Spacer(modifier = Modifier.height(16.dp))
        
        FormTextField(value = email, onValueChange = { email = it }, label = "Email Address")
        Spacer(modifier = Modifier.height(16.dp))
        
        FormTextField(value = "+91 $phoneNumber", onValueChange = {}, label = "Phone Number", readOnly = true)
        Spacer(modifier = Modifier.height(16.dp))
        
        FormTextField(value = experience, onValueChange = { experience = it }, label = "Experience (e.g. 5 Years)")
        Spacer(modifier = Modifier.height(16.dp))
        
        FormTextField(
            value = address, 
            onValueChange = { address = it }, 
            label = "Service Address / City",
            modifier = Modifier.height(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Next: KYC Verification",
            onClick = { 
                val categoryName = categories.find { it.id == selectedCategoryId }?.name ?: ""
                onNext(categoryName, name, email, experience, address) 
            },
            enabled = selectedCategoryId != null && name.isNotBlank() && experience.isNotBlank() && address.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CompactCategoryCard(
    category: ServiceCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MadadwalaColors.Teal else MadadwalaColors.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) MadadwalaColors.Teal.copy(alpha = 0.05f) else Color.White
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) MadadwalaColors.Teal else MadadwalaColors.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSelected) MadadwalaColors.Teal else MadadwalaColors.Ink
                )
            )
        }
    }
}
