package com.example.fundoonotes.common.util.interfaces

import com.example.fundoonotes.common.data.model.FirestoreNoteRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FirestoreApi {

    @POST("projects/{projectId}/databases/(default)/documents/notes")
    fun addNote(
        @Header("Authorization") authHeader: String,
        @Path("projectId") projectId: String,
        @Body body: FirestoreNoteRequest
    ): Call<ResponseBody>
}