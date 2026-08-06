package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class PaymentResult {
    data class Success(val paymentId: String?) : PaymentResult()
    data class Error(val code: Int, val message: String?) : PaymentResult()
}

class PaymentViewModel : ViewModel() {
    private val _paymentResult = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
    val paymentResult = _paymentResult.asSharedFlow()

    suspend fun onPaymentSuccess(paymentId: String?) {
        _paymentResult.emit(PaymentResult.Success(paymentId))
    }

    suspend fun onPaymentError(code: Int, message: String?) {
        _paymentResult.emit(PaymentResult.Error(code, message))
    }
}
