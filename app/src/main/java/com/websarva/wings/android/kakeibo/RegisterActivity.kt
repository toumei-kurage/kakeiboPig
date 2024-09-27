package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

/**
 * アカウント作成画面
 * １家族につき１アカウント作成予定
 */
class RegisterActivity : AppCompatActivity() {

    // FirebaseAuthのインスタンスを準備
    private lateinit var auth: FirebaseAuth

    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ToolbarをActionBarとして設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_register)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarに戻るボタンを有効化
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        // FirebaseAuthのインスタンスを取得
        auth = FirebaseAuth.getInstance()

        // UIの要素を取得
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val usernameError = findViewById<TextInputLayout>(R.id.username)
        val emailError = findViewById<TextInputLayout>(R.id.email)
        val passwordError = findViewById<TextInputLayout>(R.id.password)

        usernameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.usernameCheck(usernameEditText)
                if (!result) {
                    usernameError.error = errorMsg
                    return@OnFocusChangeListener
                }
                usernameError.error = ""
            }
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

        // ボタンが押された時の処理
        buttonRegister.setOnClickListener {
            clearBordFocus()
            //すべての入力項目のバリデーションチェック
            val (resultUsername: Boolean, usernameMsg: String) = validateHelper.usernameCheck(
                usernameEditText
            )
            val (resultEmail: Boolean, emailMsg: String) = validateHelper.emailCheck(emailEditText)
            val (resultPassword: Boolean, passwordMsg) = validateHelper.passwordCheck(passwordEditText)

            if (!(resultUsername && resultEmail && resultPassword)) {
                usernameError.error = usernameMsg
                emailError.error = emailMsg
                passwordError.error = passwordMsg
                return@setOnClickListener
            }

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Firebaseでユーザー登録
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 登録成功
                        Toast.makeText(this, "登録成功", Toast.LENGTH_SHORT).show()
                        // 次の画面に進むなどの処理を書く
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // このアクティビティを終了して、戻れないようにする

                    } else {
                        // エラー処理
                        dialogHelper.dialogOkOnly("","登録に失敗しました")
                    }
                }
        }
    }

    // 戻るアイコンがタップされた時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // MainActivityに遷移する処理
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun clearBordFocus(){
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(usernameEditText.windowToken,0)
        inputMethodManager.hideSoftInputFromWindow(emailEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(passwordEditText.windowToken, 0)
        //フォーカスを外す処理
        usernameEditText.clearFocus()
        emailEditText.clearFocus()
        passwordEditText.clearFocus()
    }
}