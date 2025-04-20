package com.example.fundoonotes.common.util

import com.example.fundoonotes.common.util.interfaces.FirestoreApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val firestoreApi: FirestoreApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://firestore.googleapis.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirestoreApi::class.java)
    }
}