package com.websarva.wings.android.kakeibo.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PersonDao {
    @Insert
    suspend fun insert(person: Person)

    @Query("SELECT * FROM person_table WHERE userID = :userID")
    suspend fun getAllPersonsByUserId(userID: String): List<Person>
}