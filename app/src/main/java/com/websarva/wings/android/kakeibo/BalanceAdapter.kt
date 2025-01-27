package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

// Balance のデータクラス
data class Balance(val id: Long, val userId: String, var startDate: String, var finishDate: String, var budget:Int)

class BalanceAdapter(private val context: Context, private var balanceList: List<Balance>) :
    RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder>() {

    // ViewHolder クラス
    class BalanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    companion object {
        private const val REQUEST_CODE_EDIT_BALANCE = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_balance, parent, false)
        return BalanceViewHolder(itemView)
    }

    @SuppressLint("StringFormatMatches", "SetTextI18n")
    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val balance = balanceList[position]
        holder.dateTextView.text = context.getString(R.string.date_range_set,balance.startDate,balance.finishDate)

        // アイテムをタップした時の処理
        holder.itemView.setOnClickListener {
            val intent = Intent(context, BalanceDetailActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("BALANCE_ID", balance.id)
            intent.putExtra("START_DATE", balance.startDate)
            intent.putExtra("FINISH_DATE",balance.finishDate)
            intent.putExtra("BUDGET",balance.budget)
            // Activityに戻り値を受け取るためにstartActivityForResultを使う
            (context as? BalanceListActivity)?.startActivityForResult(
                intent,
                REQUEST_CODE_EDIT_BALANCE
            )
        }
    }

    override fun getItemCount(): Int {
        return balanceList.size
    }

    // 新しいデータでAdapterを更新
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newBalanceList: List<Balance>) {
        balanceList = newBalanceList
        notifyDataSetChanged()
    }
}
