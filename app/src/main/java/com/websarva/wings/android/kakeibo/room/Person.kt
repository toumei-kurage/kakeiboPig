package com.websarva.wings.android.kakeibo.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_table")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val userID:String,
    val memberName: String
)
