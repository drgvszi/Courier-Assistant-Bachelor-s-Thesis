package com.example.locapp.utils.googleapis

import com.google.gson.annotations.SerializedName

data class RoutesRequestBody(
    @SerializedName("origin") val origin: LocationWrapper,
    @SerializedName("destination") val destination: LocationWrapper,
    val travelMode: String = "DRIVE",
    val computeAlternativeRoutes: Boolean = false,
    val units: String = "IMPERIAL"
)

data class RoutesRequestBodyWithAddress(
    @SerializedName("origin") val origin: AddressesWrapper,
    @SerializedName("destination") val destination: AddressesWrapper,
    val travelMode: String = "DRIVE",
    val computeAlternativeRoutes: Boolean = false,
    val units: String = "IMPERIAL"
)

data class RoutesRequestBodyWithWaypoints(
    @SerializedName("origin") val origin: LocationWrapper,
    @SerializedName("destination") val destination: LocationWrapper,
    @SerializedName("intermediates") val intermediates: List<LocationWrapper>,
    val travelMode: String = "DRIVE",
    val computeAlternativeRoutes: Boolean = false,
    val units: String = "IMPERIAL"
)

sealed class RoutesRequestBodyType {
    data class WithWaypoints(val body: RoutesRequestBodyWithWaypoints) : RoutesRequestBodyType()
    data class WithoutWaypoints(val body: RoutesRequestBody) : RoutesRequestBodyType()
}