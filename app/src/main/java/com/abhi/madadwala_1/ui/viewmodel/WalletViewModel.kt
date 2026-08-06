package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.TransactionResponse
import com.abhi.madadwala_1.data.remote.UserResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WalletState {
    object Idle : WalletState()
    object Loading : WalletState()
    data class Success(val balance: Double, val transactions: List<TransactionResponse>, val walletId: String) : WalletState()
    data class Error(val message: String) : WalletState()
}

class WalletViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<WalletState>(WalletState.Idle)
    val uiState: StateFlow<WalletState> = _uiState

    init {
        fetchWalletData()
    }

    fun fetchWalletData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = WalletState.Loading
        viewModelScope.launch {
            try {
                val userResponse = apiService.checkUser(auth.currentUser?.phoneNumber ?: "", uid)
                val transResponse = apiService.getWalletTransactions(uid)

                if (userResponse.isSuccessful && transResponse.isSuccessful) {
                    val user = userResponse.body()?.user
                    val transactions = transResponse.body() ?: emptyList()
                    
                    if (user != null) {
                        _uiState.value = WalletState.Success(
                            balance = user.walletBalance,
                            transactions = transactions,
                            walletId = "MW${uid.takeLast(6).uppercase()}"
                        )
                    } else {
                        _uiState.value = WalletState.Error("User not found")
                    }
                } else {
                    _uiState.value = WalletState.Error("Failed to fetch wallet data")
                }
            } catch (e: Exception) {
                _uiState.value = WalletState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
