package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// PayRecord のデータクラス
data class PayRecord(val id: String, val userId: String,var memberId: String,var payPurposeId: String, var payDate: String,var payAmount: Int,var isReceptChecked: Boolean,var note: String)

class PayRecordAdapter(private val context: Context, private var payRecordList: List<PayRecord>) :
    RecyclerView.Adapter<PayRecordAdapter.PayRecordViewHolder>() {
    // Firestore インスタンスを取得
    private val firestore = FirebaseFirestore.getInstance()


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
        holder.payAmountTextView.text = "${payRecord.payAmount}円"
        getPayPurposeName(payRecord.payPurposeId) { payPurposeName ->
            if (payPurposeName != null) {
                // pay_purpose_nameが正常に取得できた場合
                holder.payPurposeNameTextView.text = payPurposeName
            } else {
                // エラーが発生した場合や、ドキュメントが見つからなかった場合
                println("Failed to get pay purpose name.")
            }
        }
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

    private fun getPayPurposeName(payPurposeId: String, callback: (String?) -> Unit) {
        firestore.collection("payPurposes")
            .document(payPurposeId)  // payPurposeIdに一致するドキュメントを指定
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // ドキュメントが存在する場合、pay_purpose_nameを取得
                    val payPurposeName = documentSnapshot.getString("pay_purpose_name")
                    callback(payPurposeName) // コールバックで結果を返す
                } else {
                    callback(null) // ドキュメントが存在しない場合はnullを返す
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                println("Error getting document: $exception")
                callback(null)
            }
    }
}
