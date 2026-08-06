package com.abhi.madadwala_1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@Composable
fun DocumentUploadCard(
    title: String,
    description: String,
    hasImage: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stroke = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasImage) MadadwalaColors.Teal.copy(alpha = 0.05f) else Color.Transparent)
            .clickable { onClick() }
            .then(
                if (!hasImage) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MadadwalaColors.LightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MadadwalaColors.Teal,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hasImage) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MadadwalaColors.Teal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Document Uploaded",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MadadwalaColors.Teal,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Document",
                    tint = MadadwalaColors.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.Ink
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MadadwalaColors.Gray
                    )
                )
            }
        }
    }
}
