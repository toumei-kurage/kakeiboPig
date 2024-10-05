package com.websarva.wings.android.kakeibo.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_table")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userID:String,
    val memberName: String
)
