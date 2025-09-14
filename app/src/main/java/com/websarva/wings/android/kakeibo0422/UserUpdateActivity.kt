package com.websarva.wings.android.kakeibo0422

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.EmailAuthProvider
import com.websarva.wings.android.kakeibo0422.helper.DialogHelper
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper

class UserUpdateActivity : BaseActivity(R.layout.activity_user_update, R.string.title_user_update) {
    //画面部品の用意
    private lateinit var emailEditText: EditText
    private lateinit var emailError: TextInputLayout
    private lateinit var passwordEditText: EditText
    private lateinit var passwordError: TextInputLayout
    private lateinit var buttonUpdate: Button

    //Firebase関連の変数
    private val auth = FirebaseAuth.getInstance()
    private val currentUser: FirebaseUser? = auth.currentUser

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_update)

        setupDrawerAndToolbar()

        // 画面部品の取得
        emailEditText = findViewById(R.id.emailEditText)
        emailError = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordError = findViewById(R.id.password)
        buttonUpdate = findViewById(R.id.buttonUpdate)

        // ユーザー情報を事前にセット
        if (currentUser != null) {
            emailEditText.setText(currentUser.email)
        }

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

        buttonUpdate.setOnClickListener {
            clearBordFocus()
            // すべての入力項目のバリデーションチェック
            val (resultEmail: Boolean, emailMsg: String) = validateHelper.emailCheck(emailEditText)
            val (resultPassword: Boolean, passwordMsg) = validateHelper.passwordCheck(passwordEditText)

            emailError.error = if(!resultEmail) emailMsg else null
            passwordError.error = if(!resultPassword) passwordMsg else null

            if (!(resultEmail && resultPassword)) {
                return@setOnClickListener
            }

            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // 再認証用のダイアログを表示
            showReAuthenticationDialog(email, password)
        }
    }

    // ユーザー情報の更新処理
    private fun updateUserProfile(email: String, password: String,oldPassword:String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // 再認証処理（メールアドレス更新時には再認証が必要）
        val credentials = EmailAuthProvider.getCredential(user.email!!, oldPassword)
        user.reauthenticate(credentials).addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    // メールアドレスの更新
                    if (email != user.email) {  // メールが変更されている場合のみ更新
                        @Suppress("DEPRECATION")
                        user.updateEmail(email)
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful)
                                    Toast.makeText(this, "メールアドレス更新に失敗しました", Toast.LENGTH_SHORT).show()
                            }
                    }

                    // パスワードの更新
                    if (password.isNotEmpty()) {
                        user.updatePassword(password)
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful)
                                    Toast.makeText(this, "パスワード更新に失敗しました", Toast.LENGTH_SHORT).show()
                            }
                    }

                    dialogHelper.dialogOkOnly("","アカウント情報を更新しました。")
                } else {
                    Toast.makeText(this, "再認証に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
    }




    private fun clearBordFocus() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(emailEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(passwordEditText.windowToken, 0)
        //フォーカスを外す処理
        emailEditText.clearFocus()
        passwordEditText.clearFocus()
    }

    // 再認証のダイアログを表示する
    private fun showReAuthenticationDialog(newEmail:String,newPassword: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("再認証")

        // 再認証用のメールアドレスとパスワードの入力フィールド
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val emailInput = EditText(this)
        emailInput.hint = "Email"
        emailInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        layout.addView(emailInput)

        val passwordInput = EditText(this)
        passwordInput.hint = "Password"
        passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(passwordInput)

        builder.setView(layout)

        builder.setPositiveButton("OK") { _, _ ->
            //すべての入力項目のバリデーションチェック
            val resultEmail = validateHelper.emailCheck(emailInput).first
            val resultPassword = validateHelper.passwordCheck(passwordInput).first

            if (!(resultEmail && resultPassword)) {
                Toast.makeText(this,"ユーザーIDまたはパスワードが間違っています。",Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            updateUserProfile(newEmail,newPassword,passwordInput.text.toString())
        }

        builder.setNegativeButton("キャンセル", null)

        builder.show()
    }
}
