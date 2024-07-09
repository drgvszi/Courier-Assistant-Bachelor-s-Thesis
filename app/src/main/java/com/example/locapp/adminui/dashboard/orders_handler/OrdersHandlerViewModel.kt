package com.example.locapp.adminui.dashboard.orders_handler

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.BuildConfig
import com.example.locapp.utils.DBLoadData
import com.example.locapp.utils.FirebaseManager
import com.example.locapp.utils.datas.Coordinates
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OrdersHandlerViewModel : ViewModel() {

    private val firebaseManager = FirebaseManager()
    private val ordersHandlerViewModelScope = CoroutineScope(Dispatchers.Main + Job())
    private val apiKey = BuildConfig.MAPS_API_KEY

    private fun fetchCoordinates(address: String): Coordinates {
        val geocodingResponse = RetrofitClient.geocodingServices.getAddressCoordinates(address, apiKey).execute()
        if (geocodingResponse.isSuccessful && geocodingResponse.body()?.status == "OK") {
            val respLoc = geocodingResponse.body()?.results?.get(0)?.geometry?.location
            if (respLoc != null) {
                return Coordinates(respLoc.lat, respLoc.lng)
            }
        }
        throw RuntimeException("Failed to fetch coordinates")
    }

    private fun checkTheZoneOfAddress(zones: List<Zones>, addressCoordinates: Coordinates): String {
        val addressLatLng = LatLng(addressCoordinates.latitude, addressCoordinates.longitude)

        for (zone in zones) {
            val polygonCoordinates = zone.zonePoints.map { LatLng(it.latitude, it.longitude) }

            if (PolyUtil.containsLocation(addressLatLng, polygonCoordinates, true)) {
                return zone.zoneName
            }
        }
        return "Unknown zone"
    }

    fun deleteOrder(order: Order) {
        firebaseManager.deleteOrder(order.orderId,
            onSuccess = {
                Log.d("OrdersHandlerViewModel", "Order deleted successfully.")
            },
            onFailure = { e ->
                Log.e("OrdersHandlerViewModel", "Failed to delete the order: $e")
            })
    }

    fun editOrder(
        order: Order,
        zones: List<Zones>,
        orderClientName: String? = null,
        orderClientEmail: String? = null,
        orderPhone: String? = null,
        orderPayType: String? = null,
        orderPayStatus: String? = null,
        orderPrice: Int? = 0,
        orderUserId: String? = null,
        orderDescription: String? = null,
        orderDeliveryAddress: String? = null,
        orderStatus: String? = null,
        orderAdditionalInfo: String? = null,
        limitOrderDate: String? = null,
        forceChecked: Boolean
    ) {
        ordersHandlerViewModelScope.launch(Dispatchers.IO) {
            try {
                val updateMap = mutableMapOf<String, Any>()

                Log.d("forcecheck", "$forceChecked")

                orderClientName?.takeIf { it.isNotBlank() }?.let {
                    updateMap["orderClientName"] = it
                }
                orderClientEmail?.takeIf { it.isNotBlank() }?.let {
                    updateMap["orderClientEmail"] = it
                }
                orderPhone?.takeIf { it.isNotBlank() }?.let {
                    updateMap["orderPhoneNumber"] = it
                }
                orderPayType?.takeIf { it.isNotBlank() }?.let {
                    updateMap["payType"] = it
                }
                orderPayStatus?.takeIf { it.isNotBlank() }?.let {
                    updateMap["payStatus"] = it
                }
                orderPrice?.let {
                    updateMap["orderPrice"] = it
                }
                orderUserId?.takeIf { it.isNotBlank() }?.let {
                    updateMap["deliverymanId"] = it
                }
                orderDescription?.takeIf { it.isNotBlank() }?.let {
                    updateMap["description"] = it
                }
                orderStatus?.takeIf { it.isNotBlank() }?.let {
                    updateMap["orderStatus"] = it
                    if (it != order.orderStatus) {
                        updateMap["orderDeliveredDate"] = ""
                        updateMap["additionalInfo"] = ""
                    }
                }
                orderAdditionalInfo?.let {
                    updateMap["additionalInfo"] = it
                }
                limitOrderDate?.let {
                    updateMap["limitOrderDate"] = it
                    if (order.orderStatus == "Expired") {
                        updateMap["deliverymanId"] = ""
                        updateMap["orderStatus"] = "In progress"
                        updateMap["additionalInfo"] = ""
                        updateMap["orderDeliveredDate"] = ""
                    }
                }
                if (!orderDeliveryAddress.isNullOrBlank()) {
                    updateMap["takeToTheAddress"] = orderDeliveryAddress
                    val coordinates = fetchCoordinates(orderDeliveryAddress)
                    val deliverymanZone = checkTheZoneOfAddress(zones, coordinates)
                    updateMap["takeToCoords"] = coordinates
                    updateMap["zoneDelivery"] = deliverymanZone
                }
                updateMap["forceChecked"] = forceChecked

                if (updateMap.isNotEmpty()) {
                    firebaseManager.editOrder(order.orderId, updateMap,
                        onSuccess = {
                            Log.d("OrdersHandlerViewModel", "Order edited successfully.")
                        },
                        onFailure = { e ->
                            Log.e("OrdersHandlerViewModel", "Failed to edit the order: $e")
                        })
                } else {
                    Log.d("OrdersHandlerViewModel", "No fields to update.")
                }
            } catch (e: Exception) {
                Log.e("OrdersHandlerViewModel", "Failed to fetch coordinates: $e")
            }
        }
    }
}
