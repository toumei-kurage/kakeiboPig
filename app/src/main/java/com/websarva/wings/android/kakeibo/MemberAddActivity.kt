package com.websarva.wings.android.kakeibo

import android.app.AlertDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper

class MemberAddActivity : BaseActivity(R.layout.activity_member_add, R.string.title_member_add) {
    //画面部品の用意
    private lateinit var memberNameError: TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberAdd: Button

    // ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_add)

        setupDrawerAndToolbar()

        //画面部品取得
        memberNameError = findViewById(R.id.memberName)
        memberNameEditText = findViewById(R.id.memberNameEditText)
        buttonMemberAdd = findViewById(R.id.buttonMemberAdd)

        //メンバーネームのフォーカスが外れた時のバリデーションチェック
        memberNameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.usernameCheck(memberNameEditText)
                if (!result) {
                    memberNameError.error = errorMsg
                    return@OnFocusChangeListener
                }
                memberNameError.error = ""
            }
        }

        buttonMemberAdd.setOnClickListener {
            clearBordFocus()
            val (resultMemberName: Boolean, memberNameMsg: String) = validateHelper.usernameCheck(
                memberNameEditText
            )
            if (!resultMemberName) {
                memberNameError.error = memberNameMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            onSaveButtonClick()
        }

    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(memberNameEditText.windowToken, 0)
        //フォーカスを外す処理
        memberNameEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        memberNameError.error = null
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

    private fun onSaveButtonClick(){
        val db = databaseHelper.writableDatabase
        try{
            val values = ContentValues().apply {
                put("user_id",userID)
                put("member_name",memberNameEditText.text.toString())
            }
            val newRowId = db.insertOrThrow("member",null,values)
            // 成功したらトーストメッセージを表示
            if (newRowId != -1L) {
                Toast.makeText(this, "メンバーが追加されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "データベースに挿入できませんでした", Toast.LENGTH_SHORT).show()
            }
        }catch (e: SQLiteConstraintException){
           dialogHelper.dialogOkOnly("","メンバー名が重複しています。")
        }finally {
            db.close()
        }
    }
}