package com.websarva.wings.android.kakeibo.room.payPurpose

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "payPurpose_table",
        indices = [Index(value = ["userID","payPurposeName"], unique = true)])
data class PayPurpose(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userID:String,
    var payPurposeName: String,
)
