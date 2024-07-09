    package com.example.locapp.utils.datas

    data class Users(
        val userId: String = "",
        val userMail: String = "",
        val userCompleteName: String = "",
        val userPhonenumber: String = "",
        val userType: String = "delivery man",
        val deliverymanDetails: DeliverymanDetails = DeliverymanDetails()
    )

    data class DeliverymanDetails(
        val carAssigned: String = "",
        val carNumber: String = "",
        val acceptedOrders: Int = 0,
        val deliveredOrders: Int = 0,
        val targetOrders: Int = 0,
        val totalKM: Int = 0,
        val totalTimeWorked: String = "",
        val incompleteAddress: Int = 0,
        val deliveryRefused: Int = 0,
        val restrictedArea: Int = 0,
        val delayed: Int = 0,
        val expired: Int = 0,
        val anotherStatus: Int = 0,
        val completedOrdersSucces: Int = 0,
        val completedOrdersFailed: Int = 0,
        val zonesAssigned: List<String> = emptyList(),
        val timeWorked: List<TimeWorked> = emptyList()
    )

    data class TimeWorked(
        val timeWorked: String = "",
        val dateStartOfTimeWorked: String ="",
        val dateEndOfTimeWorked: String = "",
        val distanceTraveled: Float = 0f
    )