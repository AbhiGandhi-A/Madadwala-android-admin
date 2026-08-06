package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.ProviderResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onProviderClick: (String) -> Unit,
    onBookClick: (String, String, Double) -> Unit
) {
    var favorites by remember { mutableStateOf<List<ProviderResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            scope.launch {
                try {
                    val res = RetrofitClient.apiService.getFavorites(uid)
                    if (res.isSuccessful) favorites = res.body() ?: emptyList()
                } catch (e: Exception) {} finally { isLoading = false }
            }
        } ?: run { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Favorites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MadadwalaColors.Cream
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MadadwalaColors.Teal)
            }
        } else if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't added any favorites yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favorites) { provider ->
                    ProviderCard(
                        provider = provider,
                        distance = "Favorite",
                        isFavorite = true,
                        onFavoriteToggle = {
                            scope.launch {
                                user?.uid?.let { uid ->
                                    val res = RetrofitClient.apiService.removeFromFavorites(uid, provider.uid)
                                    if (res.isSuccessful) {
                                        favorites = favorites.filter { it.uid != provider.uid }
                                    }
                                }
                            }
                        },
                        onCardClick = { onProviderClick(provider.uid) },
                        onBookClick = { onBookClick(provider.uid, provider.category, provider.startingPrice) }
                    )
                }
            }
        }
    }
}
