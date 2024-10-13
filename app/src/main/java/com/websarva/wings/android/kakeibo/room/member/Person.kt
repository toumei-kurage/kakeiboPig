package com.websarva.wings.android.kakeibo.room.member

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_table",
        indices = [androidx.room.Index(value = ["userID","memberName"], unique = true)])
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userID:String,
    var memberName: String
)
