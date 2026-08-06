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
import com.abhi.madadwala_1.data.remote.CategoryResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    user: com.abhi.madadwala_1.data.remote.UserResponse?,
    userLat: Double,
    userLng: Double
) {
    var categories by remember { mutableStateOf<List<CategoryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && categories.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Green)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayCategories) { category ->
                        AllCategoryListItem(category = category) {
                            onCategoryClick(category.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllCategoryListItem(category: CategoryResponse, onClick: () -> Unit) {
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

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
