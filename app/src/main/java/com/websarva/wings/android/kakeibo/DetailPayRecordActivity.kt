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
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class DetailPayRecordActivity : BaseActivity(R.layout.activity_detail_pay_record, R.string.title_detail_pay_record) {
    //画面部品の用意
    private lateinit var payerTextView: TextView
    private lateinit var payDateTextView: TextView
    private lateinit var payPurposeTextView: TextView
    private lateinit var payAmountTextView: TextView
    private lateinit var payDoneCheckTextView: TextView
    private lateinit var payNoteTextView: TextView
    private lateinit var buttonPayRecordUpdate: Button

    //支払い履歴のID
    private var payRecordId = -1

    //ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pay_record)

        setupDrawerAndToolbar()
        
        // 画面部品の取得
        payerTextView = findViewById(R.id.payerTextView)
        payDateTextView = findViewById(R.id.payDateTextView)
        payPurposeTextView = findViewById(R.id.payPurposeTextView)
        payAmountTextView = findViewById(R.id.payAmountTextView)
        payDoneCheckTextView = findViewById(R.id.payDoneCheckTextView)
        payNoteTextView = findViewById(R.id.payNoteTextView)
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)

        //前画面からもらった値を取得
        payRecordId = intent.getLongExtra("PAY_RECORD_ID",-1).toInt()
        val memberId = intent.getLongExtra("MEMBER_ID",-1).toInt()
        val payDate = intent.getStringExtra("PAY_DATE")
        val payPurposeId = intent.getLongExtra("PAY_PURPOSE_ID",-1).toInt()
        val payAmount = intent.getIntExtra("PAY_AMOUNT",-1)
        val isReceptChecked = intent.getBooleanExtra("IS_RECEPT_CHECKED",false)
        val note = intent.getStringExtra("NOTE")
        
        payerTextView.text = databaseHelper.getMemberNameById(memberId)
        payDateTextView.text = payDate
        payPurposeTextView.text = databaseHelper.getPayPurposeNameById(payPurposeId)
        payAmountTextView.text = "${payAmount}円"
        payDoneCheckTextView.text = if(isReceptChecked) "領収済み" else "未受領"
        payNoteTextView.text = note

        buttonPayRecordUpdate.setOnClickListener {
            val intent = Intent(this, UpdatePayRecordActivity::class.java)
            intent.putExtra("PAY_RECORD_ID",payRecordId)
            intent.putExtra("MEMBER_ID",memberId)
            intent.putExtra("PAY_DATE",payDate)
            intent.putExtra("PAY_PURPOSE_ID",payPurposeId)
            intent.putExtra("PAY_AMOUNT",payAmount)
            intent.putExtra("IS_RECEPT_CHECKED",isReceptChecked)
            intent.putExtra("NOTE",note)
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

    // メンバーを削除する処理
    private fun deletePayRecord() {
        val db = DatabaseHelper(this).writableDatabase

        val rowsDeleted = db.delete(
            "payment_history",
            "_id = ?",
            arrayOf(payRecordId.toString())
        )

        if (rowsDeleted > 0) {
            Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
            // 削除成功した場合、親Activityに通知する
            val resultIntent = Intent()
            resultIntent.putExtra("PAY_RECORD_DELETE", true)  // 削除フラグを渡す
            setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
            finish()  // アクティビティを終了し、前の画面に戻る
        } else {
            Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show()
        }
        db.close()
    }
}