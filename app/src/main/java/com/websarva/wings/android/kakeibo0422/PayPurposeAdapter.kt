package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

// PayPurpose のデータクラス
data class PayPurpose(val id: String, val userID: String,val payPurposeName: String, val resistDate: String)

class PayPurposeAdapter(private val context: Context, private var payPurposeList: List<PayPurpose>) :
    RecyclerView.Adapter<PayPurposeAdapter.PayPurposeViewHolder>() {

    // ViewHolder クラス
    class PayPurposeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val payPurposeNameTextView: TextView = itemView.findViewById(R.id.payPurposeNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayPurposeViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_pay_purpose, parent, false)
        return PayPurposeViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("StringFormatMatches")
    override fun onBindViewHolder(holder: PayPurposeViewHolder, position: Int) {
        val payPurpose = payPurposeList[position]
        holder.payPurposeNameTextView.text = payPurpose.payPurposeName

        // アイテムをタップした時の処理
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PayPurposeUpdateActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("PAY_PURPOSE_ID", payPurpose.id)
            intent.putExtra("PAY_PURPOSE_NAME", payPurpose.payPurposeName)
            intent.putExtra("RESIST_DATE", payPurpose.resistDate)
            // Activityに戻り値を受け取るために registerForActivityResult を使う
            (context as? PayPurposeListActivity)?.launchEditPayPurpose(intent)
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
