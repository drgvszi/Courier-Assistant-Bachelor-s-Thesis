package com.example.locapp.ui.dashboard.stats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.utils.FirebaseManager
import com.example.locapp.utils.datas.DeliverymanDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.*

class StatsViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    private val _orderText = MutableLiveData<String>()
    private val _deliveryProgress = MutableLiveData<Float>()
    private val _deliveredVsFailed = MutableLiveData<String>()
    private val _failedOrdersCount = MutableLiveData<String>()
    private val _deliveredOrdersCount = MutableLiveData<String>()
    private val _zonesAssigned = MutableLiveData<List<String>>()
    private val _deliveryProgressSuccesVsFailed = MutableLiveData<Float>()
    private val _refusedOrdersCount = MutableLiveData<String>()
    private val _restrictedAreaOrdersCount = MutableLiveData<String>()
    private val _expiredOrdersCount = MutableLiveData<String>()
    private val _anotherStatusOrdersCount = MutableLiveData<String>()
    private val _incompleteAddressOrdersCount = MutableLiveData<String>()
    private val _distanceTraveledCount = MutableLiveData<String>()
    private val _timeWorkedCount = MutableLiveData<String>()

    val userName: LiveData<String> = _userName
    val orderText: LiveData<String> = _orderText
    val deliveryProgress: LiveData<Float> = _deliveryProgress
    val deliveredVsFailed: LiveData<String> = _deliveredVsFailed
    val zonesAssigned: LiveData<List<String>> = _zonesAssigned
    val failedOrdersCount: LiveData<String> = _failedOrdersCount
    val deliveredOrdersCount: LiveData<String> = _deliveredOrdersCount
    val deliveryProgressSuccesVsFailed: LiveData<Float> = _deliveryProgressSuccesVsFailed
    val refusedOrdersCount:LiveData<String> = _refusedOrdersCount
    val restrictedAreaOrdersCount:LiveData<String> = _restrictedAreaOrdersCount
    val expiredOrdersCount:LiveData<String> = _expiredOrdersCount
    val anotherStatusOrdersCount:LiveData<String> = _anotherStatusOrdersCount
    val incompleteAddressOrdersCount:LiveData<String> = _incompleteAddressOrdersCount
    val distanceTraveledCount:LiveData<String> = _distanceTraveledCount
    val timeWorkedCount:LiveData<String> = _timeWorkedCount

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val firebaseManager = FirebaseManager()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        startStatsListener()
        fetchDeliverymanDetails()
    }

    fun setDateRange(startDate: Date? = null, endDate: Date? = null) {
        this.startDate = startDate
        this.endDate = endDate
        fetchDeliverymanDetails()
        startStatsListener()
    }

    private fun startStatsListener() {
        firebaseManager.setStatsListener(
            scope = viewModelScope,
            onStatsUpdated = {
                fetchDeliverymanDetails()
            },
            onCancelled = { error ->
                Log.e("StatsViewModel", "Stats listener cancelled: ${error.message}")
            },
            startDate = startDate,
            endDate = endDate
        )
    }

    private fun fetchDeliverymanDetails() {
        firebaseManager.getDeliverymanDetails(
            firebaseManager.publicCurrentUserId,
            onSuccess = { user ->
                _zonesAssigned.postValue(user.deliverymanDetails.zonesAssigned)
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
                _restrictedAreaOrdersCount.postValue("$restrictedArea")
                _expiredOrdersCount.postValue("$expiredOrders")
                _anotherStatusOrdersCount.postValue("$anotherStatus")
                _incompleteAddressOrdersCount.postValue("$incompleteAddress")
                _distanceTraveledCount.postValue("$totalKm")
                _timeWorkedCount.postValue(totalTimeWorked)
                _refusedOrdersCount.postValue("$deliveryRefused")
                _failedOrdersCount.postValue("$failedDelivered")
                _deliveredOrdersCount.postValue("$deliveredOrders")
            },
            onFailure = { error ->
                Log.e("StatsViewModel", "Firebase Database Error: ${error.message}")
                _deliveryProgress.postValue(0f)
                _orderText.postValue("0/0")
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}


