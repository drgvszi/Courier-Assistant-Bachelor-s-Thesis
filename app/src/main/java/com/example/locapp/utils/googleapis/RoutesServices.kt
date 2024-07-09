package com.example.locapp.utils.googleapis
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RoutesServices {
    @POST("directions/v2:computeRoutes")
    fun getRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body requestBody: RoutesRequestBody
    ): Call<RoutesResponse>

    @POST("directions/v2:computeRoutes")
    fun getRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body requestBody: RoutesRequestBodyWithAddress
    ): Call<RoutesResponse>

    @POST("directions/v2:computeRoutes")
    fun getRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body requestBody: RoutesRequestBodyWithWaypoints
    ): Call<RoutesResponse>
}
