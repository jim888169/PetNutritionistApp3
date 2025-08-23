package com.example.petnutritionistapp.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class BcsRequest(
    val imageUrl: String,
    val ribs: String,
    val waist: String,
    val stomach: String
)

data class BcsResponse(
    @SerializedName("image_score") val imageScore: Int,
    @SerializedName("final_bcs")   val finalBcs: Int,
    val confidence: Double,
    val notes: String,
    @SerializedName("touch_scores") val touchScores: Map<String, Int>?
)

interface ApiService {
    @POST("bcs-score")
    fun getBcsScore(@Body body: BcsRequest): Call<BcsResponse>
}
