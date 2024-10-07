package com.websarva.wings.android.kakeibo

import BaseActivity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.websarva.wings.android.kakeibo.room.payrecord.PayRecordListViewModel
import com.websarva.wings.android.kakeibo.room.payrecord.PaymentAdapter

class PayRecordListActivity :
    BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list) {
    private lateinit var payRecordListViewModel: PayRecordListViewModel
    private lateinit var paymentAdapter: PaymentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        payRecordListViewModel = ViewModelProvider(this)[PayRecordListViewModel::class.java]

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
                paymentAdapter = PaymentAdapter(payRecordList = payments,this,this)
                recyclerView.adapter = paymentAdapter
            } else {
                // paymentsがnullまたは空の場合に適切な処理を追加
                // 例えば、"メンバーがいません"と表示するなど
                paymentAdapter = PaymentAdapter(
                    payRecordList = listOf(),this,this
                )
                recyclerView.adapter = paymentAdapter
            }
        }
    }
}