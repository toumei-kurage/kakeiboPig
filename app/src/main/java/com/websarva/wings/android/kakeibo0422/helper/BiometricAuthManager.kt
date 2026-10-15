package com.websarva.wings.android.kakeibo0422.helper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.websarva.wings.android.kakeibo0422.R

class BiometricAuthManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "biometric_secured_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_IS_BIOMETRIC_ENABLED = "key_is_biometric_enabled"
        private const val KEY_SAVED_EMAIL = "key_saved_email"
        private const val KEY_SAVED_PASSWORD = "key_saved_password"
        private const val PREFIX_DISMISSED = "key_dismissed_"
    }

    fun canAuthenticateStatus(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    }

    fun isFingerprintEnrolled(): Boolean {
        return canAuthenticateStatus() != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    /**
     * 端末全体で生体認証ログインが有効か（アプリ起動時の自動ポップアップ表示判定用）
     */
    fun isBiometricAuthEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_BIOMETRIC_ENABLED, false) &&
                !getSavedEmail().isNullOrEmpty() &&
                !getSavedPassword().isNullOrEmpty()
    }

    /**
     * 指定されたメールアドレスに対して生体認証が有効化されているか
     */
    fun isBiometricAuthEnabledFor(email: String): Boolean {
        return isBiometricAuthEnabled() && getSavedEmail() == email
    }

    /**
     * 指定されたメールアドレスで登録確認ダイアログをスキップ（NOを選択済み）しているか
     */
    fun isPromptDismissedFor(email: String): Boolean {
        val key = PREFIX_DISMISSED + email.replace(".", "_")
        return sharedPreferences.getBoolean(key, false)
    }

    /**
     * 指定されたメールアドレスに対するスキップフラグを設定
     */
    fun setPromptDismissedFor(email: String, dismissed: Boolean) {
        val key = PREFIX_DISMISSED + email.replace(".", "_")
        sharedPreferences.edit()
            .putBoolean(key, dismissed)
            .apply()
    }

    /**
     * 資格情報を最新のユーザー情報で上書き保存 & 有効化
     */
    fun saveCredentials(email: String, password: String) {
        setPromptDismissedFor(email, true)
        sharedPreferences.edit()
            .putBoolean(KEY_IS_BIOMETRIC_ENABLED, true)
            .putString(KEY_SAVED_EMAIL, email)
            .putString(KEY_SAVED_PASSWORD, password)
            .apply()
    }

    /**
     * 保存されている資格情報と生体認証設定を削除
     */
    fun clearCredentials() {
        sharedPreferences.edit()
            .remove(KEY_IS_BIOMETRIC_ENABLED)
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_PASSWORD)
            .apply()
    }

    fun getSavedEmail(): String? = sharedPreferences.getString(KEY_SAVED_EMAIL, null)
    fun getSavedPassword(): String? = sharedPreferences.getString(KEY_SAVED_PASSWORD, null)

    fun showEnrollPromptDialog(activity: FragmentActivity, onCancel: () -> Unit = {}) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.dialog_biometric_not_enrolled_title)
            .setMessage(R.string.dialog_biometric_not_enrolled_message)
            .setPositiveButton(R.string.button_open_settings) { _, _ ->
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
                    }
                } else {
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton(R.string.button_later) { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * BiometricPrompt の表示
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = activity.getString(R.string.biometric_prompt_title),
        subtitle: String = activity.getString(R.string.biometric_prompt_subtitle),
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(activity.getString(R.string.biometric_prompt_negative))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}