package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.room.payrecord.PayRecordListViewModel
import com.websarva.wings.android.kakeibo.room.payrecord.Payment
import com.websarva.wings.android.kakeibo.room.payrecord.PaymentAdapter

class PayRecordListActivity :
    BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list),PaymentAdapter.OnPaymentClickListener {
    private lateinit var payRecordListViewModel: PayRecordListViewModel
    private lateinit var paymentAdapter: PaymentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton

    private lateinit var dialogHelper: DialogHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        payRecordListViewModel = ViewModelProvider(this)[PayRecordListViewModel::class.java]
        dialogHelper = DialogHelper(this)

        //画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, AddPayRecordActivity::class.java)
            startActivity(intent)
        }

        payRecordListViewModel.getPaymentsByUserId(userID).observe(this) { payments ->
            if (payments != null && payments.isNotEmpty()) {
                paymentAdapter = PaymentAdapter(payRecordList = payments,this,this,this)
                recyclerView.adapter = paymentAdapter
            } else {
                // paymentsがnullまたは空の場合に適切な処理を追加
                paymentAdapter = PaymentAdapter(payRecordList = listOf(),this,this,this)
                recyclerView.adapter = paymentAdapter
                dialogHelper.dialogOkOnly("","支払い明細が未登録です")
            }
        }
    }

    override fun onPaymentClick(payment: Payment) {
        // 画面遷移の処理
        val intent = Intent(this, DetailPayRecordActivity::class.java)
        intent.putExtra("item_id", payment.id.toString()) // 必要なデータを渡す
        startActivity(intent)
    }
}