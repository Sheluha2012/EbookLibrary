package com.example.booklibrary.ui.addedit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.example.booklibrary.R
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.network.ImageKitUploadApi
import com.example.booklibrary.viewmodel.BookViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.imagekit.android.ImageKit
import com.imagekit.android.ImageKitCallback
import com.imagekit.android.entity.UploadError
import com.imagekit.android.entity.UploadResponse
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withContext
import com.example.booklibrary.BuildConfig

class AddEditBookActivity : AppCompatActivity() {
    private lateinit var viewModel: BookViewModel
    private var editingBook: Book? = null
    private var selectedImageUri: Uri? = null
    private var currentThumbnailUrl: String = ""
    private var photoFile: File? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            selectedImageUri = Uri.fromFile(photoFile)
            findViewById<ImageView>(R.id.iv_preview).setImageURI(selectedImageUri)
            findViewById<ImageView>(R.id.iv_preview).visibility = View.VISIBLE
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_book)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val etTitle = findViewById<TextInputEditText>(R.id.et_title)
        val etAuthor = findViewById<TextInputEditText>(R.id.et_author)
        val etGenre = findViewById<TextInputEditText>(R.id.et_genre)
        val tilTitle = findViewById<TextInputLayout>(R.id.til_title)
        val tilAuthor = findViewById<TextInputLayout>(R.id.til_author)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save)
        val ivPreview = findViewById<ImageView>(R.id.iv_preview)
        val btnCamera = findViewById<MaterialButton>(R.id.btn_camera)
        val btnSelect = findViewById<MaterialButton>(R.id.btn_select_image)
        val progress = findViewById<ProgressBar>(R.id.progress_upload)
        val etDesc = findViewById<TextInputEditText>(R.id.et_description)

        btnCamera.setOnClickListener {
            checkCameraPermission()
        }

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                ivPreview.setImageURI(it)
                ivPreview.visibility = View.VISIBLE
            }
        }
        btnSelect.setOnClickListener { pickImage.launch("image/*") }

        val bookId = intent.getIntExtra("book_id", -1)
        if (bookId != -1) {
            editingBook = Book(
                id = bookId,
                uuid = intent.getStringExtra("book_uuid") ?: "",
                title = intent.getStringExtra("book_title") ?: "",
                author = intent.getStringExtra("book_author") ?: "",
                description = intent.getStringExtra("book_description") ?: "",
                genre = intent.getStringExtra("book_genre") ?: "",
                dateAdded = intent.getStringExtra("book_date") ?: "",
                thumbnail = intent.getStringExtra("book_thumbnail") ?: ""
            )
            toolbar.title = getString(R.string.edit_book)
            etTitle.setText(editingBook?.title)
            etAuthor.setText(editingBook?.author)
            etGenre.setText(editingBook?.genre)
            etDesc.setText(editingBook?.description)

            if (currentThumbnailUrl.isNotEmpty()) {
                ivPreview.visibility = View.VISIBLE
                Glide.with(this).load(currentThumbnailUrl).into(ivPreview)
            }
        } else {
            toolbar.title = getString(R.string.add_book)
        }

        toolbar.setNavigationOnClickListener { finish() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val author = etAuthor.text.toString().trim()
            val genre = etGenre.text.toString().trim()
            val description = etDesc.text.toString().trim()

            if (title.isEmpty() || author.isEmpty()) {
                if (title.isEmpty()) tilTitle.error = getString(R.string.error_field_required)
                if (author.isEmpty()) tilAuthor.error = getString(R.string.error_field_required)
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            if (selectedImageUri != null) {
                progress.visibility = View.VISIBLE
                uploadToImageKit(selectedImageUri!!) { url ->
                    saveBookData(title, author, genre, description, url)
                }
            } else {
                saveBookData(title, author, genre, description, currentThumbnailUrl)
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val file = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        photoFile = file
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        takePhoto.launch(uri)
    }

    private fun uploadToImageKit(uri: Uri, onSuccess: (String) -> Unit) {
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val tempFile = java.io.File.createTempFile("cover_", ".jpg", cacheDir)
        tempFile.outputStream().use { inputStream.copyTo(it) }

        val privateKey = BuildConfig.IMAGEKIT_PRIVATE_KEY
        val auth = "Basic " + android.util.Base64.encodeToString(
            "$privateKey:".toByteArray(), android.util.Base64.NO_WRAP
        )

        val filePart = MultipartBody.Part.createFormData(
            "file", tempFile.name,
            okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), tempFile)
        )
        val fileNameBody = "cover_${System.currentTimeMillis()}.jpg"
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val folderBody = "/book_covers/"
            .toRequestBody("text/plain".toMediaTypeOrNull())

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://upload.imagekit.io/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ImageKitUploadApi::class.java)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val result = retrofit.uploadFile(auth, filePart, fileNameBody, folderBody)
                tempFile.delete()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(result.url)
                }
            } catch (e: Exception) {
                tempFile.delete()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    findViewById<MaterialButton>(R.id.btn_save).isEnabled = true
                    findViewById<ProgressBar>(R.id.progress_upload).visibility = View.GONE
                }
            }
        }
    }

    private fun saveBookData(title: String, author: String, genre: String, desc: String, url: String) {
        val date = editingBook?.dateAdded ?: SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val book = if (editingBook != null) {
            editingBook!!.copy(
                title = title,
                author = author,
                genre = genre,
                description = desc,
                thumbnail = url
            )
        } else {
            Book(
                title = title,
                author = author,
                genre = genre,
                description = desc,
                dateAdded = date,
                thumbnail = url
            )
        }
        if (editingBook != null) viewModel.update(book) else viewModel.insert(book)
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = if (language == "russian") Locale("ru") else Locale("en")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfig: Configuration) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = if (language == "russian") Locale("ru") else Locale("en")
        overrideConfig.setLocale(locale)
        super.applyOverrideConfiguration(overrideConfig)
    }
}