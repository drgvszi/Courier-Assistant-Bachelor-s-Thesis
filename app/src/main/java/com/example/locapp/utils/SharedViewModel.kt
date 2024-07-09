package com.example.locapp.utils

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.BuildConfig
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.Users
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class SharedViewModel: ViewModel() {


    private val apiKey = BuildConfig.MAPS_API_KEY

    fun getApiKey():String
    {
        return apiKey
    }

    private val currentIterator = MutableLiveData(0)
    val iterator: LiveData<Int> = currentIterator

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    private val _acceptedOrders = MutableLiveData<List<Order>>()
    val acceptedOrders:LiveData<List<Order>> = _acceptedOrders

    private val _users = MutableLiveData<List<Users>>()
    val users: LiveData<List<Users>> = _users

    private val firebaseManager = FirebaseManager()
    val currentUserId = firebaseManager.publicCurrentUserId

    private val dbLoadDataManager = DBLoadData()

    init {
        dbLoadDataManager.loadOrders(_orders)
        dbLoadDataManager.loadUsers(_users)
        dbLoadDataManager.loadAcceptedOrders(_acceptedOrders)
    }

    fun sortOrders(allOrders: List<Order>, selectedOption: String): List<Order> {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return when (selectedOption) {
            "Newest" -> allOrders.sortedByDescending { format.parse(it.orderCreateDate) }
            "Oldest" -> allOrders.sortedBy { format.parse(it.orderCreateDate) }
            "User Id" -> allOrders.sortedBy { it.deliverymanId }
            "Order ID" -> allOrders.sortedBy { it.orderId }
            "Order Status" -> allOrders.sortedBy { it.orderStatus }
            "Pay status" -> allOrders.sortedBy { it.payStatus }
            "Delivery zone" -> allOrders.sortedBy { it.zoneDelivery }
            else -> allOrders
        }
    }

    fun sortOrdersNotf(allOrders: List<Order>, selectedOption: String): List<Order> {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return when (selectedOption) {
            "Newest" -> allOrders.sortedByDescending { format.parse(it.orderCreateDate) }
            "Oldest" -> allOrders.sortedBy { format.parse(it.orderCreateDate) }
            "Order ID" -> allOrders.sortedBy { it.orderId }
            "Pay status" -> allOrders.sortedBy { it.payStatus }
            "Delivery zone" -> allOrders.sortedBy { it.zoneDelivery }
            else -> allOrders
        }
    }

    fun filterOrders(allOrders: List<Order>, selectedOption: String): List<Order> {
        return when (selectedOption) {
            "None" -> allOrders
            "Paid" -> allOrders.filter { it.payStatus == "Paid"}
            "Unpaid" -> allOrders.filter { it.payStatus == "Unpaid" }
            "Order in progress" -> allOrders.filter { it.orderStatus == "In progress" }
            "Delayed orders" -> allOrders.filter { it.orderStatus == "Delayed" }
            else -> allOrders
        }
    }
}
