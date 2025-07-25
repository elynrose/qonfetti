package com.example.qonfetty.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricAuthManager(private val context: Context) {
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }
    
    suspend fun authenticateWithBiometric(activity: FragmentActivity): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        continuation.resume(Result.failure(Exception(errString.toString())))
                    }
                    
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(Result.success(Unit))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        continuation.resume(Result.failure(Exception("Authentication failed")))
                    }
                })
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login with Fingerprint")
                .setSubtitle("Use your fingerprint to login to Qonfetty")
                .setNegativeButtonText("Use Password")
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    fun getBiometricStatus(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric enrolled"
            else -> "Unknown"
        }
    }
} 