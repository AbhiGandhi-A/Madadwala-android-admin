package com.abhi.madadwala_1.ui.screens

import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhi.madadwala_1.ui.components.FormTextField
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.SignupState
import com.abhi.madadwala_1.ui.viewmodel.SignupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSignupScreen(
    uid: String,
    phoneNumber: String,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val signupViewModel: SignupViewModel = viewModel()
    val signupState by signupViewModel.signupState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            profileImageUri = tempUri
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

    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create your account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MadadwalaColors.Teal,
                    navigationIconContentColor = MadadwalaColors.Teal
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo Picker Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MadadwalaColors.Cream)
                    .clickable { 
                        val uri = getTempUri()
                        tempUri = uri
                        cameraLauncher.launch(uri)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photo",
                        tint = MadadwalaColors.Teal,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Text(
                text = "Add profile photo",
                style = MaterialTheme.typography.bodySmall.copy(color = MadadwalaColors.Teal, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            FormTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                errorText = if (name.isNotEmpty() && name.length < 3) "Name is too short" else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            FormTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address (Optional)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            FormTextField(
                value = "+91 $phoneNumber",
                onValueChange = { },
                label = "Phone Number",
                readOnly = true
            )

            Spacer(modifier = Modifier.height(48.dp))

            PrimaryButton(
                text = "Create account",
                onClick = {
                    signupViewModel.register(
                        context = context,
                        uid = uid,
                        phoneNumber = phoneNumber,
                        role = "customer",
                        name = name,
                        email = if (email.isEmpty()) null else email,
                        category = null,
                        profileImageUri = profileImageUri,
                        aadhaarImageUri = null
                    )
                },
                isLoading = signupState is SignupState.Loading,
                enabled = name.length >= 3 && signupState !is SignupState.Loading
            )

            if (signupState is SignupState.Error) {
                Text(
                    text = (signupState as SignupState.Error).message,
                    color = MadadwalaColors.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
