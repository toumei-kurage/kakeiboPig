package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Member のデータクラス
data class Member(val id: Long, val userId: String, var memberName: String)

class MemberAdapter(private val context: Context, private var memberList: List<Member>) :
    RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    // ViewHolder クラス
    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberNameTextView: TextView = itemView.findViewById(R.id.memberNameTextView)
    }

    companion object {
        private const val REQUEST_CODE_EDIT_MEMBER = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(itemView)
    }

    @SuppressLint("StringFormatMatches")
    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = memberList[position]
        holder.memberNameTextView.text = member.memberName

        // アイテムをタップした時の処理
        holder.itemView.setOnClickListener {
            val intent = Intent(context, MemberUpdateActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("MEMBER_ID", member.id)
            intent.putExtra("MEMBER_NAME", member.memberName)
            // Activityに戻り値を受け取るためにstartActivityForResultを使う
            (context as? MemberListActivity)?.startActivityForResult(
                intent,
                REQUEST_CODE_EDIT_MEMBER
            )
        }
    }

    override fun getItemCount(): Int {
        return memberList.size
    }

    // 新しいデータでAdapterを更新
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newMemberList: List<Member>) {
        memberList = newMemberList
        notifyDataSetChanged()
    }
}
