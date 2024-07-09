package com.example.locapp.adminui.dashboard.order_creator

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.BuildConfig
import com.example.locapp.utils.FirebaseManager
import com.example.locapp.utils.datas.Coordinates
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

class OrdersCreatorViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Add order details:"
    }
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val firebaseManager = FirebaseManager()

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

    fun fetchedCoordinates(address: String): Coordinates {
        return fetchCoordinates(address)
    }

    fun checkTheZoneOfAddress(zones: List<Zones>, addressCoordinates: Coordinates): String {
        val addressLatLng = LatLng(addressCoordinates.latitude, addressCoordinates.longitude)

        for (zone in zones) {
            val polygonCoordinates = zone.zonePoints.map { LatLng(it.latitude, it.longitude) }

            if (PolyUtil.containsLocation(addressLatLng, polygonCoordinates, true)) {
                return zone.zoneName
            }
        }
        return "Unknown"
    }

    fun createOrder(orderClientName: String, orderPhoneNumber: String, clientEmail: String, description: String,
        payType: String, payStatus: String, orderPrice: Int, takeToCoords: Coordinates, nrDaysForDelivery: Int,
        takeToTheAddress: String, zoneDelivery: String, forceChecked: Boolean) {

        firebaseManager.createOrder(
            orderClientName, orderPhoneNumber, clientEmail, description, payType, payStatus,
            nrDaysForDelivery, orderPrice, takeToTheAddress, takeToCoords, zoneDelivery, forceChecked,
            onSuccess = {
                Log.d("OrdersCreatorViewModel", "Order created successfully.")
            },
            onFailure = { e ->
                Log.e("OrdersCreatorViewModel", "Failed to create the order: $e")
            }
        )

    }
    val text: LiveData<String> = _text

}
