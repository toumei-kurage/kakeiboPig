package com.websarva.wings.android.kakeibo

import BaseActivity
import android.os.Bundle
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

        setupDrawerAndToolbar()

        viewModel = DetailPayRecordViewModel(application)

        // 画面部品の取得
        payerTextView = findViewById(R.id.payerTextView)
        payDateTextView = findViewById(R.id.payDateTextView)
        payPurposeTextView = findViewById(R.id.payPurposeTextView)
        payAmountTextView = findViewById(R.id.payAmountTextView)
        payDoneCheckTextView = findViewById(R.id.payDoneCheckTextView)
        payNoteTextView = findViewById(R.id.payNoteTextView)
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)

        val itemId = intent.getStringExtra("item_id")?.toInt()
        viewModel.getPayment(itemId).observe(this) { payment ->
            val payDate = formatLongToDateString(payment.paymentDate)
            val state = if (payment.isReceiptChecked) "領収済み" else "未完了"
            val note = if(payment.notes == "") "特になし" else payment.notes

            CoroutineScope(Dispatchers.IO).launch {
                // データ取得などのバックグラウンド処理
                val payerName = viewModel.getPerson(payment.payerId).memberName

                // UI更新はメインスレッドで行う
                withContext(Dispatchers.Main) {
                    payerTextView.text = payerName
                    payDateTextView.text = payDate
                    payPurposeTextView.text = payment.purpose
                    payAmountTextView.text = payment.amount.toString()
                    payDoneCheckTextView.text = state
                    payNoteTextView.text = note
                }
            }
        }


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