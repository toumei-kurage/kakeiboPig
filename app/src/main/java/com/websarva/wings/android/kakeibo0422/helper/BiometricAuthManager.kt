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
import androidx.core.content.edit

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
        private const val KEY_BIOMETRIC_PROMPT_DISMISSED = "key_biometric_prompt_dismissed"
    }

    /**
     * 初回登録ダイアログで「NO」を選択済み（または非表示対象）か判定
     */
    fun isBiometricPromptDismissed(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_PROMPT_DISMISSED, false)
    }

    /**
     * 初回登録ダイアログのスキップフラグを設定
     */
    fun setBiometricPromptDismissed(dismissed: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_PROMPT_DISMISSED, dismissed)
            .apply()
    }
    /**
     * 端末が生体認証に対応し、指紋が登録されているか判定
     */
    fun canAuthenticateStatus(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    }

    /**
     * 端末内に指紋情報が登録されているか
     */
    fun isFingerprintEnrolled(): Boolean {
        return canAuthenticateStatus() != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    /**
     * アプリ内で生体認証ログインが有効化されているか
     */
    fun isBiometricAuthEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_BIOMETRIC_ENABLED, false) &&
                !getSavedEmail().isNullOrEmpty() &&
                !getSavedPassword().isNullOrEmpty()
    }

    /**
     * 資格情報の暗号化保存 & 有効化
     * （SettingActivityからの登録時も含め、登録確認ダイアログスキップフラグも同時にtrueにする）
     */
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit {
            putBoolean(KEY_IS_BIOMETRIC_ENABLED, true)
                .putBoolean(KEY_BIOMETRIC_PROMPT_DISMISSED, true)
                .putString(KEY_SAVED_EMAIL, email)
                .putString(KEY_SAVED_PASSWORD, password)
        }
    }

    /**
     * 保存されている資格情報と生体認証設定を削除
     * ※ KEY_BIOMETRIC_PROMPT_DISMISSED は保持し、OFFにした後に通常ログインした際ダイアログが再表示されるのを防ぐ
     */
    fun clearCredentials() {
        sharedPreferences.edit {
            remove(KEY_IS_BIOMETRIC_ENABLED)
                .remove(KEY_SAVED_EMAIL)
                .remove(KEY_SAVED_PASSWORD)
        }
    }

    fun getSavedEmail(): String? = sharedPreferences.getString(KEY_SAVED_EMAIL, null)
    fun getSavedPassword(): String? = sharedPreferences.getString(KEY_SAVED_PASSWORD, null)

    /**
     * 端末設定画面への登録誘導ダイアログを表示
     */
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