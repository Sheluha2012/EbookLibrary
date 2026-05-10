package com.example.booklibrary.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.db.BookDatabase
import com.example.booklibrary.data.repository.BookRepository
import com.example.booklibrary.data.repository.RemoteBookRepository
import com.example.booklibrary.utils.FuzzySearchUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    private val bookDao = BookDatabase.getDatabase(application).bookDao()
    private val remoteRepository = RemoteBookRepository(bookDao)

    val sortOrder = MutableLiveData("date")
    val searchQuery = MutableLiveData("")
    val selectedGenres = MutableLiveData<Set<String>>(emptySet())

    init {
        repository = BookRepository(bookDao)
    }

    private val booksFromDb: LiveData<List<Book>> = sortOrder.switchMap { order ->
        when (order) {
            "title" -> bookDao.getAllBooksSortedByTitle()
            "author" -> bookDao.getAllBooksSortedByAuthor()
            else -> bookDao.getAllBooks()
        }
    }

    val availableGenres: LiveData<List<String>> = repository.allBooks.map { books ->
        books.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredBooks = MediatorLiveData<List<Book>>().apply {
        addSource(booksFromDb) { updateFilteredList() }
        addSource(searchQuery) { updateFilteredList() }
        addSource(selectedGenres) { updateFilteredList() }
    }

    private fun updateFilteredList() {
        val query = searchQuery.value ?: ""
        val filters = selectedGenres.value ?: emptySet()
        val books = booksFromDb.value ?: emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            val filtered = books.filter { book ->
                val matchesQuery = FuzzySearchUtils.isMatch(book.title, query) ||
                        FuzzySearchUtils.isMatch(book.author, query)
                val matchesGenre = if (filters.isEmpty()) true else filters.contains(book.genre)

                matchesQuery && matchesGenre
            }
            withContext(Dispatchers.Main) {
                filteredBooks.value = filtered
            }
        }
    }

    fun startSync() {
        remoteRepository.startRealtimeSync(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        remoteRepository.stopRealtimeSync()
    }

    val allBooks: LiveData<List<Book>> = repository.allBooks

    fun insert(book: Book) = viewModelScope.launch {
        remoteRepository.insertBook(book)
    }

    fun update(book: Book) = viewModelScope.launch {
        remoteRepository.updateBook(book)
    }

    fun delete(book: Book) = viewModelScope.launch {
        remoteRepository.deleteBook(book)
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            remoteRepository.stopRealtimeSync()
            bookDao.clearAllBooks()

            FirebaseAuth.getInstance().signOut()

            Thread.sleep(100)

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}