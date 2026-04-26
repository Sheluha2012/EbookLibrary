package com.example.booklibrary.ui.search

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.booklibrary.MainActivity
import com.example.booklibrary.R
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.network.BookItem
import com.example.booklibrary.utils.NetworkMonitor
import com.example.booklibrary.viewmodel.BookSearchViewModel
import com.example.booklibrary.viewmodel.BookViewModel
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var searchViewModel: BookSearchViewModel
    private lateinit var bookViewModel: BookViewModel
    private lateinit var adapter: BookSearchAdapter
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = ViewModelProvider(this)[BookSearchViewModel::class.java]
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]

        val etSearch = view.findViewById<TextInputEditText>(R.id.et_search)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_search)

        adapter = BookSearchAdapter { bookItem -> addBookToLibrary(bookItem) }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        networkMonitor = (activity as? MainActivity)?.networkMonitor
            ?: NetworkMonitor(requireContext())

        val tvCacheBanner = view.findViewById<TextView>(R.id.tv_cache_banner)

        networkMonitor.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                tvCacheBanner.visibility = View.VISIBLE
            }
        }

        searchViewModel.isFromCache.observe(viewLifecycleOwner) { isFromCache ->
            tvCacheBanner.visibility = if (isFromCache) View.VISIBLE else View.GONE
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    val isOnline = networkMonitor.value ?: true
                    searchViewModel.searchBooks(query, isOnline)
                    hideKeyboard(etSearch)
                }
                true
            } else false
        }

        searchViewModel.isFromCache.observe(viewLifecycleOwner) { isFromCache ->
            tvCacheBanner.visibility = if (isFromCache) View.VISIBLE else View.GONE
        }

        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            recycler.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
        }

        searchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        searchViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                tvError.visibility = View.VISIBLE
                tvError.text = error
            } else {
                tvError.visibility = View.GONE
            }
        }

        (activity as? MainActivity)?.run {
            updateToolbarTitle(R.id.bottom_nav_library)
        }
    }

    private fun addBookToLibrary(bookItem: BookItem) {
        val info = bookItem.volumeInfo
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        val book = Book(
            title = info.title ?: "Без названия",
            author = info.authors?.joinToString(", ") ?: "Автор неизвестен",
            description = info.description ?: "",
            genre = info.categories?.firstOrNull() ?: "",
            dateAdded = date,
            thumbnail = info.imageLinks?.thumbnail?.replace("http://", "https://") ?: ""
        )

        bookViewModel.insert(book)
        Toast.makeText(requireContext(), R.string.search_book_added, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}