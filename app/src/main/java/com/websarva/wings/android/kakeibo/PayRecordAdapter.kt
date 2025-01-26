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

// PayRecord のデータクラス
data class PayRecord(val id: Long, val userId: String,var memberId:Long,var payPurposeId:Long, var payDate: String,var payAmount:Int,var isReceptChecked:Boolean,var note:String)

class PayRecordAdapter(private val context: Context, private var payRecordList: List<PayRecord>) :
    RecyclerView.Adapter<PayRecordAdapter.PayRecordViewHolder>() {
    private val databaseHelper = DatabaseHelper(context)

    // ViewHolder クラス
    class PayRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val payDateTextView: TextView = itemView.findViewById(R.id.payDateTextView)
        val payPurposeNameTextView:TextView = itemView.findViewById(R.id.payPurposeNameTextView)
        val payAmountTextView:TextView = itemView.findViewById(R.id.payAmountTextView)
    }

    companion object {
        private const val REQUEST_CODE_EDIT_PAY_RECORD = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayRecordViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_pay_record, parent, false)
        return PayRecordViewHolder(itemView)
    }

    @SuppressLint("StringFormatMatches", "SetTextI18n")
    override fun onBindViewHolder(holder: PayRecordViewHolder, position: Int) {
        val payRecord = payRecordList[position]
        holder.payDateTextView.text = payRecord.payDate
        holder.payPurposeNameTextView.text = databaseHelper.getPayPurposeNameById(payRecord.payPurposeId.toInt())
        holder.payAmountTextView.text = "${payRecord.payAmount}円"

        // アイテムをタップした時の処理
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PayRecordDetailActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("PAY_RECORD_ID", payRecord.id)
            intent.putExtra("USER_ID",payRecord.userId)
            intent.putExtra("MEMBER_ID", payRecord.memberId)
            intent.putExtra("PAY_PURPOSE_ID",payRecord.payPurposeId)
            intent.putExtra("PAY_DATE",payRecord.payDate)
            intent.putExtra("PAY_AMOUNT",payRecord.payAmount)
            intent.putExtra("IS_RECEPT_CHECKED",payRecord.isReceptChecked)
            intent.putExtra("NOTE",payRecord.note)
            // Activityに戻り値を受け取るためにstartActivityForResultを使う
            (context as? PayRecordListActivity)?.startActivityForResult(
                intent,
                REQUEST_CODE_EDIT_PAY_RECORD
            )
        }
    }

    override fun getItemCount(): Int {
        return payRecordList.size
    }

    // 新しいデータでAdapterを更新
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPayRecordList: List<PayRecord>) {
        payRecordList = newPayRecordList
        notifyDataSetChanged()
    }
}
