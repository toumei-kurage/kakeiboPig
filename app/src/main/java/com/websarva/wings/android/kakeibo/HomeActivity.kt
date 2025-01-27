package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class HomeActivity : BaseActivity(R.layout.activity_home,R.string.title_home) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var leftoverTextView: TextView
    private lateinit var buttonSetInfo: Button
    private lateinit var buttonBalanceSheetAdd: Button

    // Fragmentからもらう値の用意
    private var budgetSet: String = "0"
    private var startDateString: String = ""
    private var finishDateString: String = ""

    // ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()

        // 画面部品の取得
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        budgetTextView = findViewById(R.id.budgetTextView)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        leftoverTextView = findViewById(R.id.leftoverTextView)
        buttonSetInfo = findViewById(R.id.buttonSetInfo)
        buttonBalanceSheetAdd = findViewById(R.id.buttonBalanceSheetAdd)

        loadLatestBalanceHistory()
//
//        // 期間のセット
//        dateRangeTextView.text = getString(R.string.date_range_set, startDateString, finishDateString)
//
//        // 予算額のセット
//        budgetTextView.text = "${budgetSet}円"

        // 合計額のセット
        val sumExpenditure = databaseHelper.getTotalAmountForUserInDateRange(userID, startDateString, finishDateString)
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

        buttonBalanceSheetAdd.setOnClickListener{
            if(budgetSet != "0" && startDateString != "" && finishDateString != "") {
                saveBalanceHistory()
            }else {
                Toast.makeText(this,"予算と期間を設定してください。",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    // Fragmentから情報を受け取る
    fun setInfo(budget: Int, startDate: String, finishDate: String) {
        budgetSet = budget.toString()
        this.startDateString = startDate
        this.finishDateString = finishDate

        // 新しいデータで画面を再描画
        updateUI()
    }

    // UIを更新するメソッド
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        // 画面に表示する内容を更新
        dateRangeTextView.text = if (startDateString != "" && finishDateString != "") {
            getString(R.string.date_range_set, startDateString, finishDateString)
        } else {
            getString(R.string.date_range_set, "未設定", "未設定")
        }

        budgetTextView.text = if (budgetSet != "0") {
            "${budgetSet}円"
        } else {
            "未設定"
        }
        // 合計額の再計算
        val sumExpenditure = databaseHelper.getTotalAmountForUserInDateRange(userID, startDateString, finishDateString)
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
        val payAmountByPurposeList = databaseHelper.getAmountByPurposeNameForUserInDateRange(userID, startDateString, finishDateString)
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

    private fun saveBalanceHistory() {
        val db = databaseHelper.writableDatabase

        // 重複をチェックするためのSQLクエリ
        val checkQuery = """
        SELECT COUNT(*) FROM balance_history
        WHERE user_id = ? AND start_date = ? AND finish_date = ?
    """

        val cursor = db.rawQuery(checkQuery, arrayOf(userID, startDateString, finishDateString))

        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        if (count > 0) {
            // 同じユーザーで同じ日付範囲が存在する場合
            Toast.makeText(this, "この期間は既に存在します", Toast.LENGTH_SHORT).show()
        } else {
            // 重複がなければデータを挿入
            val values = ContentValues().apply {
                put("user_id", userID)
                put("start_date", startDateString)
                put("finish_date", finishDateString)
                put("budget", budgetSet.toInt())
            }

            try {
                val newRowId = db.insert("balance_history", null, values)
                if (newRowId != -1L) {
                    Toast.makeText(this, "家計簿が追加されました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "データベースに挿入できませんでした", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // エラーハンドリング
                Toast.makeText(this, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                db.close()
            }
        }
    }

    private fun loadLatestBalanceHistory() {
        val db = databaseHelper.readableDatabase
        val query = """
            SELECT budget, start_date, finish_date 
            FROM balance_history
            WHERE user_id = ?
            ORDER BY _id DESC
            LIMIT 1
        """
        val cursor = db.rawQuery(query, arrayOf(userID))

        if (cursor.moveToFirst()) {
            // レコードが見つかった場合
            budgetSet = cursor.getString(cursor.getColumnIndex("budget"))
            startDateString = cursor.getString(cursor.getColumnIndex("start_date"))
            finishDateString = cursor.getString(cursor.getColumnIndex("finish_date"))
        } else {
            // レコードが見つからなかった場合
            budgetSet= "0"
            startDateString = ""
            finishDateString = ""
        }

        cursor.close()
        db.close()

        // 取得した値をUIにセット
        updateUI()
    }


}
