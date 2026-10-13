package com.websarva.wings.android.kakeibo0422

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class PayRecordUiModel(
    val id: String,
    val userId: String,
    val memberId: String,
    val payPurposeId: String,
    val payPurposeName: String,
    val payDate: String,
    val payAmount: Int,
    val isReceptChecked: Boolean,
    val note: String
)

class PayRecordAdapter(
    private val onItemClick: (PayRecordUiModel) -> Unit
) : ListAdapter<PayRecordUiModel, PayRecordAdapter.PayRecordViewHolder>(DiffCallback) {

    class PayRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val payDateTextView: TextView = itemView.findViewById(R.id.payDateTextView)
        val payPurposeNameTextView:TextView = itemView.findViewById(R.id.payPurposeNameTextView)
        val payAmountTextView:TextView = itemView.findViewById(R.id.payAmountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayRecordViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_pay_record, parent, false)
        val holder = PayRecordViewHolder(itemView)

        itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(getItem(position))
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: PayRecordViewHolder, position: Int) {
        val item = getItem(position)
        holder.payDateTextView.text = item.payDate
        holder.payPurposeNameTextView.text = item.payPurposeName
        holder.payAmountTextView.text = holder.itemView.context.getString(R.string.formatted_number, item.payAmount)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PayRecordUiModel>() {
            override fun areItemsTheSame(oldItem: PayRecordUiModel, newItem: PayRecordUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PayRecordUiModel, newItem: PayRecordUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}