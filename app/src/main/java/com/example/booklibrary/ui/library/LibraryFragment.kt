package com.example.booklibrary.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)

            if (books.isEmpty()) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showFab(false)
    }

    private fun openAddActivity() {
        val intent = Intent(requireContext(), AddEditBookActivity::class.java)
        startActivity(intent)
    }

    private fun openEditActivity(book: Book) {
        val intent = Intent(requireContext(), AddEditBookActivity::class.java).apply {
            putExtra("book_id", book.id)
            putExtra("book_title", book.title)
            putExtra("book_author", book.author)
            putExtra("book_description", book.description)
            putExtra("book_genre", book.genre)
            putExtra("book_date", book.dateAdded)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                viewModel.delete(book)
            }
            .show()
    }
}