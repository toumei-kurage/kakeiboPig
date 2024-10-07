package com.websarva.wings.android.kakeibo.room.member

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.R

// RecyclerView用のAdapterクラス
class PersonAdapter(
    private val personList: List<Person>, // 表示するPersonのリスト
    private val onUpdateClick: (Person) -> Unit, // 更新ボタンのクリックリスナー
    private val onDeleteClick: (Person) -> Unit // 削除ボタンのクリックリスナー
) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    // ViewHolderクラスを定義（個々のリスト項目のビューを保持）
    inner class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val personNameTextView: TextView = itemView.findViewById(R.id.personNameTextView) // 名前表示用TextView
        val updateButton: Button = itemView.findViewById(R.id.updateButton) // 更新ボタン
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton) // 削除ボタン
    }

    // 新しいViewHolderを作成するメソッド（レイアウトの初期化）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        // item_person.xmlレイアウトをinflateしてViewHolderを作成
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    // ViewHolderにデータをバインドするメソッド（表示内容の設定）
    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = personList[position]
        holder.personNameTextView.text = person.memberName

        // 更新ボタンのクリックリスナーを設定
        holder.updateButton.setOnClickListener {
            onUpdateClick(person) // 更新処理を呼び出す
        }

        // 削除ボタンのクリックリスナーを設定
        holder.deleteButton.setOnClickListener {
            onDeleteClick(person) // 削除処理を呼び出す
        }
    }

    // リストのサイズを返すメソッド
    override fun getItemCount(): Int {
        return personList.size // 表示するアイテムの数を返す
    }
}
