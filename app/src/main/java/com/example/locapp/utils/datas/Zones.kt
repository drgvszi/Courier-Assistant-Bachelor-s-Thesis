package com.example.locapp.utils.datas

data class Zones(
    val zoneName: String = "",
    val zonePoints: List<Points> = emptyList()
)

data class Points(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

