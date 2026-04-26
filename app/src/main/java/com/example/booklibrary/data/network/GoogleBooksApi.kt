package com.example.booklibrary.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("langRestrict") lang: String = "ru"
    ): BookSearchResponse
}