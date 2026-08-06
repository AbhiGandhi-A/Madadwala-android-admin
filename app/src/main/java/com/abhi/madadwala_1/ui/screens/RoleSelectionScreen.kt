package com.abhi.madadwala_1.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    var selectedRole by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MadadwalaColors.Cream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "How do you want to use madadwala?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        RoleCard(
            title = "I need a service",
            description = "Find and book local professionals",
            icon = Icons.Default.Person,
            isSelected = selectedRole == "customer",
            onClick = { selectedRole = "customer" }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleCard(
            title = "I offer a service",
            description = "Join as a partner and grow your business",
            icon = Icons.Default.Engineering,
            isSelected = selectedRole == "provider",
            onClick = { selectedRole = "provider" }
        )

        Spacer(modifier = Modifier.height(64.dp))

        PrimaryButton(
            text = "Continue",
            onClick = { selectedRole?.let { onRoleSelected(it) } },
            enabled = selectedRole != null
        )
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MadadwalaColors.Teal else MadadwalaColors.LightGray,
        label = "BorderColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        label = "CardScale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MadadwalaColors.Teal.copy(alpha = 0.05f) else Color.White,
        label = "BackgroundColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isSelected) MadadwalaColors.Teal else MadadwalaColors.Cream,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MadadwalaColors.Teal,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MadadwalaColors.Gray
                    )
                )
            }
        }
    }
}
