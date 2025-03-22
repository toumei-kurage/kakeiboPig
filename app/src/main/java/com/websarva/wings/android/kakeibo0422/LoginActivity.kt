package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo0422.helper.DialogHelper
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper

/**
 * ログイン画面
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_login)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        //画面部品の取得
        val emailError = findViewById<TextInputLayout>(R.id.email)
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordError = findViewById<TextInputLayout>(R.id.password)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val signUpButton: Button = findViewById(R.id.SignUpButton)

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
            login(email, password)
        }

        signUpButton.setOnClickListener {
            clearBordFocus()
            val intent = Intent(this, UserAddActivity::class.java)
            startActivity(intent)
            finish() // このアクティビティを終了して、戻れないようにする
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ログイン成功
                    Toast.makeText(this, "ログイン成功", Toast.LENGTH_SHORT).show()
                    // 次の画面に遷移する処理
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // このアクティビティを終了して、戻れないようにする
                } else {
                    // ログイン失敗
                    dialogHelper.dialogOkOnly("ログイン失敗","ユーザーIDまたはパスワードが間違っています。")
                }
            }
    }

    private fun clearBordFocus(){
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
}