package com.example.booklibrary.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_books")
data class CachedBook(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val genre: String,
    val thumbnail: String,
    val cachedAt: Long = System.currentTimeMillis()
)