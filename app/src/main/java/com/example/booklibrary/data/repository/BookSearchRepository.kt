package com.example.booklibrary.data.repository

import android.content.Context
import com.example.booklibrary.data.db.BookDatabase
import com.example.booklibrary.data.db.CachedBook
import com.example.booklibrary.data.network.BookItem
import com.example.booklibrary.data.network.ImageLinks
import com.example.booklibrary.data.network.RetrofitInstance
import com.example.booklibrary.data.network.VolumeInfo

class BookSearchRepository(context: Context) {

    private val cachedBookDao = BookDatabase.getDatabase(context).cachedBookDao()

    suspend fun searchBooks(query: String, isOnline: Boolean): List<BookItem> {
        return if (isOnline) {
            try {
                val results = RetrofitInstance.api.searchBooks(query).items ?: emptyList()
                val cached = results.map { item ->
                    CachedBook(
                        id = item.id,
                        title = item.volumeInfo.title ?: "",
                        author = item.volumeInfo.authors?.joinToString(", ") ?: "",
                        description = item.volumeInfo.description ?: "",
                        genre = item.volumeInfo.categories?.firstOrNull() ?: "",
                        thumbnail = item.volumeInfo.imageLinks?.thumbnail
                            ?.replace("http://", "https://") ?: ""
                    )
                }
                cachedBookDao.clearAll()
                cachedBookDao.insertAll(cached)
                results
            } catch (e: Exception) {
                getCachedAsBookItems(query)
            }
        } else {
            getCachedAsBookItems(query)
        }
    }

    private suspend fun getCachedAsBookItems(query: String): List<BookItem> {
        return cachedBookDao.searchByQuery(query).map { cached ->
            BookItem(
                id = cached.id,
                volumeInfo = VolumeInfo(
                    title = cached.title,
                    authors = listOf(cached.author),
                    description = cached.description,
                    categories = if (cached.genre.isNotEmpty()) listOf(cached.genre) else null,
                    imageLinks = if (cached.thumbnail.isNotEmpty())
                        ImageLinks(cached.thumbnail)
                    else null
                )
            )
        }
    }
}