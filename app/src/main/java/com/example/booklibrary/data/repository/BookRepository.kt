package com.example.booklibrary.data.repository

import androidx.lifecycle.LiveData
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.db.BookDao

class BookRepository(private val bookDao: BookDao) {
    val allBooks: LiveData<List<Book>> = bookDao.getAllBooks()

    suspend fun insert(book: Book) = bookDao.insert(book)
    suspend fun update(book: Book) = bookDao.update(book)
    suspend fun delete(book: Book) = bookDao.delete(book)
}