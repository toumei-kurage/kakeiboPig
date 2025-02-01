package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class PayRecordDetailActivity : BaseActivity(R.layout.activity_pay_record_detail, R.string.title_detail_pay_record) {
    // 画面部品の用意
    private lateinit var payerTextView: TextView
    private lateinit var payDateTextView: TextView
    private lateinit var payPurposeTextView: TextView
    private lateinit var payAmountTextView: TextView
    private lateinit var payDoneCheckTextView: TextView
    private lateinit var payNoteTextView: TextView
    private lateinit var buttonPayRecordUpdate: Button

    // 支払い履歴のID
    private var payRecordId = ""
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_detail)

        setupDrawerAndToolbar()

        // 画面部品の取得
        payerTextView = findViewById(R.id.payerTextView)
        payDateTextView = findViewById(R.id.payDateTextView)
        payPurposeTextView = findViewById(R.id.payPurposeTextView)
        payAmountTextView = findViewById(R.id.payAmountTextView)
        payDoneCheckTextView = findViewById(R.id.payDoneCheckTextView)
        payNoteTextView = findViewById(R.id.payNoteTextView)
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)

        // 前画面からもらった値を取得
        payRecordId = intent.getStringExtra("PAY_RECORD_ID") ?: ""
        val memberId = intent.getStringExtra("MEMBER_ID") ?: ""
        val payDate = intent.getStringExtra("PAY_DATE")
        val payPurposeId = intent.getStringExtra("PAY_PURPOSE_ID") ?: ""
        val payAmount = intent.getIntExtra("PAY_AMOUNT", -1)
        val isReceptChecked = intent.getBooleanExtra("IS_RECEPT_CHECKED", false)
        val note = intent.getStringExtra("NOTE")

        // Firestoreからデータを取得
        getMemberNameById(memberId) { memberName ->
            payerTextView.text = memberName
        }

        payDateTextView.text = payDate
        getPayPurposeNameById(payPurposeId) { payPurposeName ->
            payPurposeTextView.text = payPurposeName
        }

        payAmountTextView.text = "${payAmount}円"
        payDoneCheckTextView.text = if (isReceptChecked) "領収済み" else "未受領"
        payNoteTextView.text = note

        buttonPayRecordUpdate.setOnClickListener {
            val intent = Intent(this, PayRecordUpdateActivity::class.java)
            intent.putExtra("PAY_RECORD_ID", payRecordId)
            intent.putExtra("MEMBER_ID", memberId)
            intent.putExtra("PAY_DATE", payDate)
            intent.putExtra("PAY_PURPOSE_ID", payPurposeId)
            intent.putExtra("PAY_AMOUNT", payAmount)
            intent.putExtra("IS_RECEPT_CHECKED", isReceptChecked)
            intent.putExtra("NOTE", note)
            startActivity(intent)
            finish()
        }
    }

    // メニュー（ActionBar）の作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
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

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ ->
                deletePayRecord()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    // 支払い履歴を削除する処理
    private fun deletePayRecord() {
        firestore.collection("payment_history")
            .document(payRecordId)  // payRecordIdでドキュメントを指定
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
                // 削除成功した場合、親Activityに通知する
                val resultIntent = Intent()
                resultIntent.putExtra("PAY_RECORD_DELETE", true)  // 削除フラグを渡す
                setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
                finish()  // アクティビティを終了し、前の画面に戻る
            }
            .addOnFailureListener {
                Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestoreからメンバー名を取得するメソッド
    private fun getMemberNameById(memberId: String, callback: (String) -> Unit) {
        firestore.collection("members")
            .document(memberId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val memberName = document.getString("member_name") ?: ""
                    callback(memberName)
                } else {
                    callback("")
                }
            }
            .addOnFailureListener {
                callback("")
            }
    }

    // Firestoreから支払い目的名を取得するメソッド
    private fun getPayPurposeNameById(payPurposeId: String, callback: (String) -> Unit) {
        firestore.collection("payPurposes")
            .document(payPurposeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val payPurposeName = document.getString("pay_purpose_name") ?: ""
                    callback(payPurposeName)
                } else {
                    callback("")
                }
            }
            .addOnFailureListener {
                callback("")
            }
    }
}
