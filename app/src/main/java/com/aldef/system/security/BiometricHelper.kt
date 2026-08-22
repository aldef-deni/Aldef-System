package com.aldef.system.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Hasil pemeriksaan kesiapan biometrik pada perangkat. */
enum class BiometricStatus {
    READY,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE;

    val isUsable: Boolean get() = this == READY
}

class BiometricHelper(private val context: Context) {

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun status(): BiometricStatus =
        when (BiometricManager.from(context).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }

    /**
     * Menampilkan dialog biometrik sistem.
     *
     * Catatan: aplikasi tidak pernah menyentuh data sidik jari. Pendaftaran
     * sidik jari sendiri dilakukan di Pengaturan Android; yang disimpan di sini
     * hanya penanda bahwa pengguna sudah menautkan biometriknya ke aplikasi.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeText: String = "Batal",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Pembatalan oleh pengguna bukan kegagalan yang perlu diteriakkan.
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onError("")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText(negativeText)
            .setConfirmationRequired(false)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
