package com.websarva.wings.android.kakeibo0422.helper

import android.content.Context
import androidx.appcompat.app.AlertDialog

class DialogHelper(private val context:Context) {
    fun dialogOkOnly(title:String,message:String){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss() // ダイアログを閉じる
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}