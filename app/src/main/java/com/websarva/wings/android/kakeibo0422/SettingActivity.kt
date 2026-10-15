package com.websarva.wings.android.kakeibo0422

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.websarva.wings.android.kakeibo0422.helper.BiometricAuthManager

class SettingActivity : BaseActivity(R.layout.activity_setting, R.string.title_setting) {

    private lateinit var provisionalBudgetLayout: TextInputLayout
    private lateinit var provisionalBudgetEditText: TextInputEditText
    private lateinit var switchRoundDown: SwitchMaterial
    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var buttonSaveSetting: Button

    private lateinit var biometricAuthManager: BiometricAuthManager
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        setupDrawerAndToolbar()

        biometricAuthManager = BiometricAuthManager(this)

        provisionalBudgetLayout = findViewById(R.id.provisionalBudgetLayout)
        provisionalBudgetEditText = findViewById(R.id.provisionalBudgetEditText)
        switchRoundDown = findViewById(R.id.switchRoundDown)
        switchBiometric = findViewById(R.id.switchBiometric)
        buttonSaveSetting = findViewById(R.id.buttonSaveSetting)


        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        // 現在ログインしているユーザーに紐づく生体認証が有効か判定
        switchBiometric.isChecked = biometricAuthManager.isBiometricAuthEnabledFor(currentUserEmail)

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (!switchBiometric.isPressed) return@setOnCheckedChangeListener // ユーザー操作のみ検知

            if (isChecked) {
                // 生体認証をONにする処理
                handleEnableBiometric()
            } else {
                // 生体認証をOFF（削除）にする処理
                biometricAuthManager.clearCredentials()
                Toast.makeText(this, R.string.toast_biometric_disabled, Toast.LENGTH_SHORT).show()
            }
        }

        loadUserSettings()

        buttonSaveSetting.setOnClickListener {
            clearKeyboardFocus()
            saveUserSettings()
        }
    }

    /**
     * 生体認証の再有効化処理
     */
    private fun handleEnableBiometric() {
        if (!biometricAuthManager.isFingerprintEnrolled()) {
            switchBiometric.isChecked = false
            biometricAuthManager.showEnrollPromptDialog(this)
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        if (currentUser == null || email.isNullOrEmpty()) {
            switchBiometric.isChecked = false
            Toast.makeText(this, "ユーザー情報の取得に失敗しました", Toast.LENGTH_SHORT).show()
            return
        }

        // 保存用パスワードを取得・検証するためのダイアログ
        val passwordInput = EditText(this).apply {
            hint = "パスワード"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(passwordInput)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_biometric_reauth_title)
            .setMessage(R.string.dialog_biometric_reauth_message)
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val enteredPassword = passwordInput.text.toString().trim()
                if (enteredPassword.isEmpty()) {
                    switchBiometric.isChecked = false
                    Toast.makeText(this, "パスワードを入力してください", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 入力されたパスワードの整合性をFirebaseで検証
                val credential = EmailAuthProvider.getCredential(email, enteredPassword)
                currentUser.reauthenticate(credential)
                    .addOnSuccessListener {
                        // パスワード認証成功後、指紋認証プロンプトを表示
                        biometricAuthManager.authenticate(
                            activity = this,
                            title = getString(R.string.biometric_prompt_title),
                            subtitle = getString(R.string.biometric_prompt_subtitle),
                            onSuccess = {
                                // 資格情報の暗号化保存とフラグ更新
                                biometricAuthManager.saveCredentials(email, enteredPassword)
                                switchBiometric.isChecked = true
                                Toast.makeText(this, R.string.toast_biometric_registered, Toast.LENGTH_SHORT).show()
                            },
                            onError = { _, _ ->
                                switchBiometric.isChecked = false
                            },
                            onFailed = {
                                Toast.makeText(this, "認証に失敗しました", Toast.LENGTH_SHORT).show()
                                switchBiometric.isChecked = false
                            }
                        )
                    }
                    .addOnFailureListener {
                        switchBiometric.isChecked = false
                        Toast.makeText(this, "パスワードが正しくありません", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                switchBiometric.isChecked = false
                dialog.dismiss()
            }
            .setOnCancelListener {
                switchBiometric.isChecked = false
            }
            .show()
    }

    private fun loadUserSettings() {
        if (userID.isEmpty()) return

        firestore.collection("user_settings")
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val budget = document.getLong("provisional_budget") ?: 0
                    val roundDown = document.getBoolean("round_down_fraction") ?: false
                    provisionalBudgetEditText.setText(budget.toString())
                    switchRoundDown.isChecked = roundDown
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserSettings() {
        val budgetText = provisionalBudgetEditText.text.toString().trim()
        if (budgetText.isEmpty()) {
            provisionalBudgetLayout.error = "仮予算を入力してください。"
            return
        }
        provisionalBudgetLayout.error = null

        val budget = budgetText.toIntOrNull() ?: 0
        val roundDown = switchRoundDown.isChecked

        val settingData = hashMapOf(
            "user_id" to userID,
            "provisional_budget" to budget,
            "round_down_fraction" to roundDown,
            "updated_at" to FieldValue.serverTimestamp()
        )

        firestore.collection("user_settings")
            .document(userID)
            .set(settingData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearKeyboardFocus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(provisionalBudgetEditText.windowToken, 0)
        provisionalBudgetEditText.clearFocus()
    }
}