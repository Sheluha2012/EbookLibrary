package com.example.booklibrary.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id DESC")
    fun getAllBooks(): LiveData<List<Book>>

    @Insert
    suspend fun insert(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Update
    suspend fun update(book: Book)
}