package com.example.booklibrary.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ImageKitUploadResult(val url: String, val fileId: String)

interface ImageKitUploadApi {
    @Multipart
    @POST("https://upload.imagekit.io/api/v1/files/upload")
    suspend fun uploadFile(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("folder") folder: RequestBody
    ): ImageKitUploadResult
}