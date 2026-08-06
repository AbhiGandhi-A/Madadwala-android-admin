package com.abhi.madadwala_1.ui.screens.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhi.madadwala_1.ui.components.DocumentUploadCard
import com.abhi.madadwala_1.ui.components.FormTextField
import com.abhi.madadwala_1.ui.components.PrimaryButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ProviderStep2(
    onNext: (aadhaarNo: String, frontUri: Uri, backUri: Uri) -> Unit
) {
    var aadhaarNumber by remember { mutableStateOf("") }
    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var backUri by remember { mutableStateOf<Uri?>(null) }

    val frontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { frontUri = it }
    val backLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { backUri = it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "KYC: Aadhaar Verification",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))

        FormTextField(
            value = aadhaarNumber,
            onValueChange = { if (it.length <= 12) aadhaarNumber = it },
            label = "Aadhaar Card Number",
            errorText = if (aadhaarNumber.isNotEmpty() && aadhaarNumber.length < 12) "Enter 12 digits" else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        DocumentUploadCard(
            title = "Aadhaar Front Image",
            description = "Clear photo of the front side",
            hasImage = frontUri != null,
            onClick = { frontLauncher.launch("image/*") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DocumentUploadCard(
            title = "Aadhaar Back Image",
            description = "Clear photo of the back side",
            hasImage = backUri != null,
            onClick = { backLauncher.launch("image/*") }
        )

        Spacer(modifier = Modifier.height(48.dp))

        PrimaryButton(
            text = "Next: Identity Selfie",
            onClick = { 
                if (frontUri != null && backUri != null) {
                    onNext(aadhaarNumber, frontUri!!, backUri!!)
                }
            },
            enabled = aadhaarNumber.length == 12 && frontUri != null && backUri != null
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
