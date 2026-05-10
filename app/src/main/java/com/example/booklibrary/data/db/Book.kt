package com.example.booklibrary.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "books",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val description: String,
    val genre: String,
    val dateAdded: String,
    val thumbnail: String = ""
)