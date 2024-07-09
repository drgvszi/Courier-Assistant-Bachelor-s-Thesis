package com.example.locapp.adminui.dashboard.users_handler

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.utils.FirebaseManager
import com.example.locapp.utils.datas.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date

class UsersHandlerViewModel : ViewModel() {

    private val firebaseManager = FirebaseManager()
    private val usersHandlerViewModelScope = CoroutineScope(Dispatchers.Main + Job())

    private val _userName = MutableLiveData<String>()
    private val _orderText = MutableLiveData<String>()
    private val _deliveryProgress = MutableLiveData<Float>()
    private val _deliveredVsFailed = MutableLiveData<String>()
    private val _failedOrdersCount = MutableLiveData<String>()
    private val _deliveredOrdersCount = MutableLiveData<String>()
    private val _deliveryProgressSuccesVsFailed = MutableLiveData<Float>()
    private val _distanceTraveledCount = MutableLiveData<String>()
    private val _timeWorkedCount = MutableLiveData<String>()

    val userName: LiveData<String> = _userName
    val orderText: LiveData<String> = _orderText
    val deliveryProgress: LiveData<Float> = _deliveryProgress
    val deliveredVsFailed: LiveData<String> = _deliveredVsFailed
    val deliveryProgressSuccesVsFailed: LiveData<Float> = _deliveryProgressSuccesVsFailed
    val distanceTraveledCount:LiveData<String> = _distanceTraveledCount
    val timeWorkedCount:LiveData<String> = _timeWorkedCount

    fun startStatsListener(userId: String, startDate: Date? = null, endDate: Date? = null) {
        firebaseManager.setStatsListener(
            scope = usersHandlerViewModelScope,
            onStatsUpdated = {
                fetchUserStats(userId)
            },
            onCancelled = { error ->
                Log.e("StatsViewModel", "Stats listener cancelled: ${error.message}")
            },
            startDate = startDate,
            endDate = endDate
        )
    }

    fun fetchUserStats(userId: String) {
        firebaseManager.getDeliverymanDetails(userId, onSuccess = { user ->
            _userName.postValue(user.userCompleteName)
            Log.d("FETCHDD", "hm ? $user")
            val deliveredOrders = user.deliverymanDetails.deliveredOrders
            val targetOrders = user.deliverymanDetails.targetOrders
            val expiredOrders = user.deliverymanDetails.expired
            val incompleteAddress = user.deliverymanDetails.incompleteAddress
            val deliveryRefused = user.deliverymanDetails.deliveryRefused
            val restrictedArea = user.deliverymanDetails.restrictedArea
            val expired = user.deliverymanDetails.expired
            val anotherStatus = user.deliverymanDetails.anotherStatus
            val totalKm = user.deliverymanDetails.totalKM
            val totalTimeWorked = user.deliverymanDetails.totalTimeWorked
            val failedDelivered = incompleteAddress + deliveryRefused + restrictedArea + expired + anotherStatus

            val progress = if (targetOrders != 0) {
                (deliveredOrders.toFloat() / targetOrders.toFloat()) * 100
            } else {
                0f
            }
            val totalDeliveries = deliveredOrders + failedDelivered
            val deliveredVsFailedProgress = if (totalDeliveries != 0) {
                (deliveredOrders.toFloat() / totalDeliveries.toFloat()) * 100f
            } else {
                0f
            }

            _deliveryProgressSuccesVsFailed.postValue(deliveredVsFailedProgress)
            _deliveryProgress.postValue(progress)
            _orderText.postValue("$deliveredOrders/$targetOrders")
            _deliveredVsFailed.postValue(("$deliveredOrders/ $failedDelivered/ $totalDeliveries"))
            _distanceTraveledCount.postValue("$totalKm")
            _timeWorkedCount.postValue(totalTimeWorked)
            _failedOrdersCount.postValue("$failedDelivered")
            _deliveredOrdersCount.postValue("$deliveredOrders")
        }, onFailure = { error ->
            Log.e("StatsViewModel", "Firebase Database Error: ${error.message}")
        })
    }

    fun deleteUser(user: Users) {
        firebaseManager.deleteUser(user.userId,
            onSuccess = {
                Log.d("OrdersHandlerViewModel", "Order deleted successfully.")
            },
            onFailure = { e ->
                Log.e("OrdersHandlerViewModel", "Failed to delete the order: $e")
            })
    }

    fun editUsers(
        user: Users,
        userName: String? = null,
        selectedUserPhoneNumber: String? = null,
        selectedUserOrdersTarget: Int? = null,
        selectedUserDeliveryZones: List<String>? = null
    ) {
        usersHandlerViewModelScope.launch(Dispatchers.IO) {
            try {
                val updateMap = mutableMapOf<String, Any>()
                userName?.takeIf { it.isNotBlank() }?.let { updateMap["userCompleteName"] = it }
                selectedUserPhoneNumber?.takeIf { it.isNotBlank() }?.let { updateMap["userPhonenumber"] = it }

                val updatedDeliverymanDetails = user.deliverymanDetails.copy(
                    targetOrders = selectedUserOrdersTarget ?: user.deliverymanDetails.targetOrders,
                    zonesAssigned = selectedUserDeliveryZones ?: user.deliverymanDetails.zonesAssigned
                )

                if (updatedDeliverymanDetails != user.deliverymanDetails) {
                    updateMap["deliverymanDetails"] = updatedDeliverymanDetails
                }

                if (updateMap.isNotEmpty()) {
                    firebaseManager.editUser(user.userId, updateMap,
                        onSuccess = {
                            Log.d("UsersHandlerViewModel", "User edited successfully.")
                        },
                        onFailure = { e ->
                            Log.e("UsersHandlerViewModel", "Failed to edit user: $e")
                        })
                } else {
                    Log.d("UsersHandlerViewModel", "No fields to update.")
                }
            } catch (e: Exception) {
                Log.e("UsersHandlerViewModel", "Failed to edit user: $e")
            }
        }
    }
}
