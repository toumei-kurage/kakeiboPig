package com.websarva.wings.android.kakeibo

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

class PayPurposeAddActivity :
    BaseActivity(R.layout.activity_pay_purpose_add, R.string.title_pay_purpose_add) {
    //画面部品の用意
    private lateinit var payPurposeNameEditText: EditText
    private lateinit var payPurposeNameError: TextInputLayout
    private lateinit var buttonPayPurposeAdd: Button

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)
    private val databaseHelper = DatabaseHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_add)

        setupDrawerAndToolbar()

        //画面部品の取得
        payPurposeNameEditText = findViewById(R.id.payPurposeNameEditText)
        payPurposeNameError = findViewById(R.id.payPurposeName)
        buttonPayPurposeAdd = findViewById(R.id.buttonPayPurposeAdd)

        payPurposeNameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                //フォーカスが外れた時の処理
                val (result, errorMessage) = validateHelper.payPurposeNameCheck(
                    payPurposeNameEditText
                )
                if (!result) {
                    payPurposeNameError.error = errorMessage
                    return@OnFocusChangeListener
                }
                payPurposeNameError.error = null
            }
        }

        buttonPayPurposeAdd.setOnClickListener {
            clearBordFocus()
            val (resultPayPurposeName: Boolean, payPurposeNameMsg: String) = validateHelper.payPurposeNameCheck(
                payPurposeNameEditText
            )
            if (!resultPayPurposeName) {
                payPurposeNameError.error = payPurposeNameMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            onSaveButtonClick()
        }

    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(payPurposeNameEditText.windowToken, 0)
        //フォーカスを外す処理
        payPurposeNameEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        payPurposeNameError.error = null
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

    private fun onSaveButtonClick() {
        val db = databaseHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put("user_id", userID)
                put("pay_purpose_name", payPurposeNameEditText.text.toString())
            }

            val newRowId = db.insertOrThrow("payment_purpose", null, values)

            // 成功したらトーストメッセージを表示
            if (newRowId != -1L) {
                Toast.makeText(this, "支払い目的が追加されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "データベースに挿入できませんでした", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SQLiteConstraintException) {
            dialogHelper.dialogOkOnly("", "メンバー名が重複しています。")
        } finally {
            db.close()
        }
    }
}