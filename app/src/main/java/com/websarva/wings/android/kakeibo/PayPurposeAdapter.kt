package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// PayPurpose のデータクラス
data class PayPurpose(val id: Long, val userID: String,val payPurposeName: String)

class PayPurposeAdapter(private val context: Context, private var payPurposeList: List<PayPurpose>) :
    RecyclerView.Adapter<PayPurposeAdapter.PayPurposeViewHolder>() {

    // ViewHolder クラス
    class PayPurposeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val payPurposeNameTextView: TextView = itemView.findViewById(R.id.payPurposeNameTextView)
    }

    companion object {
        private const val REQUEST_CODE_EDIT_PAY_PURPOSE = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayPurposeViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_pay_purpose, parent, false)
        return PayPurposeViewHolder(itemView)
    }

    @SuppressLint("StringFormatMatches")
    override fun onBindViewHolder(holder: PayPurposeViewHolder, position: Int) {
        val payPurpose = payPurposeList[position]
        holder.payPurposeNameTextView.text = payPurpose.payPurposeName

        // アイテムをタップした時の処理
        holder.itemView.setOnClickListener {
            val intent = Intent(context, UpdatePayPurposeActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("PAY_PURPOSE_ID", payPurpose.id)
            intent.putExtra("PAY_PURPOSE_NAME", payPurpose.payPurposeName)
            // Activityに戻り値を受け取るためにstartActivityForResultを使う
            (context as? PayPurposeListActivity)?.startActivityForResult(
                intent,
                REQUEST_CODE_EDIT_PAY_PURPOSE
            )
        }
    }

    override fun getItemCount(): Int {
        return payPurposeList.size
    }

    // 新しいデータでAdapterを更新
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPayPurposeList: List<PayPurpose>) {
        payPurposeList = newPayPurposeList
        notifyDataSetChanged()
    }

}
