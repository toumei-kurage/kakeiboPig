package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.websarva.wings.android.kakeibo.room.member.MemberViewModel
import com.websarva.wings.android.kakeibo.room.payRecord.PayRecordViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailPayRecordActivity :
    BaseActivity(R.layout.activity_detail_pay_record, R.string.title_detail_pay_record) {
    private lateinit var payRecordViewModel:PayRecordViewModel
    private lateinit var memberViewModel:MemberViewModel

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

        payRecordViewModel = ViewModelProvider(this)[PayRecordViewModel::class.java]
        memberViewModel = ViewModelProvider(this)[MemberViewModel::class.java]

        val itemId = intent.getStringExtra("item_id")?.toInt()

        setupDrawerAndToolbar()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    payRecordViewModel.getPayment(itemId).observe(this) { payment ->
                        if (payment != null) {
                            payRecordViewModel.deletePayment(payment)
                            // 削除が完了した後に次のアクティビティに移動
                            val intent = Intent(this, PayRecordListActivity::class.java)
                            startActivity(intent)
                            finish()
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



        payRecordViewModel.getPayment(itemId).observe(this) { payment ->
            if (payment != null) {
                val payDate = formatLongToDateString(payment.paymentDate)
                val state = if (payment.isReceiptChecked) "領収済み" else "未完了"
                val note = payment.notes ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val payerName = memberViewModel.getPerson(payment.payerId).memberName

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


        buttonPayRecordUpdate.setOnClickListener {
            val intent = Intent(this, UpdatePayRecordActivity::class.java)

            payRecordViewModel.getPayment(itemId).observe(this) { payment ->
                if (payment != null) {
                    intent.putExtra("支払い明細", payment) // Paymentオブジェクトを直接渡す
                    startActivity(intent)
                    finish()
                }
            }
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