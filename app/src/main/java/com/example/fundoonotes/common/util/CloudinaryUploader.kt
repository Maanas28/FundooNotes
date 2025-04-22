package com.example.fundoonotes.common.util

import com.cloudinary.Cloudinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CloudinaryUploader {

    private const val CLOUD_NAME = "dbhrxjv8u"
    private const val UPLOAD_PRESET = "Profile_Images"

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to CLOUD_NAME,
            "secure" to true,
            "api_key" to "799243587245965",
            "api_secret" to "ZvIwlxGJSdsh64Ec5zTCl9z1MZQ"
        )
    )
    suspend fun uploadImage(file: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val result = cloudinary.uploader().upload(
                    file,
                    mapOf("upload_preset" to UPLOAD_PRESET)
                )
                result["secure_url"] as? String
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}