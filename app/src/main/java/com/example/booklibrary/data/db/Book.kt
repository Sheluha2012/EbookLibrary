package com.example.booklibrary.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String,
    val description: String,
    val genre: String,
    val dateAdded: String,
    val thumbnail: String = ""
)