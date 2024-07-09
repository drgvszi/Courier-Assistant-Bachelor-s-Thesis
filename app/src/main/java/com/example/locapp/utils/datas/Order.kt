package com.example.locapp.utils.datas

import com.example.locapp.utils.googleapis.LocationWrapper

data class Order(
    val deliverymanId: String = "",
    val orderUniqueId: String = "",
    val orderPin: Int = 0,
    val orderId: Int = 0,
    val orderClientName: String = "",
    val orderPhoneNumber:  String = "",
    val orderClientEmail: String = "",
    val orderName: String = "",
    val description: String =" ",
    val payType: String = "",
    val payStatus: String = "",
    val orderPrice: Int = 0,
    val takeToTheAddress: String = "",
    val getFromTheAddress: String = "",
    val takeToCoords: Coordinates = Coordinates(),
    val getFromCoords: Coordinates = Coordinates(),
    val zoneDelivery: String =" ",
    val orderCreateDate: String = "",
    val orderDeliveredDate: String = "",
    val limitOrderDate: String = "",
    val daysForDelivery: Int = 0,
    val orderStatus: String = "",
    val additionalInfo: String = "",
    val pickupStatus: String = "",
    val orderNumber: Int = 0,
    var index: Int = 0,
    var forceChecked: Boolean = false
)

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class RouteOrderInfo(
    val orderId: Int,
    val orderName: String?,
    val locationWrapper: LocationWrapper
)