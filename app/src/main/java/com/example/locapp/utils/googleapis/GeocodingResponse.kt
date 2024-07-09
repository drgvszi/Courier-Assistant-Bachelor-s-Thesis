package com.example.locapp.utils.googleapis

data class GeocodingResult(
    val results: List<Result>,
    val status: String
)

data class Result(
    val geometry: Geometry,
    val formatted_address: String,
)

data class Geometry(
    val location: Geolocation
)

data class Geolocation(
    val lat: Double,
    val lng: Double
)
