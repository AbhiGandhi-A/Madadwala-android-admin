package com.abhi.madadwala_1.ui.screens.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File
import coil.compose.AsyncImage

@Composable
fun ProviderStep3(
    onNext: (selfieUri: Uri) -> Unit
) {
    val context = LocalContext.current
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selfieUri = tempUri
        }
    }

    fun getTempUri(): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile("selfie_", ".jpg", directory)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live Identity Selfie",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Please take a clear selfie to verify your identity. Ensure your face is well lit.",
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MadadwalaColors.Cream),
            contentAlignment = Alignment.Center
        ) {
            if (selfieUri != null) {
                AsyncImage(
                    model = selfieUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                IconButton(onClick = { 
                    val uri = getTempUri()
                    tempUri = uri
                    launcher.launch(uri)
                }, modifier = Modifier.size(100.dp)) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take Selfie",
                        tint = MadadwalaColors.Teal,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        
        if (selfieUri != null) {
            Text(
                text = "Selfie captured successfully!",
                color = MadadwalaColors.Green,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Next: Review Details",
            onClick = { selfieUri?.let { onNext(it) } },
            enabled = selfieUri != null
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
