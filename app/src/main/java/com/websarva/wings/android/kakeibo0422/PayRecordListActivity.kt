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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PayRecordListActivity : BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list) {
    // 画面部品
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    private lateinit var buttonRefinement: Button
    private lateinit var payRecordAdapter: PayRecordAdapter
    private lateinit var textViewTotalAmount: TextView

    private val firestore = FirebaseFirestore.getInstance()

    // 目的ID -> 目的名、メンバーID -> メンバー名のインメモリキャッシュ
    private val purposeMap = mutableMapOf<String, String>()
    private val memberMap = mutableMapOf<String, String>()

    // Firestoreから取得した全件のインメモリキャッシュ
    private var allPayRecords: List<PayRecordUiModel> = emptyList()

    // 現在の絞り込み条件（ローカルフィルタ用）
    private var currentMemberId: String? = null
    private var currentStartDate: String? = null
    private var currentFinishDate: String? = null
    private var currentPayDone: String? = null
    private var currentPayPurposeId: String? = null

    // Activity Result API を使ってコールバックを登録
    @RequiresApi(Build.VERSION_CODES.O)
    private val editPayRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            // 削除または更新された場合はリモートから再取得
            if (data?.getBooleanExtra("PAY_RECORD_DELETE", false) == true ||
                data?.getBooleanExtra("PAY_RECORD_UPDATED", false) == true ||
                data?.getBooleanExtra("IS_DATA_CHANGED", false) == true) {
                fetchRecordsFromRemote()
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

        // リストビューの区切り線
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        // ListAdapterの初期化
        payRecordAdapter = PayRecordAdapter { item ->
            val intent = Intent(this, PayRecordDetailActivity::class.java).apply {
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

        // マスタデータをロード後にFirestoreから全履歴を取得
        loadMasterDataAndFetchList()

        // 支払い登録画面への遷移
        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, PayRecordAddActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launchEditPayRecord(intent)
            } else {
                startActivity(intent)
            }
        }

        // 絞り込みダイアログの表示
        buttonRefinement.setOnClickListener {
            val fragment = PayRecordListRefinementFragment()
            fragment.show(supportFragmentManager, "PayRecordListRefinementFragment")
        }
    }

    override fun onResume() {
        super.onResume()
        // 初回ロード完了後に画面復帰した場合はデータを再取得
        if (purposeMap.isNotEmpty() && memberMap.isNotEmpty()) {
            fetchRecordsFromRemote()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun launchEditPayRecord(intent: Intent) {
        editPayRecordLauncher.launch(intent)
    }

    /**
     * マスタデータ（目的・メンバー）を一括事前ロードし、その後全履歴を取得
     */
    private fun loadMasterDataAndFetchList() {
        val currentUserId = userID.takeIf { it.isNotEmpty() } ?: return

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

                        // マスタ準備完了後に全件データを取得
                        fetchRecordsFromRemote()
                    }
                    .addOnFailureListener {
                        fetchRecordsFromRemote()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "マスタデータ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                fetchRecordsFromRemote()
            }
    }

    /**
     * Firestoreからは user_id のみの一致で全件取得（複合インデックス不要）
     */
    private fun fetchRecordsFromRemote() {
        val currentUserId = userID.takeIf { it.isNotEmpty() } ?: return

        firestore.collection("payment_history")
            .whereEqualTo("user_id", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val recordList = mutableListOf<PayRecordUiModel>()

                for (document in querySnapshot) {
                    val pPurposeId = document.getString("pay_purpose_id") ?: ""
                    val mId = document.getString("member_id") ?: ""

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
                    recordList.add(payRecord)
                }

                // 日付降順にソートしてメモリキャッシュに保存
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                allPayRecords = recordList.sortedByDescending {
                    try {
                        dateFormat.parse(it.payDate) ?: Date(0)
                    } catch (e: Exception) {
                        Date(0)
                    }
                }

                // 現在の条件でローカルフィルタを実行
                applyLocalFilter()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * ダイアログからの絞り込み条件を受け取り、ローカルフィルタを実行
     */
    fun applyRefinement(memberId: String?, startDate: String?, finishDate: String?, payDone: String?, payPurposeId: String?) {
        currentMemberId = memberId
        currentStartDate = startDate
        currentFinishDate = finishDate
        currentPayDone = payDone
        currentPayPurposeId = payPurposeId

        // ネットワーク通信を行わず、ローカルで即座に絞り込み
        applyLocalFilter()
    }

    /**
     * メモリ上の allPayRecords に対してKotlinコレクション関数でフィルタリング
     */
    private fun applyLocalFilter() {
        val filteredList = allPayRecords.filter { item ->
            // メンバー絞り込み
            val matchesMember = currentMemberId == null || item.memberId == currentMemberId

            // 支払い目的絞り込み
            val matchesPurpose = currentPayPurposeId == null || item.payPurposeId == currentPayPurposeId

            // 期間絞り込み（yyyy/MM/dd 形式のため文字列比較が可能）
            val matchesStartDate = currentStartDate == null || item.payDate >= currentStartDate!!
            val matchesFinishDate = currentFinishDate == null || item.payDate <= currentFinishDate!!

            // 領収状態絞り込み
            val matchesPayDone = when (currentPayDone) {
                "領収済み" -> item.isReceptChecked
                "未受領" -> !item.isReceptChecked
                else -> true
            }

            matchesMember && matchesPurpose && matchesStartDate && matchesFinishDate && matchesPayDone
        }

        // 合計金額の更新
        val totalAmount = filteredList.sumOf { it.payAmount }
        textViewTotalAmount.text = "合計金額：" + getString(R.string.formatted_number, totalAmount)

        if (allPayRecords.isNotEmpty() && filteredList.isEmpty()) {
            Toast.makeText(this, "該当する支払い履歴がありません。", Toast.LENGTH_SHORT).show()
        } else if (allPayRecords.isEmpty()) {
            Toast.makeText(this, "支払い履歴が登録されていません。", Toast.LENGTH_SHORT).show()
        }

        // ListAdapterに差分反映
        payRecordAdapter.submitList(filteredList)
    }
}