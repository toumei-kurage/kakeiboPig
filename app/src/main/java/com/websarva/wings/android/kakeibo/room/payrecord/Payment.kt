package com.websarva.wings.android.kakeibo.room.payrecord

import android.os.Parcel
import android.os.Parcelable
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    ) {
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(payerId) // 追加
        parcel.writeString(userId) // 追加
        parcel.writeString(purpose) // 追加
        parcel.writeLong(paymentDate)
        parcel.writeInt(amount)
        parcel.writeByte(if (isReceiptChecked) 1 else 0)
        parcel.writeString(notes)
    }

    companion object CREATOR : Parcelable.Creator<Payment> {
        override fun createFromParcel(parcel: Parcel): Payment {
            return Payment(parcel)
        }

        override fun newArray(size: Int): Array<Payment?> {
            return arrayOfNulls(size)
        }
    }
}