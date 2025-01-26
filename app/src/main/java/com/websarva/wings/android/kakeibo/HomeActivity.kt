package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class HomeActivity : BaseActivity(R.layout.activity_home,R.string.title_home) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var leftoverTextView: TextView
    private lateinit var buttonSetInfo: Button

    // Fragmentからもらう値の用意
    private var budgetSet: String = "0"
    private var startDate: String = ""
    private var finishDate: String = ""

    // ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()

        // SharedPreferencesから保存されていた値を取得
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val savedBudgetSet = sharedPreferences.getString("budgetSet", "0") // デフォルト値は "0"
        val savedStartDate = sharedPreferences.getString("startDate", "")
        val savedFinishDate = sharedPreferences.getString("finishDate", "")

        // 取得したデータをフィールドにセット
        budgetSet = savedBudgetSet ?: "0"
        startDate = savedStartDate ?: ""
        finishDate = savedFinishDate ?: ""

        // 画面部品の取得
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        budgetTextView = findViewById(R.id.budgetTextView)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        leftoverTextView = findViewById(R.id.leftoverTextView)
        buttonSetInfo = findViewById(R.id.buttonSetInfo)

        // 期間のセット
        dateRangeTextView.text = getString(R.string.date_range_set, startDate, finishDate)

        // 予算額のセット
        budgetTextView.text = "${budgetSet}円"

        // 合計額のセット
        val sumExpenditure = databaseHelper.getTotalAmountForUserInDateRange(userID, startDate, finishDate)
        sumExpenditureTextView.text = "${sumExpenditure}円"

        // 繰越額のセット
        leftoverTextView.text = "${budgetSet.toInt() - sumExpenditure}円"

        // 支払い目的リスト
        val payPurposeList = databaseHelper.getPaymentPurposesForUser(userID)
        addLayoutsForRecords(payPurposeList)

        // 「設定」ボタンのクリックリスナー
        buttonSetInfo.setOnClickListener {
            // フラグメントを表示
            val fragment = BalanceSheetSetInfoFragment()
            fragment.show(supportFragmentManager, "BalanceSheetSetInfoFragment")
        }
    }

    // Fragmentから情報を受け取る
    fun setInfo(budget: Int, startDate: String, finishDate: String) {
        budgetSet = budget.toString()
        this.startDate = startDate
        this.finishDate = finishDate

        // データをSharedPreferencesに保存する
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("budgetSet", budgetSet)
        editor.putString("startDate", startDate)
        editor.putString("finishDate", finishDate)
        editor.apply()

        // 新しいデータで画面を再描画
        updateUI()
    }

    // UIを更新するメソッド
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        // 期間の再設定
        dateRangeTextView.text = getString(R.string.date_range_set, startDate, finishDate)
        // 予算額の再設定
        budgetTextView.text = "${budgetSet}円"
        // 合計額の再計算
        val sumExpenditure = databaseHelper.getTotalAmountForUserInDateRange(userID, startDate, finishDate)
        sumExpenditureTextView.text = "${sumExpenditure}円"

        // 繰越額の再計算
        leftoverTextView.text = "${budgetSet.toInt() - sumExpenditure}円"

        // 支払い目的リストの再描画
        val payPurposeList = databaseHelper.getPaymentPurposesForUser(userID)
        addLayoutsForRecords(payPurposeList)
    }

    // レコードに基づいて動的にLinearLayoutを追加する処理
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun addLayoutsForRecords(records: List<String>) {
        val payAmountByPurposeList = databaseHelper.getAmountByPurposeNameForUserInDateRange(userID, startDate, finishDate)
        linearLayoutContainer.removeAllViews()
        for (record in records) {
            val layout = LayoutInflater.from(this).inflate(R.layout.layout_item, null) as LinearLayout
            val payPurposeNameTextView: TextView = layout.findViewById(R.id.payPurposeName)
            val payAmount: TextView = layout.findViewById(R.id.payAmount)
            payPurposeNameTextView.text = getString(R.string.purpose_name, record)
            payAmount.text = "${payAmountByPurposeList.getOrDefault(record, 0)}円"
            linearLayoutContainer.addView(layout)
        }
    }
}
