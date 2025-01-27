package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class BalanceDetailActivity : BaseActivity(R.layout.activity_balance_detail,R.string.title_balance_detail) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var leftoverTextView: TextView
    private lateinit var buttonBalanceSheetUpdate: Button


    // 前画面からもらう値の用意
    private var balanceId = -1
    private var budgetSet: String = "0"
    private var startDate: String = ""
    private var finishDate: String = ""

    // ヘルパークラス
    private val databaseHelper = DatabaseHelper(this)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_detail)

        setupDrawerAndToolbar()

        // 取得したデータをフィールドにセット
        balanceId = intent.getLongExtra("BALANCE_ID",-1).toInt()
        budgetSet = intent.getIntExtra("BUDGET",-1).toString()
        startDate = intent.getStringExtra("START_DATE").toString()
        finishDate = intent.getStringExtra("FINISH_DATE").toString()

        // 画面部品の取得
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        budgetTextView = findViewById(R.id.budgetTextView)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        leftoverTextView = findViewById(R.id.leftoverTextView)
        buttonBalanceSheetUpdate = findViewById(R.id.buttonBalanceSheetUpdate)

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

        buttonBalanceSheetUpdate.setOnClickListener{
            val intent = Intent(this,BalanceUpdateActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("BALANCE_ID", balanceId)
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("FINISH_DATE",finishDate)
            intent.putExtra("BUDGET",budgetSet)
            startActivity(intent)
            finish()
        }
    }

    // メニュー（ActionBar）の作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()  // 削除確認ダイアログを表示
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ ->
                deleteBalance()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    // 家計簿を削除する処理
    private fun deleteBalance() {
        val db = DatabaseHelper(this).writableDatabase

        val rowsDeleted = db.delete(
            "balance_history",
            "_id = ?",
            arrayOf(balanceId.toString())
        )

        if (rowsDeleted > 0) {
            Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
            // 削除成功した場合、親Activityに通知する
            val resultIntent = Intent()
            resultIntent.putExtra("BALANCE_DELETE", true)  // 削除フラグを渡す
            setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
            finish()  // アクティビティを終了し、前の画面に戻る
        } else {
            Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show()
        }
        db.close()
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}