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

// Member のデータクラス
data class Member(val id: String, val userId: String, var memberName: String, var resistDate: String)

class MemberAdapter(private val context: Context, private var memberList: List<Member>) :
    RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    // ViewHolder クラス
    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberNameTextView: TextView = itemView.findViewById(R.id.memberNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
            intent.putExtra("RESIST_DATE", member.resistDate)
            // Activityに戻り値を受け取るために registerForActivityResult を使う
            (context as? MemberListActivity)?.launchEditMember(intent)
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
