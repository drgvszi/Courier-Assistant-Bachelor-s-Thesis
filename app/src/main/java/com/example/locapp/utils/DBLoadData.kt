package com.example.locapp.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.RouteOrderInfo
import com.example.locapp.utils.datas.Users
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.LocationWrapper
import com.example.locapp.utils.googleapis.RoutesResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DBLoadData {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())
    private val firebaseManager = FirebaseManager()

    internal fun loadOrders(orders:MutableLiveData<List<Order>>) {
        viewModelScope.launch {
            try {
                firebaseManager.setOrdersListener(
                    scope = viewModelScope,
                    onOrdersChange = { orderList ->
                        orders.postValue(orderList)
                    },
                    onCancelled = { error ->
                        Log.e("Firebase",
                            "Firebase Database Error: ${error.message}",
                            error.toException()
                        )
                    }
                )
            } catch(e: Exception) {
                Log.e("DBLoader Orders ERROR FOUND!","Something is wrong: $e")
            }
        }
    }

    internal fun loadUsers( users:MutableLiveData<List<Users>> ) {
        viewModelScope.launch {
            try {
                firebaseManager.setUsersListener(
                    scope = viewModelScope,
                    onUsersChange = { userList ->
                        users.postValue(userList)
                    },
                    onCancelled = { error ->
                        Log.e("UsersHandlerViewModel",
                            "Firebase Database Error: ${error.message}",
                            error.toException()
                        )
                    }
                )
            } catch(e: Exception) {
                Log.e("DBLoader Users ERROR FOUND!","Something is wrong: $e")
            }
        }
    }

    internal fun loadAcceptedOrders(orders:MutableLiveData<List<Order>>) {
        viewModelScope.launch {
            try {
                firebaseManager.setOrdersListener(
                    scope = viewModelScope,
                    onOrdersChange = { orderList ->
                        val filteredOrders = orderList.filter {
                            it.deliverymanId == firebaseManager.publicCurrentUserId &&
                                    it.orderStatus == "Accepted"
                        }
                        orders.postValue(filteredOrders)
                    },
                    onCancelled = { error ->
                        Log.e("Firebase",
                            "Firebase Database Error: ${error.message}",
                            error.toException()
                        )
                    }
                )
            } catch(e: Exception) {
                Log.e("DBLoader Orders ERROR FOUND!","Something is wrong: $e")
            }
        }
    }


    internal fun loadZones(zones:MutableLiveData<List<Zones>>){
        viewModelScope.launch {
            try {
                firebaseManager.setZonesListener(
                    scope = viewModelScope,
                    onZonesChange = { zoneList ->
                        zones.postValue(zoneList)
                    },
                    onCancelled = { error ->
                        Log.e("UsersHandlerViewModel",
                            "Firebase Database Error: ${error.message}",
                            error.toException()
                        )
                    }
                )
            } catch(e: Exception) {
                Log.e("DBLoader Users ERROR FOUND!","Something is wrong: $e")
            }
        }
    }

    /*internal fun loadRoutes(routesResponse: MutableLiveData<RoutesResponse>,
                            ordersIdsList:MutableLiveData< List<Int>>,
                            intermediateHashMap: MutableLiveData<HashMap<Int, LocationWrapper>>
    ) {
        viewModelScope.launch {
            try {
                firebaseManager.loadRouteResponse(
                    scope = viewModelScope,
                    onSuccess = { routeResp, ordersIds, intermediateMap ->
                        routesResponse.postValue(routeResp)
                        ordersIdsList.postValue((ordersIds))
                        intermediateHashMap.postValue((intermediateMap))
                    },
                    onFailure = { error ->
                        Log.e("DBLoadData", "Failed to load routes: $error")
                    }
                )
            } catch (e: Exception) {
                Log.e("DBLoadData", "Error loading routes: $e")
            }
        }
    }*/
}