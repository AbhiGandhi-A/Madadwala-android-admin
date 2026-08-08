package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.ui.components.TransactionItemV2
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.WalletState
import com.abhi.madadwala_1.ui.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedDateRange by remember { mutableStateOf("All Time") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Transaction History", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                        }
                    }
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by title or description...", color = MadadwalaColors.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MadadwalaColors.Gray) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MadadwalaColors.Ink,
                        unfocusedTextColor = MadadwalaColors.Ink,
                        focusedBorderColor = MadadwalaColors.Green,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        cursorColor = MadadwalaColors.Green
                    )
                )

                // Filters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Filter
                    val types = listOf("All", "Credit", "Debit")
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { 
                                Text(
                                    text = type,
                                    color = if (selectedType == type) MadadwalaColors.Green else MadadwalaColors.Gray,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = MadadwalaColors.Gray,
                                selectedContainerColor = MadadwalaColors.Green.copy(alpha = 0.15f),
                                selectedLabelColor = MadadwalaColors.Green
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.LightGray.copy(alpha = 0.5f),
                                selectedBorderColor = MadadwalaColors.Green,
                                enabled = true,
                                selected = selectedType == type
                            )
                        )
                    }
                    
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )

                    // Date Filter
                    val ranges = listOf("All Time", "Today", "Last 7 Days", "Last 30 Days")
                    ranges.forEach { range ->
                        FilterChip(
                            selected = selectedDateRange == range,
                            onClick = { selectedDateRange = range },
                            label = { 
                                Text(
                                    text = range,
                                    color = if (selectedDateRange == range) MadadwalaColors.Green else MadadwalaColors.Gray,
                                    fontWeight = if (selectedDateRange == range) FontWeight.Bold else FontWeight.Medium
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = MadadwalaColors.Gray,
                                selectedContainerColor = MadadwalaColors.Green.copy(alpha = 0.15f),
                                selectedLabelColor = MadadwalaColors.Green
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.LightGray.copy(alpha = 0.5f),
                                selectedBorderColor = MadadwalaColors.Green,
                                enabled = true,
                                selected = selectedDateRange == range
                            )
                        )
                    }
                }
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9F9))
        ) {
            when (val state = uiState) {
                is WalletState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Green)
                }
                is WalletState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = state.message, color = Color.Red)
                        Button(onClick = { viewModel.fetchWalletData() }, colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green)) {
                            Text("Retry")
                        }
                    }
                }
                is WalletState.Success -> {
                    val filteredTransactions = remember(state.transactions, searchQuery, selectedType, selectedDateRange) {
                        state.transactions.filter { tx ->
                            // 1. Search filter
                            val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true) || 
                                              (tx.description?.contains(searchQuery, ignoreCase = true) == true)
                            
                            // 2. Type filter
                            val matchesType = selectedType == "All" || tx.type.equals(selectedType, ignoreCase = true)
                            
                            // 3. Date filter
                            val matchesDate = when (selectedDateRange) {
                                "Today" -> isDateToday(tx.createdAt)
                                "Last 7 Days" -> isWithinDays(tx.createdAt, 7)
                                "Last 30 Days" -> isWithinDays(tx.createdAt, 30)
                                else -> true
                            }
                            
                            matchesSearch && matchesType && matchesDate
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isNotEmpty() || selectedType != "All" || selectedDateRange != "All Time") 
                                        "No matching transactions found" 
                                    else "No transactions yet", 
                                    color = Color.Gray
                                )
                                if (searchQuery.isNotEmpty() || selectedType != "All" || selectedDateRange != "All Time") {
                                    TextButton(onClick = {
                                        searchQuery = ""
                                        selectedType = "All"
                                        selectedDateRange = "All Time"
                                    }) {
                                        Text("Clear Filters", color = MadadwalaColors.Green)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTransactions) { transaction ->
                                TransactionItemV2(transaction, state.balance)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

private fun isDateToday(dateStr: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val txDate = sdf.parse(dateStr.take(10))
        val today = sdf.parse(sdf.format(java.util.Date()))
        txDate == today
    } catch (e: Exception) { false }
}

private fun isWithinDays(dateStr: String, days: Int): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val txDate = sdf.parse(dateStr.take(10)) ?: return false
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val limitDate = calendar.time
        txDate.after(limitDate) || txDate == limitDate
    } catch (e: Exception) { false }
}
