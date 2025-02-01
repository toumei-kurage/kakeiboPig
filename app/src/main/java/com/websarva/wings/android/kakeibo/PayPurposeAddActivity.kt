package com.websarva.wings.android.kakeibo

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
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
            val (resultPayPurposeName: Boolean, payPurposeNameMsg: String) = validateHelper.payPurposeNameCheck(payPurposeNameEditText)
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

    private fun onSaveButtonClick() {
        val firestore = FirebaseFirestore.getInstance()

        // 現在ログインしているユーザーのIDを取得 (Firebase Authenticationを使用している場合)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "ユーザーがログインしていません", Toast.LENGTH_SHORT).show()
            return
        }

        // メンバー名とユーザーIDを取得
        val payPurposeName = payPurposeNameEditText.text.toString()

        // 「members」コレクションから、user_idとmember_nameの組み合わせで既に存在するかチェック
        val query = firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_name", payPurposeName)

        query.get()
            .addOnSuccessListener { querySnapshot ->
                // クエリ結果が空ならば新しいメンバーを追加
                if (querySnapshot.isEmpty) {
                    // Firestoreに追加するデータ
                    val payPurposeData = hashMapOf(
                        "user_id" to userID,
                        "pay_purpose_name" to payPurposeName
                    )

                    // Firestoreの「payPurposes」コレクションにデータを追加
                    firestore.collection("payPurposes")
                        .add(payPurposeData)
                        .addOnSuccessListener { documentReference ->
                            // 成功した場合の処理
                            Toast.makeText(this, "支払い目的が追加されました", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // エラーが発生した場合の処理
                            Toast.makeText(this, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // 既に同じ組み合わせのデータが存在する場合
                    Toast.makeText(this, "この支払い目的は既に存在します", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // クエリ実行時のエラー処理
                Toast.makeText(this, "データベースの読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}