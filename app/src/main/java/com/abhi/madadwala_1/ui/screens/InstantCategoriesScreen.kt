package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.data.remote.CategoryResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantCategoriesScreen(
    onBack: () -> Unit,
    user: com.abhi.madadwala_1.data.remote.UserResponse?,
    userLat: Double,
    userLng: Double
) {
    var categories by remember { mutableStateOf<List<CategoryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInstantDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getCategories()
            if (response.isSuccessful) {
                categories = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
        } finally {
            isLoading = false
        }
    }

    val displayCategories = if (categories.isEmpty()) {
        listOf(
            CategoryResponse("", "Cleaning", "Cleaning", null),
            CategoryResponse("", "Plumbing", "Plumbing", null),
            CategoryResponse("", "Electrical", "Electrical", null),
            CategoryResponse("", "Painting", "Painting", null),
            CategoryResponse("", "Appliances", "Appliance", null),
            CategoryResponse("", "Carpentry", "Carpentry", null),
            CategoryResponse("", "Pest Control", "Pest Control", null),
            CategoryResponse("", "AC Service", "AC Service", null),
            CategoryResponse("", "Gardening", "Gardening", null)
        )
    } else {
        categories
    }

    val filteredCategories = remember(displayCategories, searchQuery) {
        if (searchQuery.isBlank()) {
            displayCategories
        } else {
            displayCategories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "All Categories", 
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.Green
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MadadwalaColors.Green
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA) // Light background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search categories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MadadwalaColors.Green,
                    unfocusedBorderColor = Color.LightGray.copy(0.5f)
                )
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && categories.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Green)
                } else if (filteredCategories.isEmpty()) {
                    Text(
                        "No categories found",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredCategories) { category ->
                            InstantCategoryItem(category = category) {
                                selectedCategory = category.name
                                showInstantDialog = true
                            }
                        }
                    }
                }
            }
        }

        if (showInstantDialog && selectedCategory != null && user != null) {
            CustomRequestDialog(
                category = selectedCategory!!,
                user = user,
                onDismiss = { showInstantDialog = false },
                userLat = userLat,
                userLng = userLng
            )
        }
    }
}

@Composable
fun InstantCategoryItem(category: CategoryResponse, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MadadwalaColors.Green.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(category.icon ?: category.name),
                    contentDescription = null,
                    tint = MadadwalaColors.Green,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.weight(1f)
            )

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
