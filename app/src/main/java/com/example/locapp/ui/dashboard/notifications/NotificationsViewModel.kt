package com.example.locapp.ui.dashboard.notifications

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.utils.DBLoadData
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


class NotificationsViewModel : ViewModel() {

    private val firebaseManager = FirebaseManager()

    fun acceptOrder(orderId: Int) {
        firebaseManager.acceptOrder(
            orderId,
            onSuccess = {
                Log.d("NotificationsViewModel", "Order accepted successfully.")
            },
            onFailure = { e ->
                Log.e("NotificationsViewModel", "Failed to accept the order: $e")
            }
        )
    }

    fun updateOrderIt(orderId: Int){

    }
}

