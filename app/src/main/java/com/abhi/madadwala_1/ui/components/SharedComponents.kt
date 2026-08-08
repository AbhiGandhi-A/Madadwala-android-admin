package com.abhi.madadwala_1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(),
        label = "ButtonScale"
    )

    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MadadwalaColors.Teal,
            contentColor = MadadwalaColors.White,
            disabledContainerColor = MadadwalaColors.Teal.copy(alpha = 0.5f),
            disabledContentColor = MadadwalaColors.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MadadwalaColors.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = if (text.length > 18) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(),
        label = "ButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(14.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MadadwalaColors.Teal)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MadadwalaColors.Teal
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MadadwalaColors.Teal
            )
        )
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = errorText != null,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            readOnly = readOnly,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MadadwalaColors.Ink,
                unfocusedTextColor = MadadwalaColors.Ink,
                focusedBorderColor = MadadwalaColors.Teal,
                unfocusedBorderColor = MadadwalaColors.LightGray,
                errorBorderColor = MadadwalaColors.Red,
                focusedLabelColor = MadadwalaColors.Teal,
                unfocusedLabelColor = MadadwalaColors.Gray,
                cursorColor = MadadwalaColors.Teal
            )
        )
        if (errorText != null) {
            Text(
                text = errorText,
                color = MadadwalaColors.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        StepInfo("Describe", Icons.AutoMirrored.Filled.Assignment),
        StepInfo("Location", Icons.Default.LocationOn),
        StepInfo("Schedule", Icons.Default.CalendarToday),
        StepInfo("Confirm", Icons.Default.CheckCircle)
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .background(Color.White),
        verticalAlignment = Alignment.Top
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep
            val isUpcoming = i > currentStep
            val step = steps[i - 1]
            
            // Step Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isCurrent) Modifier
                            .background(MadadwalaColors.Teal.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp)
                        else Modifier.padding(vertical = 8.dp)
                    )
            ) {
                // Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted || isCurrent) MadadwalaColors.Teal 
                            else MadadwalaColors.LightGray.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = i.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isCompleted || isCurrent) Color.White else MadadwalaColors.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Icon
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isCompleted || isCurrent) MadadwalaColors.Teal else MadadwalaColors.Gray.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Label
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCompleted || isCurrent) MadadwalaColors.Teal else MadadwalaColors.Gray,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }

            // Connecting Line
            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .width(20.dp) // Small fixed width for line
                        .padding(top = 24.dp) // Align with circle center (32/2 + padding)
                        .height(1.dp)
                        .background(if (i < currentStep) MadadwalaColors.Teal else MadadwalaColors.Gray.copy(alpha = 0.3f))
                )
            }
        }
    }
}

data class StepInfo(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
