package com.websarva.wings.android.kakeibo.room.member

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.R
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurpose

class PayPurposeAdapter(
    private val payPurposeList: List<PayPurpose>,
    private val onUpdateClick: (PayPurpose) -> Unit,
    private val onDeleteClick: (PayPurpose) -> Unit
) : RecyclerView.Adapter<PayPurposeAdapter.PayPurposeViewHolder>() {

    inner class PayPurposeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val payPurposeNameTextView: TextView = itemView.findViewById(R.id.payPurposeNameTextView)
        val updateButton: Button = itemView.findViewById(R.id.updateButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayPurposeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pay_purpose,parent,false)
        return PayPurposeViewHolder(view)
    }

    override fun onBindViewHolder(holder:PayPurposeViewHolder,position:Int){
        val payPurpose = payPurposeList[position]
        holder.payPurposeNameTextView.text = payPurpose.payPurposeName
        holder.updateButton.setOnClickListener{
            onUpdateClick(payPurpose)
        }
        holder.deleteButton.setOnClickListener{
            onDeleteClick(payPurpose)
        }
    }
    override fun getItemCount():Int{
        return payPurposeList.size
    }
}
