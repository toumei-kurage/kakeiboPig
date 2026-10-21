package com.websarva.wings.android.kakeibo0422

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class RecurringPaymentUiModel(
    val id: String,
    val userId: String,
    val memberId: String,
    val payPurposeId: String,
    val payPurposeName: String,
    val paymentDay: Int, // 毎月何日か (1〜31)
    val amount: Int,
    val isReceptChecked: Boolean,
    val note: String
)

class RecurringPaymentAdapter(
    private val onItemClick: (RecurringPaymentUiModel) -> Unit
) : ListAdapter<RecurringPaymentUiModel, RecurringPaymentAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recurringDayTextView: TextView = itemView.findViewById(R.id.recurringDayTextView)
        val payPurposeNameTextView: TextView = itemView.findViewById(R.id.payPurposeNameTextView)
        val payAmountTextView: TextView = itemView.findViewById(R.id.payAmountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring_payment, parent, false)
        val holder = ViewHolder(view)
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(getItem(position))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.recurringDayTextView.text = holder.itemView.context.getString(R.string.recurring_payment_day_format, item.paymentDay)
        holder.payPurposeNameTextView.text = item.payPurposeName
        holder.payAmountTextView.text = holder.itemView.context.getString(R.string.formatted_number, item.amount)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RecurringPaymentUiModel>() {
            override fun areItemsTheSame(oldItem: RecurringPaymentUiModel, newItem: RecurringPaymentUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: RecurringPaymentUiModel, newItem: RecurringPaymentUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}