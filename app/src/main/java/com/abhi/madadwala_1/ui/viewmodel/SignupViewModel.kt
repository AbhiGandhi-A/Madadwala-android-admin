package com.abhi.madadwala_1.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}

class SignupViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun register(
        context: Context,
        uid: String,
        phoneNumber: String,
        role: String,
        name: String,
        email: String?,
        category: String? = null,
        profession: String? = null,
        aadhaarNumber: String? = null,
        referralCode: String? = null,
        profileImageUri: Uri?,
        aadhaarImageUri: Uri?
    ) {
        _signupState.value = SignupState.Loading
        viewModelScope.launch {
            try {
                val uidBody = uid.toRequestBody("text/plain".toMediaTypeOrNull())
                val phoneBody = phoneNumber.toRequestBody("text/plain".toMediaTypeOrNull())
                val roleBody = role.toRequestBody("text/plain".toMediaTypeOrNull())
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailBody = email?.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = category?.toRequestBody("text/plain".toMediaTypeOrNull())
                val professionBody = profession?.toRequestBody("text/plain".toMediaTypeOrNull())
                val aadhaarNoBody = aadhaarNumber?.toRequestBody("text/plain".toMediaTypeOrNull())
                val referralBody = referralCode?.toRequestBody("text/plain".toMediaTypeOrNull())

                val profilePart = profileImageUri?.let { uriToMultipart(context, it, "profileImage") }
                val aadhaarPart = aadhaarImageUri?.let { uriToMultipart(context, it, "aadhaarImage") }

                val response = apiService.registerUser(
                    uidBody, phoneBody, roleBody, nameBody, emailBody, categoryBody, 
                    professionBody, aadhaarNoBody, referralBody, profilePart, aadhaarPart
                )

                if (response.isSuccessful) {
                    _signupState.value = SignupState.Success
                } else {
                    _signupState.value = SignupState.Error("Registration failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun uriToMultipart(context: Context, uri: Uri, partName: String): MultipartBody.Part? {
        val file = uriToFile(context, uri) ?: return null
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
