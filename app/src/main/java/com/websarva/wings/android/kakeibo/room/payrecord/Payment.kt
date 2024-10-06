package com.websarva.wings.android.kakeibo.room.payrecord

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.websarva.wings.android.kakeibo.room.member.Person

@Entity(
    tableName = "payment",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["payerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "payerId") val payerId: Int,
    @ColumnInfo(name = "userId") val userId: String, // ログイン中のユーザーID
    val purpose: String,
    val paymentDate: Long, // UNIXタイムスタンプ
    val amount: Int,
    val isReceiptChecked: Boolean,
    val notes: String? = null // NULL許容
)
