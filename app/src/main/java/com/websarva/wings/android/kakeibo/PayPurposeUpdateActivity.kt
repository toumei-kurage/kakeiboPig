package com.websarva.wings.android.kakeibo

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper

class PayPurposeUpdateActivity :BaseActivity(R.layout.activity_pay_purpose_update, R.string.title_pay_purpose_update)  {
    //画面部品の用意
    private lateinit var payPurposeNameError: TextInputLayout
    private lateinit var payPurposeNameEditText: EditText
    private lateinit var buttonPayPurposeUpdate: Button

    // ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    private var payPurposeId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_update)

        setupDrawerAndToolbar()

        //画面部品取得
        payPurposeNameError = findViewById(R.id.payPurposeName)
        payPurposeNameEditText = findViewById(R.id.payPurposeNameEditText)
        buttonPayPurposeUpdate = findViewById(R.id.buttonPayPurposeUpdate)

        // 渡されたデータを受け取る
        payPurposeId = intent.getLongExtra("PAY_PURPOSE_ID", -1)
        val payPurposeName = intent.getStringExtra("PAY_PURPOSE_NAME")
        
        payPurposeNameEditText.setText(payPurposeName)

        //メンバーネームのフォーカスが外れた時のバリデーションチェック
        payPurposeNameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.usernameCheck(payPurposeNameEditText)
                if (!result) {
                    payPurposeNameError.error = errorMsg
                    return@OnFocusChangeListener
                }
                payPurposeNameError.error = ""
            }
        }

        buttonPayPurposeUpdate.setOnClickListener {
            clearBordFocus()
            val (resultPayPurposeName: Boolean, payPurposeNameMsg: String) = validateHelper.usernameCheck(
                payPurposeNameEditText
            )
            if (!resultPayPurposeName) {
                payPurposeNameError.error = payPurposeNameMsg
                return@setOnClickListener
            }

            clearErrorMessage()
            updatePayPurpose()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()  // 削除確認ダイアログを表示
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // メニュー（ActionBar）の作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(payPurposeNameEditText.windowToken, 0)
        //フォーカスを外す処理
        payPurposeNameEditText.clearFocus()
    }

    private fun clearErrorMessage(){
        payPurposeNameError.error = null
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

    private fun updatePayPurpose() {
        // 入力された値を取得
        val newPayPurposeName = payPurposeNameEditText.text.toString()

        // データベースの更新処理
        val db = DatabaseHelper(this).writableDatabase
        try{
            val values = ContentValues().apply {
                put("user_id", userID)
                put("pay_purpose_name", newPayPurposeName)
            }
            val rowsAffected = db.update(
                "payment_purpose",
                values,
                "_id = ?",
                arrayOf(payPurposeId.toString())
            )
            if (rowsAffected > 0) {
                Toast.makeText(this, "更新されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "更新できませんでした", Toast.LENGTH_SHORT).show()
            }
        }catch (e: SQLiteConstraintException){
            dialogHelper.dialogOkOnly("","支払い目的名が重複しています。")
        }finally {
            db.close()
        }
    }

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ ->
                deletePayPurpose()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    // メンバーを削除する処理
    private fun deletePayPurpose() {
        val db = DatabaseHelper(this).writableDatabase

        try{
            val rowsDeleted = db.delete(
                "payment_purpose",
                "_id = ?",
                arrayOf(payPurposeId.toString())
            )

            if (rowsDeleted > 0) {
                Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
                // 削除成功した場合、親Activityに通知する
                val resultIntent = Intent()
                resultIntent.putExtra("PAY_PURPOSE_DELETED", true)  // 削除フラグを渡す
                setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
                finish()  // アクティビティを終了し、前の画面に戻る
            } else {
                Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show()
            }
        }catch (e:SQLiteConstraintException) {
            dialogHelper.dialogOkOnly("","支払い履歴に登録済みの支払い目的は削除できません")
        }finally {
            db.close()
        }
    }
}