package com.abhi.madadwala_1.ui.screens.provider

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhi.madadwala_1.ui.components.StepProgressBar
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.abhi.madadwala_1.ui.viewmodel.SignupViewModel
import com.abhi.madadwala_1.ui.viewmodel.SignupState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSignupScreen(
    uid: String,
    phoneNumber: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val signupViewModel: SignupViewModel = viewModel()
    val signupState by signupViewModel.signupState.collectAsState()
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    // Data State to persist across steps
    var selectedCategory by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf<String?>(null) }
    
    var aadhaarNumber by remember { mutableStateOf("") }
    var aadhaarFrontUri by remember { mutableStateOf<Uri?>(null) }
    var aadhaarBackUri by remember { mutableStateOf<Uri?>(null) }
    
    var selfieUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Provider Registration") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentStep > 1) currentStep-- else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MadadwalaColors.Teal,
                        navigationIconContentColor = MadadwalaColors.Teal
                    )
                )
                StepProgressBar(currentStep = currentStep, totalSteps = totalSteps)
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(500)) { width -> width } + fadeIn(animationSpec = tween(500))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(500)) { width -> -width } + fadeOut(animationSpec = tween(500)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(500)) { width -> -width } + fadeIn(animationSpec = tween(500))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(500)) { width -> width } + fadeOut(animationSpec = tween(500)))
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> ProviderStep1(
                        phoneNumber = phoneNumber,
                        onNext = { cat, n, e, exp, addr, ref ->
                            selectedCategory = cat
                            name = n
                            email = e
                            experience = exp
                            address = addr
                            referralCode = ref
                            currentStep = 2
                        }
                    )
                    2 -> ProviderStep2(
                        onNext = { no, front, back ->
                            aadhaarNumber = no
                            aadhaarFrontUri = front
                            aadhaarBackUri = back
                            currentStep = 3
                        }
                    )
                    3 -> ProviderStep3(
                        onNext = { uri ->
                            selfieUri = uri
                            currentStep = 4
                        }
                    )
                    4 -> ProviderStep4(
                        data = mapOf(
                            "Category" to selectedCategory,
                            "Name" to name,
                            "Email" to email,
                            "Phone" to "+91 $phoneNumber",
                            "Experience" to experience,
                            "Address" to address,
                            "Aadhaar" to aadhaarNumber
                        ),
                        onComplete = {
                            signupViewModel.register(
                                context = context,
                                uid = uid,
                                phoneNumber = phoneNumber,
                                role = "provider",
                                name = name,
                                email = email,
                                category = selectedCategory,
                                profession = experience, // Use entered experience string as profession/bio
                                aadhaarNumber = aadhaarNumber,
                                referralCode = referralCode,
                                profileImageUri = selfieUri,
                                aadhaarImageUri = aadhaarFrontUri
                            )
                        },
                        isLoading = signupState is SignupState.Loading
                    )
                }
            }
        }
    }

    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
            onRegistrationSuccess()
        }
    }
}
