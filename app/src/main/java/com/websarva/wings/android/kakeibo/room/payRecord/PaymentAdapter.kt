package com.websarva.wings.android.kakeibo.room.payRecord

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.R
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.PersonDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentAdapter(
    private val payRecordList: List<Payment>,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val listener: OnPaymentClickListener
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    interface OnPaymentClickListener {
        fun onPaymentClick(payment: Payment)
    }

    inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val paymentDateTextView: TextView = itemView.findViewById(R.id.paymentDateTextView)
        val paymentPurposeTextView: TextView = itemView.findViewById(R.id.paymentPurposeTextView)
        val paymentPersonNameTextView: TextView = itemView.findViewById(R.id.paymentPersonNameTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onPaymentClick(payRecordList[position]) // リスナーを呼び出す
                }
            }
        }
    }

    // 新しいViewHolderを作成するメソッド（レイアウトの初期化）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        // item_payment.xmlレイアウトをinflateしてViewHolderを作成
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    // ViewHolderにデータをバインドするメソッド（表示内容の設定）
    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payRecordList[position]
        val personDao: PersonDao = AppDatabase.getDatabase(context).personDao()
        lifecycleOwner.lifecycleScope.launch {
            val person = withContext(Dispatchers.IO) {
                personDao.getPerson(payment.payerId) // バックグラウンドスレッドで呼び出す
            }
            // UIスレッドでテキストを設定
            holder.paymentDateTextView.text = formatLongToDateString(payment.paymentDate)
            holder.paymentPurposeTextView.text = payment.purpose
            holder.paymentPersonNameTextView.text = person.memberName
        }
    }

    // リストのサイズを返すメソッド
    override fun getItemCount(): Int {
        return payRecordList.size // 表示するアイテムの数を返す
    }

    private fun formatLongToDateString(timestamp: Long): String {
        // Long型のtimestampをDate型に変換
        val date = Date(timestamp)
        // yyyy/MM/ddフォーマットのSimpleDateFormatを作成
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        // Dateをフォーマットして文字列を返す
        return format.format(date)
    }
}