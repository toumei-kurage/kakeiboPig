package com.websarva.wings.android.kakeibo.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.R

class PersonAdapter(private val personList: List<Person>) :
    RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    // ViewHolderクラスを定義します
    class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val personNameTextView: TextView = itemView.findViewById(R.id.personNameTextView)
    }

    // 新しいViewHolderを作成します（レイアウトの初期化）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    // ViewHolderにデータをバインドします（データの表示）
    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = personList[position]
        holder.personNameTextView.text = person.memberName
    }

    // リストのサイズを返します
    override fun getItemCount(): Int {
        return personList.size
    }
}
