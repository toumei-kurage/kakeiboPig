package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class RecurringPaymentListActivity : BaseActivity(R.layout.activity_recurring_payment_list, R.string.title_recurring_payment_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonAdd: FloatingActionButton
    private lateinit var textViewTotal: TextView
    private lateinit var emptyView: TextView
    private lateinit var adapter: RecurringPaymentAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val purposeMap = mutableMapOf<String, String>()

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPurposesAndFetchList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_payment_list)

        setupDrawerAndToolbar()

        recyclerView = findViewById(R.id.recyclerView)
        buttonAdd = findViewById(R.id.buttonRecurringPaymentAdd)
        textViewTotal = findViewById(R.id.textViewTotalRecurringAmount)
        emptyView = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        adapter = RecurringPaymentAdapter { item ->
            val intent = Intent(this, RecurringPaymentUpdateActivity::class.java).apply {
                putExtra("RECURRING_ID", item.id)
                putExtra("MEMBER_ID", item.memberId)
                putExtra("PAY_PURPOSE_ID", item.payPurposeId)
                putExtra("PAYMENT_DAY", item.paymentDay)
                putExtra("AMOUNT", item.amount)
                putExtra("IS_RECEPT_CHECKED", item.isReceptChecked)
                putExtra("NOTE", item.note)
            }
            editLauncher.launch(intent)
        }
        recyclerView.adapter = adapter

        buttonAdd.setOnClickListener {
            val intent = Intent(this, RecurringPaymentAddActivity::class.java)
            editLauncher.launch(intent)
        }

        loadPurposesAndFetchList()
    }

    private fun loadPurposesAndFetchList() {
        val currentUserId = userID.takeIf { it.isNotEmpty() } ?: return

        firestore.collection("payPurposes")
            .whereEqualTo("user_id", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                purposeMap.clear()
                for (doc in snapshot) {
                    purposeMap[doc.id] = doc.getString("pay_purpose_name") ?: "未分類"
                }
                fetchRecurringPayments()
            }
            .addOnFailureListener {
                fetchRecurringPayments()
            }
    }

    private fun fetchRecurringPayments() {
        val currentUserId = userID.takeIf { it.isNotEmpty() } ?: return

        firestore.collection("recurring_payments")
            .whereEqualTo("user_id", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<RecurringPaymentUiModel>()
                for (doc in snapshot) {
                    val pId = doc.getString("pay_purpose_id") ?: ""
                    list.add(
                        RecurringPaymentUiModel(
                            id = doc.id,
                            userId = doc.getString("user_id") ?: "",
                            memberId = doc.getString("member_id") ?: "",
                            payPurposeId = pId,
                            payPurposeName = purposeMap[pId] ?: "未設定",
                            paymentDay = (doc.getLong("payment_day") ?: 1L).toInt(),
                            amount = (doc.getLong("amount") ?: 0L).toInt(),
                            isReceptChecked = doc.getBoolean("is_recept_checked") ?: false,
                            note = doc.getString("note") ?: ""
                        )
                    )
                }

                // 支払日（1〜31）の昇順でソート
                list.sortBy { it.paymentDay }

                val totalAmount = list.sumOf { it.amount }
                textViewTotal.text = "毎月の合計額: " + getString(R.string.formatted_number, totalAmount)

                if (list.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.submitList(list)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "固定費一覧の取得に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}