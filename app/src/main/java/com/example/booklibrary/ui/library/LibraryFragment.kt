package com.example.booklibrary.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.booklibrary.ui.addedit.AddEditBookActivity
import com.example.booklibrary.MainActivity
import com.example.booklibrary.R
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.viewmodel.BookViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.example.booklibrary.ui.search.SearchFragment

class LibraryFragment : Fragment() {

    private lateinit var viewModel: BookViewModel
    private lateinit var adapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this)[BookViewModel::class.java]
        viewModel.startSync()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_library, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_books)
        val emptyState = view.findViewById<View>(R.id.empty_state)
        val btnAddEmpty = view.findViewById<MaterialButton>(R.id.btn_add_empty)

        adapter = BookAdapter(
            onBookClick = { book -> openEditActivity(book) },
            onBookLongClick = { book -> showDeleteDialog(book) }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        viewModel.availableGenres.observe(viewLifecycleOwner) {}

        viewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
            if (books.isEmpty() && viewModel.searchQuery.value.isNullOrEmpty() && viewModel.selectedGenres.value.isNullOrEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                (activity as? MainActivity)?.showFab(false)
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                (activity as? MainActivity)?.showFab(true) { openAddActivity() }
            }
        }

        btnAddEmpty.setOnClickListener { openAddActivity() }

        val fabSearch = view.findViewById<ExtendedFloatingActionButton>(R.id.fab_search)
        fabSearch.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, SearchFragment())
                .addToBackStack(null)
                .commit()
            (activity as? MainActivity)?.updateToolbarTitle(R.id.bottom_nav_library)
        }

        viewModel.startSync()
    }

    private fun sendToTelegram(book: Book) {
        val shareText = """
            📖 Посмотри, какую книгу я нашел:
            
            Название: ${book.title}
            Автор: ${book.author}
            Жанр: ${book.genre}
            
            Отправлено из BookLibrary App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val telegramPackage = "org.telegram.messenger"
        val isTelegramInstalled = isPackageInstalled(telegramPackage)

        if (isTelegramInstalled) {
            intent.setPackage(telegramPackage)
            startActivity(intent)
        } else {
            val chooser = Intent.createChooser(intent, "Поделиться через...")
            startActivity(chooser)
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        if (!viewModel.searchQuery.value.isNullOrEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(viewModel.searchQuery.value, false)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchQuery.value = newText ?: ""
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> showSortDialog()
            R.id.action_filter -> showFilterDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_date),
            getString(R.string.sort_by_title),
            getString(R.string.sort_by_author)
        )
        val sortCodes = arrayOf("date", "title", "author")
        val currentSort = sortCodes.indexOf(viewModel.sortOrder.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_title)
            .setSingleChoiceItems(sortOptions, currentSort) { dialog, which ->
                viewModel.sortOrder.value = sortCodes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val genres = viewModel.availableGenres.value ?: emptyList()
        if (genres.isEmpty()) return

        val selectedItems = viewModel.selectedGenres.value ?: emptySet()

        val checkedItems = genres.map { selectedItems.contains(it) }.toBooleanArray()
        val tempSelectedItems = selectedItems.toMutableSet()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_title)
            .setMultiChoiceItems(genres.toTypedArray(), checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    tempSelectedItems.add(genres[which])
                } else {
                    tempSelectedItems.remove(genres[which])
                }
            }
            .setNeutralButton(R.string.filter_reset) { _, _ ->
                viewModel.selectedGenres.value = emptySet()
            }
            .setPositiveButton(R.string.filter_apply) { _, _ ->
                viewModel.selectedGenres.value = tempSelectedItems
            }
            .setNegativeButton(R.string.filter_cancel, null)
            .show()
    }

    private fun openAddActivity() {
        val intent = Intent(requireContext(), AddEditBookActivity::class.java)
        startActivity(intent)
    }

    private fun openEditActivity(book: Book) {
        val intent = Intent(requireContext(), AddEditBookActivity::class.java).apply {
            putExtra("book_id", book.id)
            putExtra("book_uuid", book.uuid)
            putExtra("book_title", book.title)
            putExtra("book_author", book.author)
            putExtra("book_description", book.description)
            putExtra("book_genre", book.genre)
            putExtra("book_date", book.dateAdded)
            putExtra("book_thumbnail", book.thumbnail)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(book.title)
            .setMessage("Выберите действие для этой книги")
            .setNeutralButton("В Telegram") { _, _ ->
                sendToTelegram(book)
            }
            .setNegativeButton("Удалить") { _, _ ->
                viewModel.delete(book)
            }
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showFab(false)
    }
}