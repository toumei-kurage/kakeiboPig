package com.websarva.wings.android.kakeibo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.websarva.wings.android.kakeibo.helper.ValidateHelper

class PayPurposeUpdateActivity :BaseActivity(R.layout.activity_pay_purpose_update, R.string.title_pay_purpose_update)  {
    //画面部品の用意
    private lateinit var payPurposeNameError: TextInputLayout
    private lateinit var payPurposeNameEditText: EditText
    private lateinit var buttonPayPurposeUpdate: Button

    // Firestoreインスタンス
    private val firestore = FirebaseFirestore.getInstance()

    // メンバーID
    private var payPurposeId: String = ""

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_update)

        setupDrawerAndToolbar()

        //画面部品取得
        payPurposeNameError = findViewById(R.id.payPurposeName)
        payPurposeNameEditText = findViewById(R.id.payPurposeNameEditText)
        buttonPayPurposeUpdate = findViewById(R.id.buttonPayPurposeUpdate)

        // 渡されたデータを受け取る
        payPurposeId = intent.getStringExtra("PAY_PURPOSE_ID") ?: ""
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
                payPurposeNameError.error = null
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
            checkForDuplicateAndUpdate()
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

    // 重複チェックと更新処理
    private fun checkForDuplicateAndUpdate() {
        val newPayPurposeName = payPurposeNameEditText.text.toString()

        // 重複をチェックするクエリを作成
        val query: Query = firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)  // user_idを基に検索
            .whereEqualTo("pay_purpose_name", newPayPurposeName)  // pay_purpose_nameが一致するものを検索

        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // 重複がない場合、更新処理を実行
                    updatePayPurpose(newPayPurposeName)
                } else {
                    // 重複がある場合
                    Toast.makeText(this, "この支払い目的はすでに存在します", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "重複チェックに失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePayPurpose(newPayPurposeName: String) {
        val payPurposeRef = firestore.collection("payPurposes").document(payPurposeId)

        val updatedData = hashMapOf(
            "pay_purpose_name" to newPayPurposeName,
            "user_id" to userID
        )

        payPurposeRef.set(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "更新されました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "更新できませんでした: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ -> deletePayPurpose() }
            .setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }

        val alert = builder.create()
        alert.show()
    }

    // 支払い目的を削除する処理
    private fun deletePayPurpose() {
        // payPurposeIdを使ってpayment_historyコレクションを検索
        firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_id", payPurposeId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // もしpayment_historyコレクション内にmember_idが参照されているドキュメントがある場合
                if (!querySnapshot.isEmpty) {
                    // 支払い目的が支払い履歴に参照されているので削除不可
                    Toast.makeText(this, "この支払い目的は支払い履歴に参照されています。削除できません。", Toast.LENGTH_SHORT).show()
                } else {
                    // payment_historyコレクションに参照されていない場合、メンバーを削除
                    val payPurposeRef = firestore.collection("payPurposes").document(payPurposeId)

                    payPurposeRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
                            // 削除成功した場合、親Activityに通知する
                            val resultIntent = Intent()
                            resultIntent.putExtra("PAY_PURPOSE_DELETED", true)  // 削除フラグを渡す
                            setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
                            finish()  // アクティビティを終了し、前の画面に戻る
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "削除できませんでした: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "エラーが発生しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}