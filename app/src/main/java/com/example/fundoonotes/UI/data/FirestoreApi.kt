package com.example.fundoonotes.UI.data

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface FirestoreApi {

    @POST("projects/{projectId}/databases/(default)/documents/notes")
    fun addNote(
        @Header("Authorization") authHeader: String,
        @Path("projectId") projectId: String,
        @Body body: FirestoreNoteRequest
    ): Call<ResponseBody>
}
