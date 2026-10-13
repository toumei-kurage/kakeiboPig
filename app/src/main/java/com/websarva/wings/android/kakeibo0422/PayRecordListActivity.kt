package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PayRecordListActivity : BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list) {
    // 画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    private lateinit var buttonRefinement: Button
    private lateinit var payRecordAdapter: PayRecordAdapter
    private lateinit var textViewTotalAmount: TextView

    private val firestore = FirebaseFirestore.getInstance()

    // 目的ID -> 目的名、メンバーID -> メンバー名のインメモリキャッシュ
    private val purposeMap = mutableMapOf<String, String>()
    private val memberMap = mutableMapOf<String, String>()

    // 現在の絞り込み条件を保持（再読み込み用）
    private var currentMemberId: String? = null
    private var currentStartDate: String? = null
    private var currentFinishDate: String? = null
    private var currentPayDone: String? = null
    private var currentPayPurposeId: String? = null

    // ✅ Activity Result API を使ってコールバックを登録
    @RequiresApi(Build.VERSION_CODES.O)
    private val editPayRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            // 削除または更新された場合に再読み込み
            if (data?.getBooleanExtra("PAY_RECORD_DELETE", false) == true ||
                data?.getBooleanExtra("PAY_RECORD_UPDATED", false) == true) {
                reloadCurrentData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        // 画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)
        buttonRefinement = findViewById(R.id.buttonRefinement)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        // ListAdapterの定義に合わせてクリックコールバックを渡して初期化
        payRecordAdapter = PayRecordAdapter { item ->
            // タップ時に編集・詳細画面へ遷移
            val intent = Intent(this, PayRecordUpdateActivity::class.java).apply {
                putExtra("PAY_RECORD_ID", item.id)
                putExtra("USER_ID", item.userId)
                putExtra("MEMBER_ID", item.memberId)
                putExtra("PAY_PURPOSE_ID", item.payPurposeId)
                putExtra("PAY_DATE", item.payDate)
                putExtra("PAY_AMOUNT", item.payAmount)
                putExtra("IS_RECEPT_CHECKED", item.isReceptChecked)
                putExtra("NOTE", item.note)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launchEditPayRecord(intent)
            } else {
                startActivity(intent)
            }
        }
        recyclerView.adapter = payRecordAdapter

        // マスタデータ（目的・メンバー）を先読みしてから一覧を取得
        loadMasterDataAndFetchList()

        // 追加画面への遷移（一覧を破棄しないよう finish() は不要）
        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, PayRecordAddActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonRefinement.setOnClickListener {
            // フラグメントを表示
            val fragment = PayRecordListRefinementFragment()
            fragment.show(supportFragmentManager, "PayRecordListRefinementFragment")
        }
    }

    override fun onResume() {
        super.onResume()
        // 新規登録画面から戻ってきた際などに一覧を最新化
        if (purposeMap.isNotEmpty()) {
            reloadCurrentData()
        }
    }

    // ✅ 他クラスから Activity を起動するためのヘルパー
    @RequiresApi(Build.VERSION_CODES.O)
    fun launchEditPayRecord(intent: Intent) {
        editPayRecordLauncher.launch(intent)
    }

    /**
     * マスタデータ（目的・メンバー）を一括事前ロードし、N+1クエリを防止する
     */
    private fun loadMasterDataAndFetchList() {
        val currentUserId = userID ?: return

        // 1. 支払い目的のロード
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", currentUserId)
            .get()
            .addOnSuccessListener { purposeSnapshot ->
                purposeMap.clear()
                for (doc in purposeSnapshot) {
                    purposeMap[doc.id] = doc.getString("pay_purpose_name") ?: "未分類"
                }

                // 2. メンバーのロード
                firestore.collection("members")
                    .whereEqualTo("user_id", currentUserId)
                    .get()
                    .addOnSuccessListener { memberSnapshot ->
                        memberMap.clear()
                        for (doc in memberSnapshot) {
                            memberMap[doc.id] = doc.getString("member_name") ?: "未設定"
                        }

                        // マスタ準備完了後に初回のデータ取得を実行
                        applyRefinement(null, null, null, null, null)
                    }
                    .addOnFailureListener {
                        applyRefinement(null, null, null, null, null)
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "マスタデータ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                applyRefinement(null, null, null, null, null)
            }
    }

    fun applyRefinement(memberId: String?, startDate: String?, finishDate: String?, payDone: String?, payPurposeId: String?) {
        currentMemberId = memberId
        currentStartDate = startDate
        currentFinishDate = finishDate
        currentPayDone = payDone
        currentPayPurposeId = payPurposeId

        fetchFilterData(memberId, startDate, finishDate, payDone, payPurposeId)
    }

    private fun reloadCurrentData() {
        fetchFilterData(currentMemberId, currentStartDate, currentFinishDate, currentPayDone, currentPayPurposeId)
    }

    /**
     * 非同期でFirestoreから取得し、PayRecordUiModelを生成してListAdapterへ反映
     */
    private fun fetchFilterData(memberId: String?, startDate: String?, finishDate: String?, payDoneString: String?, payPurposeId: String?) {
        val query = createQuery(memberId, startDate, finishDate, payDoneString, payPurposeId)

        query.get()
            .addOnSuccessListener { querySnapshot ->
                // ✅ 修正点2: PayRecordUiModel のリストを作成
                val newPayRecordList = mutableListOf<PayRecordUiModel>()

                for (document in querySnapshot) {
                    val pPurposeId = document.getString("pay_purpose_id") ?: ""
                    val mId = document.getString("member_id") ?: ""

                    // キャッシュMapから名称を解決
                    val pPurposeName = purposeMap[pPurposeId] ?: "未設定"

                    val payRecord = PayRecordUiModel(
                        id = document.id,
                        userId = document.getString("user_id") ?: "",
                        memberId = mId,
                        payPurposeId = pPurposeId,
                        payPurposeName = pPurposeName,
                        payDate = document.getString("payment_date") ?: "",
                        payAmount = document.getLong("amount")?.toInt() ?: 0,
                        isReceptChecked = document.getBoolean("is_recept_checked") == true,
                        note = document.getString("note") ?: ""
                    )
                    newPayRecordList.add(payRecord)
                }

                // 日付降順ソート
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                newPayRecordList.sortByDescending {
                    try {
                        dateFormat.parse(it.payDate) ?: Date(0)
                    } catch (e: Exception) {
                        Date(0)
                    }
                }

                // 合計金額の更新
                val totalAmount = newPayRecordList.sumOf { it.payAmount }
                textViewTotalAmount.text = "合計金額：" + getString(R.string.formatted_number, totalAmount)

                if (newPayRecordList.isEmpty()) {
                    Toast.makeText(this, "支払い履歴が登録されていません。", Toast.LENGTH_SHORT).show()
                }

                // ✅ 修正点3: ListAdapter標準の submitList() を呼んで差分アニメーション更新
                payRecordAdapter.submitList(newPayRecordList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createQuery(memberId: String?, startDate: String?, finishDate: String?, payDoneString: String?, payPurposeId: String?): Query {
        val payDone = if (payDoneString != null) (payDoneString == "領収済み") else null
        var query: Query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)

        if (memberId != null) {
            query = query.whereEqualTo("member_id", memberId)
        }
        if (payPurposeId != null) {
            query = query.whereEqualTo("pay_purpose_id", payPurposeId)
        }
        if (startDate != null && finishDate != null) {
            query = query
                .whereGreaterThanOrEqualTo("payment_date", startDate)
                .whereLessThanOrEqualTo("payment_date", finishDate)
        }
        if (payDone != null) {
            query = query.whereEqualTo("is_recept_checked", payDone)
        }

        return query
    }
}
