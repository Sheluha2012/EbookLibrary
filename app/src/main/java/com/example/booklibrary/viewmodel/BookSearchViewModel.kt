package com.example.booklibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.booklibrary.data.network.BookItem
import com.example.booklibrary.data.repository.BookSearchRepository
import kotlinx.coroutines.launch

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookSearchRepository(application)

    private val _searchResults = MutableLiveData<List<BookItem>>()
    val searchResults: LiveData<List<BookItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isFromCache = MutableLiveData<Boolean>()
    val isFromCache: LiveData<Boolean> = _isFromCache

    fun searchBooks(query: String, isOnline: Boolean) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = repository.searchBooks(query, isOnline)
                _isFromCache.value = !isOnline
                if (results.isEmpty()) {
                    _error.value = if (isOnline) "Ничего не найдено" else "Нет кэшированных данных"
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки"
            } finally {
                _isLoading.value = false
            }
        }
    }
}