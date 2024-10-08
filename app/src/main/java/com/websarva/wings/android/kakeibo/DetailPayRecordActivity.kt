package com.websarva.wings.android.kakeibo

import BaseActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import com.websarva.wings.android.kakeibo.room.payrecord.DetailPayRecordViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailPayRecordActivity :
    BaseActivity(R.layout.activity_detail_pay_record, R.string.title_detail_pay_record) {
    private lateinit var viewModel:DetailPayRecordViewModel

    private lateinit var payerTextView: TextView
    private lateinit var payDateTextView: TextView
    private lateinit var payPurposeTextView: TextView
    private lateinit var payAmountTextView: TextView
    private lateinit var payDoneCheckTextView: TextView
    private lateinit var payNoteTextView: TextView
    private lateinit var buttonPayRecordUpdate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pay_record)

        viewModel = DetailPayRecordViewModel(application)
        val itemId = intent.getStringExtra("item_id")?.toInt()

        setupDrawerAndToolbar()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    viewModel.getPayment(itemId).observe(this) { payment ->
                        if (payment != null) {
                            viewModel.deletePayment(payment)
                            // 削除が完了した後に次のアクティビティに移動
                            val intent = Intent(this, PayRecordListActivity::class.java)
                            startActivity(intent)
                        } else {
                            // paymentがnullの場合のエラーハンドリング
                            Log.e("DetailPayRecordActivity", "Payment data is null")
                            // 必要に応じてエラーメッセージを表示
                        }
                    }
                    true
                }
                else -> false
            }
        }




        // 画面部品の取得
        payerTextView = findViewById(R.id.payerTextView)
        payDateTextView = findViewById(R.id.payDateTextView)
        payPurposeTextView = findViewById(R.id.payPurposeTextView)
        payAmountTextView = findViewById(R.id.payAmountTextView)
        payDoneCheckTextView = findViewById(R.id.payDoneCheckTextView)
        payNoteTextView = findViewById(R.id.payNoteTextView)
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)



        viewModel.getPayment(itemId).observe(this) { payment ->
            if (payment != null) {
                val payDate = formatLongToDateString(payment.paymentDate)
                val state = if (payment.isReceiptChecked) "領収済み" else "未完了"
                val note = payment.notes ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val payerName = viewModel.getPerson(payment.payerId).memberName

                    withContext(Dispatchers.Main) {
                        payerTextView.text = payerName
                        payDateTextView.text = payDate
                        payPurposeTextView.text = payment.purpose
                        payAmountTextView.text = payment.amount.toString()
                        payDoneCheckTextView.text = state
                        payNoteTextView.text = note
                    }
                }
            } else {
                // paymentがnullの場合のエラーハンドリング
                Log.e("DetailPayRecordActivity", "Payment data is null")
                // 必要に応じてエラーメッセージを表示
            }
        }


        buttonPayRecordUpdate.setOnClickListener{
            val intent = Intent(this,UpdatePayRecordActivity::class.java)
            startActivity(intent)
        }
    }

    // メニューをインフレートする
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    private fun formatLongToDateString(timestamp: Long): String {
        // Long型のtimestampをDate型に変換
        val date = Date(timestamp)
        // yyyy/MM/ddフォーマットのSimpleDateFormatを作成
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        // Dateをフォーマットして文字列を返す
        return format.format(date)
    }
}