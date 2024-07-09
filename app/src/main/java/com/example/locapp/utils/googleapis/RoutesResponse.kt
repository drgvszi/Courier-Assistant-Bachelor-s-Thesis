package com.example.locapp.utils.googleapis

import com.google.gson.annotations.SerializedName

data class RoutesResponse(
    val routes: List<Route>,
    val geocodingResults: List<GeocodedWaypoint>
)

data class Route(
    val legs: List<RouteLeg>,
    val routeLabels: List<String>,
    val distanceMeters: Int,
    val duration: String,
    val staticDuration: String,
    val polyline: RoutePolyline,
    val description: String?
)

data class RouteLeg(
    val distanceMeters: Int,
    val duration: String,
    val staticDuration: String,
    val polyline: RoutePolyline,
    var startLocation: Location,
    val endLocation: Location,
    val steps: List<RouteLegStep>,
    var isActive: Boolean = false,
    var polylineReference: com.google.android.gms.maps.model.Polyline? = null,
    var isCompleted: Boolean = false,
)

data class RouteLegStep(
    val distanceMeters: Int,
    val staticDuration: String,
    val polyline: RoutePolyline,
    val startLocation: Location,
    val endLocation: Location
)

data class RoutePolyline(
    @SerializedName("encodedPolyline") val encodedPolyline: String
)

data class GeocodedWaypoint(
    val placeId: String,
    val geocoderStatus: String?,
    val type: List<String>,
    val intermediateWaypointRequestIndex: Int?
)

data class AddressesWrapper(
    @SerializedName("address") val address: String
)

data class LocationWrapper(
    @SerializedName("location") val location: Location
)

data class Location(
    @SerializedName("latLng") val latLng: MyLatLng
)

data class MyLatLng(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)