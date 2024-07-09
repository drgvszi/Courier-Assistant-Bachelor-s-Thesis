package com.example.locapp.utils.googleapis

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://routes.googleapis.com/"
    private const val BASE_URL_GEOCODING = "https://maps.googleapis.com/"
    private val retrofitRoutes: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val retrofitGeocoding: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL_GEOCODING)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val routesServices: RoutesServices = retrofitRoutes.create(RoutesServices::class.java)
    val geocodingServices: GeocodingService = retrofitGeocoding.create(GeocodingService::class.java)
}