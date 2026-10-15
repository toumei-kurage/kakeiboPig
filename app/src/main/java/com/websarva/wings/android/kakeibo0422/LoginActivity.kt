package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo0422.helper.BiometricAuthManager
import com.websarva.wings.android.kakeibo0422.helper.DialogHelper
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper

/**
 * ログイン画面
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var biometricAuthManager: BiometricAuthManager

    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 端末の戻るボタンを無効化
        onBackPressedDispatcher.addCallback(this) {
            // 何もしない
        }

        biometricAuthManager = BiometricAuthManager(this)

        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_login)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // 画面部品の取得
        val emailError = findViewById<TextInputLayout>(R.id.email)
        emailEditText = findViewById(R.id.emailEditText)
        val passwordError = findViewById<TextInputLayout>(R.id.password)
        passwordEditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val signUpButton: Button = findViewById(R.id.SignUpButton)
        val versionNameTextView = findViewById<TextView>(R.id.versionName)

        versionNameTextView.text = "ver:${this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_META_DATA).versionName}"

        // FirebaseAuthのインスタンスを取得
        auth = FirebaseAuth.getInstance()

        emailEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.emailCheck(emailEditText)
                if (!result) {
                    emailError.error = errorMsg
                    return@OnFocusChangeListener
                }
                emailError.error = ""
            }
        }

        passwordEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.passwordCheck(passwordEditText)
                if (!result) {
                    passwordError.error = errorMsg
                    return@OnFocusChangeListener
                }
                passwordError.error = ""
            }
        }

        loginButton.setOnClickListener {
            clearBordFocus()
            //すべての入力項目のバリデーションチェック
            val (resultEmail: Boolean, emailMsg: String) = validateHelper.emailCheck(emailEditText)
            val (resultPassword: Boolean, passwordMsg) = validateHelper.passwordCheck(passwordEditText)
            if (!(resultEmail && resultPassword)) {
                emailError.error = emailMsg
                passwordError.error = passwordMsg
                return@setOnClickListener
            }

            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            login(email, password, isBiometricFlow = false)
        }

        signUpButton.setOnClickListener {
            clearBordFocus()
            val intent = Intent(this, UserAddActivity::class.java)
            startActivity(intent)
        }

        // 登録済みの場合は起動時に生体認証プロンプトを自動表示
        if (savedInstanceState == null && biometricAuthManager.isBiometricAuthEnabled()) {
            checkAndTriggerBiometricLogin()
        }
    }

    /**
     * 生体認証による自動ログインの開始
     */
    private fun checkAndTriggerBiometricLogin() {
        if (!biometricAuthManager.isFingerprintEnrolled()) {
            biometricAuthManager.showEnrollPromptDialog(this)
            return
        }

        biometricAuthManager.authenticate(
            activity = this,
            onSuccess = {
                val savedEmail = biometricAuthManager.getSavedEmail()
                val savedPassword = biometricAuthManager.getSavedPassword()
                if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                    emailEditText.setText(savedEmail)
                    passwordEditText.setText(savedPassword)
                    login(savedEmail, savedPassword, isBiometricFlow = true)
                }
            },
            onError = { _, _ -> },
            onFailed = {
                Toast.makeText(this, "指紋認証に失敗しました", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Firebase認証処理
     */
    private fun login(email: String, password: String, isBiometricFlow: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "ログイン成功", Toast.LENGTH_SHORT).show()

                    if (isBiometricFlow) {
                        navigateToHome()
                        return@addOnCompleteListener
                    }

                    val savedEmail = biometricAuthManager.getSavedEmail()

                    if (savedEmail != null && savedEmail != email) {
                        // 以前と異なるアカウントでログインした場合：最新アカウントへの更新ダイアログを表示
                        showBiometricSwitchUserDialog(email, password)
                    } else if (biometricAuthManager.isBiometricAuthEnabledFor(email)) {
                        // 既にこのアカウントで生体認証が有効
                        navigateToHome()
                    } else if (biometricAuthManager.isPromptDismissedFor(email)) {
                        // このアカウントで「NO」を選択済み
                        navigateToHome()
                    } else {
                        // 初回：登録確認ダイアログを表示
                        showBiometricRegistrationDialog(email, password)
                    }
                } else {
                    dialogHelper.dialogOkOnly("ログイン失敗", "ユーザーIDまたはパスワードが間違っています。")
                }
            }
    }

    /**
     * 別アカウント登録済みの状態から最新アカウントへ更新するダイアログ
     */
    private fun showBiometricSwitchUserDialog(email: String, password: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_biometric_switch_user_title)
            .setMessage(getString(R.string.dialog_biometric_switch_user_message, email))
            .setPositiveButton("YES") { _, _ ->
                if (!biometricAuthManager.isFingerprintEnrolled()) {
                    biometricAuthManager.showEnrollPromptDialog(this, onCancel = {
                        biometricAuthManager.clearCredentials()
                        navigateToHome()
                    })
                } else {
                    biometricAuthManager.authenticate(
                        activity = this,
                        onSuccess = {
                            // 最新のアカウント（B）で上書き保存
                            biometricAuthManager.saveCredentials(email, password)
                            Toast.makeText(this, R.string.toast_biometric_registered, Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        },
                        onError = { _, _ ->
                            biometricAuthManager.clearCredentials()
                            navigateToHome()
                        },
                        onFailed = {
                            Toast.makeText(this, "認証に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("NO") { dialog, _ ->
                // NO選択時は旧アカウント（A）の情報で誤ログインされないようクリア
                biometricAuthManager.clearCredentials()
                biometricAuthManager.setPromptDismissedFor(email, true)
                dialog.dismiss()
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 初回登録ダイアログ（email単位でのスキップフラグ管理）
     */
    private fun showBiometricRegistrationDialog(email: String, password: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_biometric_register_title)
            .setMessage(R.string.dialog_biometric_register_message)
            .setPositiveButton("YES") { _, _ ->
                if (!biometricAuthManager.isFingerprintEnrolled()) {
                    // 指紋が端末に未登録の場合は設定画面への誘導ダイアログを表示
                    biometricAuthManager.showEnrollPromptDialog(this, onCancel = {
                        navigateToHome()
                    })
                } else {
                    // 指紋プロンプトを表示
                    biometricAuthManager.authenticate(
                        activity = this,
                        onSuccess = {
                            biometricAuthManager.saveCredentials(email, password)
                            Toast.makeText(this, R.string.toast_biometric_registered, Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        },
                        onError = { _, _ ->
                            navigateToHome()
                        },
                        onFailed = {
                            Toast.makeText(this, "認証に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("NO") { dialog, _ ->
                // 次回以降ダイアログを表示しないようにフラグを保存
                biometricAuthManager.setPromptDismissedFor(email, true)
                dialog.dismiss()
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun clearBordFocus() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(emailEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(passwordEditText.windowToken, 0)
        //フォーカスを外す処理
        emailEditText.clearFocus()
        passwordEditText.clearFocus()
    }
}