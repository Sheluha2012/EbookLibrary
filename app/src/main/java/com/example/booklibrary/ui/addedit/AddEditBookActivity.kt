package com.example.booklibrary.ui.addedit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.viewmodel.BookViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.example.booklibrary.R

class AddEditBookActivity : AppCompatActivity() {

    private lateinit var viewModel: BookViewModel
    private var editingBook: Book? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_book)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val etTitle = findViewById<TextInputEditText>(R.id.et_title)
        val etAuthor = findViewById<TextInputEditText>(R.id.et_author)
        val etGenre = findViewById<TextInputEditText>(R.id.et_genre)
        val etDescription = findViewById<TextInputEditText>(R.id.et_description)
        val tilTitle = findViewById<TextInputLayout>(R.id.til_title)
        val tilAuthor = findViewById<TextInputLayout>(R.id.til_author)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save)

        val bookId = intent.getIntExtra("book_id", -1)
        if (bookId != -1) {
            editingBook = Book(
                id = bookId,
                title = intent.getStringExtra("book_title") ?: "",
                author = intent.getStringExtra("book_author") ?: "",
                description = intent.getStringExtra("book_description") ?: "",
                genre = intent.getStringExtra("book_genre") ?: "",
                dateAdded = intent.getStringExtra("book_date") ?: ""
            )
            toolbar.title = getString(R.string.edit_book)
            etTitle.setText(editingBook?.title)
            etAuthor.setText(editingBook?.author)
            etGenre.setText(editingBook?.genre)
            etDescription.setText(editingBook?.description)
        } else {
            toolbar.title = getString(R.string.add_book)
        }

        toolbar.setNavigationOnClickListener { finish() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val author = etAuthor.text.toString().trim()
            val genre = etGenre.text.toString().trim()
            val description = etDescription.text.toString().trim()

            var isValid = true
            if (title.isEmpty()) {
                tilTitle.error = getString(R.string.error_field_required)
                isValid = false
            } else {
                tilTitle.error = null
            }
            if (author.isEmpty()) {
                tilAuthor.error = getString(R.string.error_field_required)
                isValid = false
            } else {
                tilAuthor.error = null
            }

            if (!isValid) return@setOnClickListener

            val date = editingBook?.dateAdded ?: SimpleDateFormat(
                "dd.MM.yyyy", Locale.getDefault()
            ).format(Date())

            if (editingBook != null) {
                viewModel.update(editingBook!!.copy(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description
                ))
            } else {
                viewModel.insert(Book(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description,
                    dateAdded = date
                ))
            }
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = when (language) {
            "russian" -> Locale("ru")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfig: Configuration) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = when (language) {
            "russian" -> Locale("ru")
            else -> Locale("en")
        }
        overrideConfig.setLocale(locale)
        super.applyOverrideConfiguration(overrideConfig)
    }
}