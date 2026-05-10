package com.example.booklibrary.data.repository

import android.util.Log
import com.example.booklibrary.data.db.Book
import com.example.booklibrary.data.db.BookDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RemoteBookRepository(private val bookDao: BookDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var snapshotListener: ListenerRegistration? = null

    private fun getUserCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("books")
    }

    suspend fun insertBook(book: Book) {
        val collection = getUserCollection() ?: return
        bookDao.insert(book)
        uploadToFirestore(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.insert(book)
        uploadToFirestore(book)
    }

    suspend fun deleteBook(book: Book) {
        val collection = getUserCollection() ?: return
        bookDao.delete(book)
        collection.document(book.uuid).delete().await()
    }

    private suspend fun uploadToFirestore(book: Book) {
        val collection = getUserCollection() ?: return
        val remoteData = hashMapOf(
            "uuid" to book.uuid,
            "title" to book.title,
            "author" to book.author,
            "description" to book.description,
            "genre" to book.genre,
            "dateAdded" to book.dateAdded,
            "thumbnail" to book.thumbnail
        )

        try {
            collection.document(book.uuid).set(remoteData).await()
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error saving to FB", e)
        }
    }

    fun startRealtimeSync(scope: CoroutineScope) {
        val collection = getUserCollection()
        if (collection == null) {
            return
        }
        snapshotListener?.remove()

        snapshotListener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null) {
                scope.launch(Dispatchers.IO) {
                    val remoteUuids = snapshot.documents.mapNotNull { it.getString("uuid") }
                    bookDao.deleteOrphans(remoteUuids)

                    snapshot.documents.forEach { doc ->
                        val book = Book(
                            id = 0,
                            uuid = doc.getString("uuid") ?: doc.id,
                            title = doc.getString("title") ?: "",
                            author = doc.getString("author") ?: "",
                            description = doc.getString("description") ?: "",
                            genre = doc.getString("genre") ?: "",
                            dateAdded = doc.getString("dateAdded") ?: "",
                            thumbnail = doc.getString("thumbnail") ?: ""
                        )
                        bookDao.insert(book)
                    }
                }
            }
        }
    }

    fun stopRealtimeSync() {
        snapshotListener?.remove()
    }
}