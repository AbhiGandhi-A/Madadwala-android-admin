package com.abhi.madadwala_1.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.abhi.madadwala_1.ui.screens.*
import com.abhi.madadwala_1.ui.screens.ProviderProfileScreen
import com.abhi.madadwala_1.ui.screens.provider.*
import com.abhi.madadwala_1.ui.screens.AdminDashboardScreen
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.abhi.madadwala_1.utils.AnalyticsHelper
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }
    object RoleSelection : Screen("role_selection")
    object CustomerSignup : Screen("customer_signup/{uid}/{phone}") {
        fun createRoute(uid: String, phone: String) = "customer_signup/$uid/$phone"
    }
    object ProviderSignup : Screen("provider_signup/{uid}/{phone}") {
        fun createRoute(uid: String, phone: String) = "provider_signup/$uid/$phone"
    }
    object VerificationPending : Screen("verification_pending")
    object ProviderDashboard : Screen("provider_dashboard")
    object ProviderActiveJob : Screen("provider_active_job/{bookingId}") {
        fun createRoute(bookingId: String) = "provider_active_job/$bookingId"
    }
    object AdminDashboard : Screen("admin_dashboard")
    object Home : Screen("home")
    object CategoryListing : Screen("category_listing/{category}") {
        fun createRoute(category: String) = "category_listing/$category"
    }
    object ProviderProfile : Screen("provider_profile/{uid}") {
        fun createRoute(uid: String) = "provider_profile/$uid"
    }
    object BookingFlow : Screen("booking_flow/{providerUid}/{serviceName}/{price}") {
        fun createRoute(providerUid: String, serviceName: String, price: Double) = "booking_flow/$providerUid/$serviceName/$price"
    }
    object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }
    object WorkCompleted : Screen("work_completed/{bookingId}") {
        fun createRoute(bookingId: String) = "work_completed/$bookingId"
    }
    object LiveTracking : Screen("live_tracking/{bookingId}") {
        fun createRoute(bookingId: String) = "live_tracking/$bookingId"
    }
    object Payment : Screen("payment/{bookingId}") {
        fun createRoute(bookingId: String) = "payment/$bookingId"
    }
    object RatingReview : Screen("rating_review/{bookingId}/{providerUid}") {
        fun createRoute(bookingId: String, providerUid: String) = "rating_review/$bookingId/$providerUid"
    }
    object Notifications : Screen("notifications")
    object Offers : Screen("offers")
    object Favorites : Screen("favorites")
    object HelpSupport : Screen("help_support")
    object AllCategories : Screen("all_categories")
    object InstantCategories : Screen("instant_categories")
    object AddMoney : Screen("add_money")
    object TransactionHistory : Screen("transaction_history")
    object SendMoney : Screen("send_money")
    object VoiceCall : Screen("voice_call")
    object InviteEarn : Screen("invite_earn")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val callViewModel: com.abhi.madadwala_1.ui.viewmodel.CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
    val authState by authViewModel.authState.collectAsState()
    val callState by callViewModel.callState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val analyticsHelper = remember { AnalyticsHelper(context) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val route = backStackEntry.destination.route
            if (route != null) {
                analyticsHelper.logScreenView(route, "ComposeView")
            }
        }
    }

    // Handle Intent-based navigation (Deep links / Notifications)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            val activity = context as? Activity
            val intent = activity?.intent
            val bookingId = intent?.getStringExtra("bookingId")
            val screen = intent?.getStringExtra("screen")

            if (bookingId != null) {
                when (screen) {
                    "active_job" -> navController.navigate(Screen.ProviderActiveJob.createRoute(bookingId))
                    "tracking" -> navController.navigate(Screen.LiveTracking.createRoute(bookingId))
                    "confirmation" -> navController.navigate(Screen.BookingConfirmation.createRoute(bookingId))
                    "chat" -> {
                        // Open notifications screen which now handles opening the chat bottom sheet
                        navController.navigate(Screen.Notifications.route)
                    }
                    "voice_call" -> {
                        val callId = intent.getStringExtra("callId") ?: ""
                        val callerName = intent.getStringExtra("callerName") ?: "Partner"
                        val callerImage = intent.getStringExtra("callerImage")
                        val callerId = intent.getStringExtra("callerId") ?: ""

                        if (callId.isNotEmpty()) {
                            callViewModel.handleIncomingCallFromIntent(
                                callId, callerName, callerImage, bookingId, callerId
                            )
                            if (navController.currentDestination?.route != Screen.VoiceCall.route) {
                                navController.navigate(Screen.VoiceCall.route)
                            }
                        }
                        intent.removeExtra("callId")
                    }
                }
                intent.removeExtra("bookingId")
                intent.removeExtra("screen")
            } else if (screen == "notifications") {
                navController.navigate(Screen.Notifications.route)
                intent?.removeExtra("screen")
            }
        }
    }

    LaunchedEffect(callState) {
        if (callState is com.abhi.madadwala_1.ui.viewmodel.CallState.Incoming || 
            callState is com.abhi.madadwala_1.ui.viewmodel.CallState.Outgoing ||
            callState is com.abhi.madadwala_1.ui.viewmodel.CallState.Ringing ||
            callState is com.abhi.madadwala_1.ui.viewmodel.CallState.Connected) {
            
            // Ensure WebRTC is initialized as early as possible
            callViewModel.initWebRTC(context)

            if (navController.currentDestination?.route != Screen.VoiceCall.route) {
                navController.navigate(Screen.VoiceCall.route)
            }
        }
    }

    LaunchedEffect(authState) {
        val currentRoute = navController.currentDestination?.route
        val startRoutes = listOf(Screen.Splash.route, Screen.Onboarding.route, Screen.Login.route, Screen.Otp.route, Screen.RoleSelection.route)
        val isAtStart = currentRoute == null || currentRoute in startRoutes

        when (authState) {
            is AuthState.Authenticated -> {
                val auth = (authState as AuthState.Authenticated)
                val user = auth.user
                val firebaseUser = auth.firebaseUser
                
                // Register for calls whenever authenticated
                callViewModel.registerUser(firebaseUser.uid)
                
                if (firebaseUser.phoneNumber == "+918460277146") {
                    if (isAtStart || currentRoute != Screen.AdminDashboard.route) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else if (user != null) {
                    if (user.role == "provider" && !user.isVerified && !user.kycRejected) {
                        if (currentRoute != Screen.VerificationPending.route) {
                            navController.navigate(Screen.VerificationPending.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    } else {
                        val targetRoute = when (user.role) {
                            "admin" -> Screen.AdminDashboard.route
                            "provider" -> Screen.ProviderDashboard.route
                            "customer" -> Screen.Home.route
                            else -> Screen.RoleSelection.route
                        }
                        // Only navigate if we are at a start screen OR not already on a home/sub-screen of that role
                        if (isAtStart) {
                            navController.navigate(targetRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
            is AuthState.NewUser -> {
                if (isAtStart) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser?.phoneNumber == "+918460277146") {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) + slideOutHorizontally(targetOffsetX = { -300 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
        popEnterTransition = { fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInHorizontally(initialOffsetX = { -300 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
        popExitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) + slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authState = authState,
                onNavigate = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinish = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Login.route) {
            val activity = LocalContext.current as Activity
            LoginScreen(
                onSendOtp = { phone ->
                    authViewModel.sendOtp(phone, activity)
                    navController.navigate(Screen.Otp.createRoute(phone))
                }
            )
        }
        
        composable(Screen.Otp.route) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phoneNumber = phone,
                authState = authState,
                onVerify = { otp ->
                    authViewModel.verifyOtp(otp)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(onRoleSelected = { role ->
                val authResult = authViewModel.authState.value
                if (authResult is AuthState.Authenticated || authResult is AuthState.NewUser) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val uid = firebaseUser?.uid ?: ""
                    val phone = firebaseUser?.phoneNumber ?: ""
                    if (role == "customer") {
                        navController.navigate(Screen.CustomerSignup.createRoute(uid, phone))
                    } else {
                        navController.navigate(Screen.ProviderSignup.createRoute(uid, phone))
                    }
                }
            })
        }
        
        composable(Screen.CustomerSignup.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            CustomerSignupScreen(
                uid = uid,
                phoneNumber = phone,
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ProviderSignup.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            ProviderSignupScreen(
                uid = uid,
                phoneNumber = phone,
                onBack = { navController.popBackStack() },
                onComplete = {
                    // This is now handled by onRegistrationSuccess to navigate to pending
                },
                onRegistrationSuccess = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ProviderDashboard.route) {
            ProviderDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToActiveJob = { bookingId ->
                    navController.navigate(Screen.ProviderActiveJob.createRoute(bookingId))
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.ProviderActiveJob.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            ProviderActiveJobScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(onLogout = {
                authViewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        
        composable(Screen.VerificationPending.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            VerificationPendingScreen(
                user = user,
                onRefresh = { authViewModel.refresh() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCategoryClick = { category ->
                    navController.navigate(Screen.CategoryListing.createRoute(category))
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.CategoryListing.route) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CategoryListingScreen(
                category = category,
                onBack = { navController.popBackStack() },
                onProviderClick = { providerUid ->
                    navController.navigate(Screen.ProviderProfile.createRoute(providerUid))
                },
                onBookClick = { providerUid, service, price ->
                    navController.navigate(Screen.BookingFlow.createRoute(providerUid, service, price))
                }
            )
        }

        composable(Screen.ProviderProfile.route) { backStackEntry ->
            val providerUid = backStackEntry.arguments?.getString("uid") ?: ""
            ProviderProfileScreen(
                providerUid = providerUid,
                onBack = { navController.popBackStack() },
                onBookService = { serviceName, price ->
                    navController.navigate(Screen.BookingFlow.createRoute(providerUid, serviceName, price))
                }
            )
        }

        composable(Screen.BookingFlow.route) { backStackEntry ->
            val providerUid = backStackEntry.arguments?.getString("providerUid") ?: ""
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            BookingFlowScreen(
                providerUid = providerUid,
                serviceName = serviceName,
                price = price,
                onBack = { navController.popBackStack() },
                onBookingConfirmed = { bookingId ->
                    navController.navigate(Screen.BookingConfirmation.createRoute(bookingId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.BookingConfirmation.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingConfirmationScreen(
                bookingId = bookingId,
                onTrackLive = { id ->
                    navController.navigate(Screen.LiveTracking.createRoute(id))
                },
                onWorkCompleted = { id ->
                    navController.navigate(Screen.WorkCompleted.createRoute(id)) {
                        popUpTo(Screen.BookingConfirmation.route) { inclusive = true }
                    }
                },
                onHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WorkCompleted.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            WorkCompletedScreen(
                bookingId = bookingId,
                onHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onRate = { bId, pUid ->
                    navController.navigate(Screen.RatingReview.createRoute(bId, pUid))
                },
                onInvoice = { booking ->
                    scope.launch {
                        com.abhi.madadwala_1.utils.InvoiceGenerator.generateInvoice(context, booking)
                    }
                }
            )
        }

        composable(Screen.LiveTracking.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            LiveTrackingScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() },
                onComplete = { id ->
                    navController.navigate(Screen.Payment.createRoute(id))
                },
                onPayNow = { id ->
                    navController.navigate(Screen.Payment.createRoute(id))
                }
            )
        }

        composable(Screen.Payment.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            val paymentViewModel: com.abhi.madadwala_1.ui.viewmodel.PaymentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(context as androidx.activity.ComponentActivity)
            
            PaymentScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() },
                onPaymentSuccess = { bId, pUid ->
                    navController.navigate(Screen.RatingReview.createRoute(bId, pUid)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                paymentViewModel = paymentViewModel
            )
        }

        composable(Screen.RatingReview.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val providerUid = backStackEntry.arguments?.getString("providerUid") ?: ""
            RatingReviewScreen(
                bookingId = bookingId,
                providerUid = providerUid,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Offers.route) {
            OffersScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onProviderClick = { uid -> navController.navigate(Screen.ProviderProfile.createRoute(uid)) },
                onBookClick = { uid, cat, price -> navController.navigate(Screen.BookingFlow.createRoute(uid, cat, price)) }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AllCategories.route) {
            val authViewModel: com.abhi.madadwala_1.ui.viewmodel.AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            val user = (authState as? com.abhi.madadwala_1.ui.viewmodel.AuthState.Authenticated)?.user
            
            val locationViewModel: com.abhi.madadwala_1.ui.viewmodel.LocationViewModel = viewModel(
                viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
            )
            val userLat by locationViewModel.latitude.collectAsState()
            val userLng by locationViewModel.longitude.collectAsState()

            AllCategoriesScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    navController.navigate(Screen.CategoryListing.createRoute(category))
                },
                user = user,
                userLat = userLat,
                userLng = userLng
            )
        }

        composable(Screen.InstantCategories.route) {
            val authViewModel: com.abhi.madadwala_1.ui.viewmodel.AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            val user = (authState as? com.abhi.madadwala_1.ui.viewmodel.AuthState.Authenticated)?.user
            
            val locationViewModel: com.abhi.madadwala_1.ui.viewmodel.LocationViewModel = viewModel(
                viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
            )
            val userLat by locationViewModel.latitude.collectAsState()
            val userLng by locationViewModel.longitude.collectAsState()

            InstantCategoriesScreen(
                onBack = { navController.popBackStack() },
                user = user,
                userLat = userLat,
                userLng = userLng
            )
        }

        composable(Screen.AddMoney.route) {
            AddMoneyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SendMoney.route) {
            SendMoneyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.InviteEarn.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            InviteEarnScreen(
                user = user,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VoiceCall.route) {
            VoiceCallScreen(
                viewModel = callViewModel,
                onDismiss = { 
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        // If no backstack (e.g. app launched from notification), navigate to home
                        val user = (authState as? com.abhi.madadwala_1.ui.viewmodel.AuthState.Authenticated)?.user
                        val targetRoute = when (user?.role) {
                            "admin" -> Screen.AdminDashboard.route
                            "provider" -> Screen.ProviderDashboard.route
                            else -> Screen.Home.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
