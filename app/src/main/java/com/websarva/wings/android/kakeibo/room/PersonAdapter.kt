package com.websarva.wings.android.kakeibo.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.R

class PersonAdapter(private val personList: List<Person>,
                    private val onUpdateClick: (Person) -> Unit,
                    private val onDeleteClick: (Person) -> Unit) :
    RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    // ViewHolderクラスを定義します
    inner class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val personNameTextView: TextView = itemView.findViewById(R.id.personNameTextView)
        val updateButton: Button = itemView.findViewById(R.id.updateButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
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

        // 更新ボタンのクリックリスナー
        holder.updateButton.setOnClickListener {
            onUpdateClick(person)
        }

        // 削除ボタンのクリックリスナー
        holder.deleteButton.setOnClickListener {
            onDeleteClick(person)
        }
    }

    // リストのサイズを返します
    override fun getItemCount(): Int {
        return personList.size
    }
}
