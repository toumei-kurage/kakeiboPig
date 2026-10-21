package com.websarva.wings.android.kakeibo0422

import android.os.Bundle
import android.text.InputType
import android.view.View
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
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper

class SettingActivity : BaseActivity(R.layout.activity_setting, R.string.title_setting) {

    private lateinit var provisionalBudgetLayout: TextInputLayout
    private lateinit var provisionalBudgetEditText: TextInputEditText
    private lateinit var switchRoundDown: SwitchMaterial
    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var buttonSaveSetting: Button

    private lateinit var biometricAuthManager: BiometricAuthManager
    private val validateHelper = ValidateHelper(this)
    private val firestore = FirebaseFirestore.getInstance()

    // 初期値保持用（差分検知用）
    private var initialBudget: Int? = null
    private var initialRoundDown: Boolean? = null
    private var isInitiallyBiometricEnabled: Boolean = false

    // 生体認証の保留状態を管理
    private enum class PendingBiometricState {
        NONE,    // 変更なし
        ENABLE,  // 保存ボタン押下時に有効化
        DISABLE  // 保存ボタン押下時に無効化
    }
    private var pendingBiometricState = PendingBiometricState.NONE
    private var pendingPassword: String? = null

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
        isInitiallyBiometricEnabled = biometricAuthManager.isBiometricAuthEnabledFor(currentUserEmail)
        switchBiometric.isChecked = isInitiallyBiometricEnabled

        provisionalBudgetEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val (result, errorMessage) = validateHelper.payAmountCheck(provisionalBudgetEditText)
                if (!result) {
                    provisionalBudgetLayout.error = errorMessage
                    return@OnFocusChangeListener
                }
            }
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (!switchBiometric.isPressed) return@setOnCheckedChangeListener // ユーザー操作のみ検知

            if (isChecked) {
                if (isInitiallyBiometricEnabled) {
                    // 元々ONだった状態に戻した場合は変更なし
                    pendingBiometricState = PendingBiometricState.NONE
                    pendingPassword = null
                } else {
                    // OFF -> ON の認証準備
                    handleEnableBiometric()
                }
            } else {
                if (!isInitiallyBiometricEnabled) {
                    // 元々OFFだった状態に戻した場合は変更なし
                    pendingBiometricState = PendingBiometricState.NONE
                    pendingPassword = null
                } else {
                    // ON -> OFF の保留
                    pendingBiometricState = PendingBiometricState.DISABLE
                    pendingPassword = null
                }
            }
        }

        loadUserSettings()

        buttonSaveSetting.setOnClickListener {
            clearKeyboardFocus()

            val budgetText = provisionalBudgetEditText.text.toString().trim()
            if (budgetText.isEmpty()) {
                provisionalBudgetLayout.error = "仮予算を入力してください。"
                return@setOnClickListener
            }

            //すべての入力項目のバリデーションチェック
            val (resultProvisionalBudget: Boolean, provisionalBudgetMsg: String) = validateHelper.payAmountCheck(provisionalBudgetEditText)
            if (!resultProvisionalBudget) {
                provisionalBudgetLayout.error = provisionalBudgetMsg
                return@setOnClickListener
            }
            provisionalBudgetLayout.error = null

            val currentBudget = budgetText.toIntOrNull() ?: 0
            val currentRoundDown = switchRoundDown.isChecked

            // 差分検知（Firestore側、ローカル生体認証側）
            val isFirestoreChanged = (initialBudget != currentBudget) || (initialRoundDown != currentRoundDown)
            val isBiometricChanged = (pendingBiometricState != PendingBiometricState.NONE)

            if (!isFirestoreChanged && !isBiometricChanged) {
                Toast.makeText(this, "変更はありません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isFirestoreChanged) {
                // Firestoreに変更がある場合は通信を実行（生体認証の変更も完了時にコミット）
                saveFirestoreUserSettings(currentBudget, currentRoundDown)
            } else {
                // 生体認証のみ変更された場合は通信を行わずローカルのみ更新
                commitBiometricSettings()
                Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 生体認証の事前検証処理（保存は行わず、認証成功時に保留状態とする）
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
                                // 認証成功：保存は行わず保留状態にする
                                pendingBiometricState = PendingBiometricState.ENABLE
                                pendingPassword = enteredPassword
                                switchBiometric.isChecked = true
                            },
                            onError = { _, _ ->
                                switchBiometric.isChecked = false
                                pendingBiometricState = PendingBiometricState.NONE
                                pendingPassword = null
                            },
                            onFailed = {
                                Toast.makeText(this, "認証に失敗しました", Toast.LENGTH_SHORT).show()
                                switchBiometric.isChecked = false
                                pendingBiometricState = PendingBiometricState.NONE
                                pendingPassword = null
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
                    val budget = (document.getLong("provisional_budget") ?: 0).toInt()
                    val roundDown = document.getBoolean("round_down_fraction") ?: false
                    provisionalBudgetEditText.setText(budget.toString())
                    switchRoundDown.isChecked = roundDown

                    // 取得した初期値を保持
                    initialBudget = budget
                    initialRoundDown = roundDown
                } else {
                    initialBudget = 0
                    initialRoundDown = false
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFirestoreUserSettings(budget: Int, roundDown: Boolean) {
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
                // Firestore保存成功に合わせて初期値スナップショットを更新
                initialBudget = budget
                initialRoundDown = roundDown

                // 生体認証の保留変更を一括コミット
                commitBiometricSettings()
                Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 保留されている生体認証設定を確定する
     */
    private fun commitBiometricSettings() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        when (pendingBiometricState) {
            PendingBiometricState.ENABLE -> {
                val password = pendingPassword
                if (!password.isNullOrEmpty() && currentUserEmail.isNotEmpty()) {
                    biometricAuthManager.saveCredentials(currentUserEmail, password)
                    isInitiallyBiometricEnabled = true
                }
            }
            PendingBiometricState.DISABLE -> {
                biometricAuthManager.clearCredentials()
                isInitiallyBiometricEnabled = false
            }
            PendingBiometricState.NONE -> {
                // 変更なし
            }
        }
        // コミット完了後は保留状態をリセット
        pendingBiometricState = PendingBiometricState.NONE
        pendingPassword = null
    }

    private fun clearKeyboardFocus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(provisionalBudgetEditText.windowToken, 0)
        provisionalBudgetEditText.clearFocus()
    }
}