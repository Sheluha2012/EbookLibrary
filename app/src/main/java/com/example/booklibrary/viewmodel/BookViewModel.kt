package com.example.booklibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.db.BookDatabase
import com.example.booklibrary.data.repository.BookRepository
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    val allBooks: LiveData<List<Book>>

    init {
        val dao = BookDatabase.getDatabase(application).bookDao()
        repository = BookRepository(dao)
        allBooks = repository.allBooks
    }

    fun insert(book: Book) = viewModelScope.launch {
        repository.insert(book)
    }

    fun update(book: Book) = viewModelScope.launch {
        repository.update(book)
    }

    fun delete(book: Book) = viewModelScope.launch {
        repository.delete(book)
    }
}