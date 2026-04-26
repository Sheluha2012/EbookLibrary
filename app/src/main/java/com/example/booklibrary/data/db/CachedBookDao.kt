package com.example.booklibrary.data.db

import androidx.room.*

@Dao
interface CachedBookDao {
    @Query("""
        SELECT * FROM cached_books 
        WHERE title LIKE '%' || :query || '%' 
        OR author LIKE '%' || :query || '%'
        ORDER BY cachedAt DESC
    """)
    suspend fun searchByQuery(query: String): List<CachedBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<CachedBook>)

    @Query("DELETE FROM cached_books")
    suspend fun clearAll()
}