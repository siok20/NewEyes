package com.neweyes.chat.gemini

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface GeminiApi {
    @Multipart
    @POST("/image-chat-gemini/")
    fun sendImageAndPrompt(
        @Part file: MultipartBody.Part,
        @Part("prompt") prompt: RequestBody
    ): Call<ImageChatResponse>
}
