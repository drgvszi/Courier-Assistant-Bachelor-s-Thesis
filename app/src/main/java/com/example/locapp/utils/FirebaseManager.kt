package com.example.locapp.utils

import android.icu.util.Calendar
import android.util.Log
import com.example.locapp.utils.datas.Coordinates
import com.example.locapp.utils.datas.DeliverymanDetails
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.TimeWorked
import com.example.locapp.utils.datas.Users
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.LocationWrapper
import com.example.locapp.utils.googleapis.RoutesResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FirebaseManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private val ordersRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("Orders")
    private val usersRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("Users")
    private val zonesRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("Zones")
    private val routesRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("Routes")
    private val dateHelper = DateHelper()

    val publicCurrentUserId = getCurrentUserId()

    private fun getCurrentUserId(): String? {
        return currentUserId
    }

    fun checkIfUserIsAdmin(callback: (isAdmin: Boolean) -> Unit) {
        Log.d("Admin", "User_$currentUserId")
        if (currentUserId == null) {
            callback(false)
            return
        }

        val userRef = usersRef.child("User_$currentUserId")
        Log.d("Admin", "User_$currentUserId")
        userRef.child("userType").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userType = snapshot.getValue(String::class.java)
                callback(userType == "admin")
            }
            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun setZonesListener(
        scope: CoroutineScope,
        onZonesChange: (List<Zones>) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                zonesRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val zoneList = mutableListOf<Zones>()
                        for (childSnapshot in snapshot.children) {
                            with(childSnapshot.getValue(Zones::class.java)) {
                                this?.let { zoneList.add(it) }
                            }
                        }
                        onZonesChange.invoke(zoneList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onCancelled.invoke(error)
                    }
                })
            } catch(e: Exception) {
                Log.e("Zones Listener", "Something went wrong: $e")
            }
        }
    }

    //Notifications
    fun setOrdersListener(
        scope: CoroutineScope,
        onOrdersChange: (List<Order>) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                ordersRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val orderList = mutableListOf<Order>()
                        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val currentTime = Calendar.getInstance().time
                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            order?.let {
                                try {
                                    val limitOrderDate = dateFormatter.parse(it.limitOrderDate)
                                    if (limitOrderDate != null && limitOrderDate.before(currentTime) && (it.orderStatus=="In progress" || it.orderStatus == "Accepted")) {
                                        val orderRef = orderSnapshot.ref
                                        val orderDeliveredDate = dateFormatter.format(currentTime)

                                        val updates = mapOf<String, Any>(
                                            "additionalInfo" to "Reached the time limit",
                                            "orderStatus" to "Expired",
                                            "orderDeliveredDate" to it.limitOrderDate
                                        )
                                        orderRef.updateChildren(updates).addOnSuccessListener {
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Order Parsing", "Error parsing date for order ${it.orderId}: $e")
                                }
                                orderList.add(it)
                            }
                        }
                        onOrdersChange.invoke(orderList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onCancelled.invoke(error)
                    }
                })
            } catch (e: Exception) {
                Log.e("Orders Listener", "Something went wrong: $e")
            }
        }
    }

    fun setStatsListener(
        scope: CoroutineScope,
        onStatsUpdated: () -> Unit,
        onCancelled: (DatabaseError) -> Unit,
        startDate: Date? = null,
        endDate: Date? = null
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                ordersRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentUserId = auth.currentUser?.uid
                        updateUserStats(currentUserId, startDate, endDate)
                        updateUserWorkStats(currentUserId, startDate, endDate)
                        onStatsUpdated.invoke()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onCancelled.invoke(error)
                    }
                })
            } catch (e: Exception) {
                Log.e("Stats Listener", "Something went wrong: $e")
            }
        }
    }

    private fun updateUserStats(userId: String?, startDate: Date?, endDate: Date?) {
        if (userId == null) return
        val statusNodeMap = mapOf(
            "Accepted" to "acceptedOrders",
            "Delivered" to "deliveredOrders",
            "Incomplete address" to "incompleteAddress",
            "Delivery refused" to "deliveryRefused",
            "Restricted area" to "restrictedArea",
            "Expired" to "expired",
            "Another status" to "anotherStatus"
        )
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val deliverymanRef = usersRef.child("User_$userId").child("deliverymanDetails")
        statusNodeMap.forEach { (orderStatus, nodeName) ->
            val query = ordersRef.orderByChild("orderStatus").equalTo(orderStatus)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var orderStatusCount = 0
                    snapshot.children.forEach { orderSnapshot ->
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.deliverymanId == userId) {
                            val orderDeliveredDateString = order.orderDeliveredDate
                            if (orderDeliveredDateString.isNotBlank()) {
                                val orderDeliveredDate = dateFormatter.parse(orderDeliveredDateString)
                                if (orderDeliveredDate != null) {
                                    val isInDateRange = when {
                                        startDate != null && endDate != null -> {
                                            // Case 1: Date range specified
                                            dateHelper.isDateInRange(orderDeliveredDate, startDate, endDate)
                                        }
                                        startDate != null && endDate == null -> {
                                            // Case 2: Single day selection
                                            dateHelper.isSameDay(orderDeliveredDate, startDate)
                                        }
                                        else -> true // No dates specifid, return all orders
                                    }
                                    if (isInDateRange) {
                                        orderStatusCount++
                                    }
                                }
                            }
                        }
                    }
                    deliverymanRef.child(nodeName).setValue(orderStatusCount)
                        .addOnSuccessListener {
                            Log.d("Stats Update", "Successfully updated $orderStatus count: $orderStatusCount")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Stats Update", "Failed to update $orderStatus count: ${e.message}")
                        }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Stats Update", "Database query cancelled: ${error.message}")
                }
            })
        }
    }

    private fun updateUserWorkStats(userId: String?, startDate: Date?, endDate: Date?) {
        if (userId == null) return

        val deliverymanRef = usersRef.child("User_$userId").child("deliverymanDetails")

        val timeWorkedRef = deliverymanRef.child("timeWorked")

        timeWorkedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Initialize variables to accumulate totals
                var totalKM = 0
                var totalTimeSeconds = 0
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                snapshot.children.forEach { timeWorkedSnapshot ->
                    val timeWorked = timeWorkedSnapshot.getValue(TimeWorked::class.java)
                    if (timeWorked != null) {
                        val dateEndOfTimeWorked = dateFormatter.parse(timeWorked.dateEndOfTimeWorked)
                        if (dateEndOfTimeWorked != null) {
                            if (startDate != null && endDate != null) {
                                // Case 1: Date range specified
                                if (dateHelper.isDateInRange(dateEndOfTimeWorked, startDate, endDate)) {
                                    totalKM += timeWorked.distanceTraveled.toInt()
                                    totalTimeSeconds += dateHelper.parseTimeToSeconds(timeWorked.timeWorked)
                                }
                            } else if (startDate != null && endDate == null) {
                                // Case 2: Single day selection
                                if (dateHelper.isSameDay(dateEndOfTimeWorked, startDate)) {
                                    totalKM += timeWorked.distanceTraveled.toInt()
                                    totalTimeSeconds += dateHelper.parseTimeToSeconds(timeWorked.timeWorked)
                                }
                            } else {
                                // Case 3: No dates specified, sum all
                                totalKM += timeWorked.distanceTraveled.toInt()
                                totalTimeSeconds += dateHelper.parseTimeToSeconds(timeWorked.timeWorked)
                            }
                        }
                    }
                }

                val totalTimeWorked = dateHelper.formatTime(totalTimeSeconds)
                deliverymanRef.child("totalKM").setValue(totalKM)
                    .addOnSuccessListener {
                        Log.d("Work Stats Update", "Successfully updated total distance traveled: $totalKM")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Work Stats Update", "Failed to update total distance traveled: ${e.message}")
                    }

                deliverymanRef.child("totalTimeWorked").setValue(totalTimeWorked)
                    .addOnSuccessListener {
                        Log.d("Work Stats Update", "Successfully updated total time worked: $totalTimeWorked")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Work Stats Update", "Failed to update total time worked: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Fetch TimeWorked", "Failed to fetch time worked list: ${error.message}")
            }
        })
    }

    fun updateTimeWorked(
        timeWorked: Long,
        startTime: String,
        distanceTraveled: Float,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            val rightNow = Calendar.getInstance()
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateEndOfTimeWorked = dateFormatter.format(rightNow.time)

            val seconds = (timeWorked / 1000) % 60
            val minutes = (timeWorked / (1000 * 60)) % 60
            val hours = (timeWorked / (1000 * 60 * 60)) % 24
            val formattedTimeWorked = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            val timeWorkedRef = usersRef.child("User_$currentUserId")
                .child("deliverymanDetails")
                .child("timeWorked")

            timeWorkedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newIndex = dataSnapshot.childrenCount.toInt() // Get current count of children
                    val newTimeWorked = TimeWorked(
                        formattedTimeWorked,
                        startTime,
                        dateEndOfTimeWorked,
                        distanceTraveled / 1000
                    )

                    Log.d("UPDATE_TIME_WORKED", "imeWorked: $newTimeWorked")
                    timeWorkedRef.child(newIndex.toString()).setValue(newTimeWorked)
                        .addOnSuccessListener {
                            onSuccess.invoke()
                        }
                        .addOnFailureListener { e ->
                            onFailure.invoke(e)
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onFailure.invoke(databaseError.toException())
                }
            })
        }
    }

    fun getDeliverymanDetails( userId:String? ,onSuccess: (Users) -> Unit, onFailure: (Exception) -> Unit) {
        usersRef.child("User_$userId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val details = snapshot.getValue(Users::class.java)
                    if (details != null) {
                        onSuccess.invoke(details)
                    } else {
                        onFailure.invoke(NullPointerException("Deliveryman details not found"))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    onFailure.invoke(error.toException())
                }
            })
    }

    fun setUsersListener(
        scope: CoroutineScope,
        onUsersChange: (List<Users>) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                usersRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userList = mutableListOf<Users>()
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(Users::class.java)
                            user?.let {
                                userList.add(it)
                            }
                        }
                        onUsersChange.invoke(userList)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        onCancelled.invoke(error)
                    }
                })
            } catch(e: Exception) {
                Log.e("Users Listener", "Something went wrong: $e")
            }
        }
    }

    fun updateOrderIndex(orderId: Int, newIndex: Int) {
        val orderRef = ordersRef.child("Order_$orderId")
        // Update the index in Firebase when the orders is moved by drag&drop
        orderRef.child("index").setValue(newIndex)
            .addOnSuccessListener {
                Log.d("AccOrdViewModel", "Order index updated successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("AccOrdViewModel", "Failed to update order index: $e")
            }
    }

    //Administrator operations: Create, edit, delete
    fun createOrder(
        orderClientName: String,
        orderPhoneNumber: String,
        orderClientEmail: String,
        description: String,
        payType: String,
        payStatus: String,
        daysForDelivery: Int,
        orderPrice: Int,
        takeToTheAddress: String,
        takeToCoords: Coordinates,
        zoneDelivery: String,
        forceChecked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        generateUniqueOrderId { uniqueOrderId, error ->
            if (error != null) {
                onFailure.invoke(error)
                return@generateUniqueOrderId
            }

            val orderRef = ordersRef.child("Order_$uniqueOrderId")

            val rightNow = Calendar.getInstance()
            val limitDate = (rightNow.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, daysForDelivery)
            }

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val orderCreateDate = dateFormatter.format(rightNow.time)
            val limitOrderDate = dateFormatter.format(limitDate.time)

            val randomPin = (100000..999999).random()

            val orderData = mapOf(
                "orderClientName" to orderClientName,
                "orderPhoneNumber" to orderPhoneNumber,
                "orderClientEmail" to orderClientEmail,
                "orderId" to uniqueOrderId,
                "orderStatus" to "In progress",
                "pickupStatus" to "Waiting for pickup",
                "takeToCoords" to takeToCoords,
                "description" to description,
                "payType" to payType,
                "payStatus" to payStatus,
                "orderPrice" to orderPrice,
                "takeToTheAddress" to takeToTheAddress,
                "getFromTheAddress" to "",
                "orderCreateDate" to orderCreateDate,
                "limitOrderDate" to limitOrderDate,
                "daysForDelivery" to daysForDelivery,
                "orderPin" to randomPin,
                "zoneDelivery" to zoneDelivery,
                "forceChecked" to forceChecked
            )

            ordersRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    currentData.child(orderRef.key!!).value = orderData
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        onFailure.invoke(error.toException())
                    } else if (committed) {
                        onSuccess.invoke()
                    } else {
                        onFailure.invoke(Exception("Transaction error"))
                    }
                }
            })
        }
    }

    private fun generateUniqueOrderId(callback: (Int?, Exception?) -> Unit) {
        //Generate Unique ID, if it exists already in RTDB, try to generate it again
        fun tryGenerateAndCheck() {
            val randomId = (10000000..99999999).random()
            ordersRef.child("Order_$randomId").get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(randomId, null)
                } else {
                    tryGenerateAndCheck()
                }
            }.addOnFailureListener { e ->
                callback(null, e)
            }
        }
        tryGenerateAndCheck()
    }

    fun deleteOrder(orderId: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = ordersRef.child("Order_$orderId")
        orderRef.removeValue()
            .addOnSuccessListener {
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                onFailure.invoke(e)
            }
    }

    fun editOrder(orderId: Int, updateMap: Map<String, Any>,
                  onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = ordersRef.child("Order_$orderId")

        orderRef.updateChildren(updateMap)
            .addOnSuccessListener {
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                onFailure.invoke(e)
            }
    }

    fun editUser(userId: String, updateMap: Map<String, Any>,
                 onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRef = usersRef.child("User_$userId")
        userRef.updateChildren(updateMap)
            .addOnSuccessListener {
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                onFailure.invoke(e)
            }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRef = usersRef.child("User_$userId")
        userRef.removeValue()
            .addOnSuccessListener {
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                onFailure.invoke(e)
            }
    }

    fun saveZone(
        zone: Zones,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        zonesRef.get().addOnSuccessListener { dataSnapshot ->
            val zoneId = dataSnapshot.childrenCount + 1
            val zoneRef = zonesRef.child("Zone $zoneId")
            val zoneData = mapOf(
                "zoneName" to zone.zoneName,
                "zonePoints" to zone.zonePoints.map { point ->
                    mapOf(
                        "latitude" to point.latitude,
                        "longitude" to point.longitude
                    )
                }
            )
            zoneRef.setValue(zoneData)
                .addOnSuccessListener {
                    onSuccess.invoke()
                }
                .addOnFailureListener { e ->
                    onFailure.invoke(e)
                }
        }.addOnFailureListener { e ->
            onFailure.invoke(e)
        }
    }

    //Deliveryman operations
    fun acceptOrder(orderId: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = ordersRef.child("Order_$orderId")
        val currentUserId = auth.currentUser?.uid ?: return

        ordersRef.orderByChild("deliverymanId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val numAcceptedOrders = snapshot.childrenCount.toInt()
                    val updateMap = mapOf(
                        "deliverymanId" to currentUserId,
                        "pickupStatus" to "Picked up",
                        "orderStatus" to "Accepted",
                        "index" to numAcceptedOrders
                    )

                    orderRef.updateChildren(updateMap)
                        .addOnSuccessListener {
                            onSuccess.invoke()
                        }
                        .addOnFailureListener { e ->
                            onFailure.invoke(e)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure.invoke(error.toException())
                }
            })
    }

    fun cancelOrder(orderId: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = ordersRef.child("Order_$orderId")

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Order::class.java)
                if (order != null && order.pickupStatus == "Picked up") {
                    val updateMap = mapOf(
                        "deliverymanId" to "",
                        "pickupStatus" to "Waiting for pickup",
                        "orderStatus" to "In progress",
                        "index" to null
                    )

                    orderRef.updateChildren(updateMap)
                        .addOnSuccessListener {
                            onSuccess.invoke()
                        }
                        .addOnFailureListener { e ->
                            onFailure.invoke(e)
                        }
                } else {
                    Log.e("Order cannot be canceled", "is not in 'Picked up' status / null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure.invoke(error.toException())
            }
        })
    }

    fun deliverOrder(
        orderId: Int,
        orderStatus: String,
        additionalInfo: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val orderRef = ordersRef.child("Order_$orderId")

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Order::class.java)
                if (order != null && order.deliverymanId == currentUserId) {
                    val rightNow = Calendar.getInstance()
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val orderDeliveredDate = dateFormatter.format(rightNow.time)
                    val updateMap = mapOf(
                        "orderStatus" to orderStatus,
                        "additionalInfo" to additionalInfo,
                        "orderDeliveredDate" to orderDeliveredDate,
                        "payStatus" to "Paid"
                    )
                    orderRef.updateChildren(updateMap)
                        .addOnSuccessListener {
                            onSuccess.invoke()
                        }
                        .addOnFailureListener { e ->
                            onFailure.invoke(e)
                        }
                } else {
                    Log.e("Invalid","Invalid order or current user is not the deliveryman assigned to the order.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onFailure.invoke(error.toException())
            }
        })
    }

    // Authentication PART -- AuthActivity & FirebaseAuth
    fun signUp(email: String, password: String, onComplete: (FirebaseUser?, Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(auth.currentUser, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
    }

    fun login(email: String, password: String, onComplete: (FirebaseUser?, Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(auth.currentUser, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun createUserDB(
        email: String, userCompleteName: String, phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = usersRef.child("User_$userId")

            val updateMap = mapOf(
                "userId" to userId,
                "userMail" to email,
                "userType" to "Delivery man",
                "userCompleteName" to userCompleteName,
                "userPhonenumber" to phoneNumber
            )

            userRef.updateChildren(updateMap)
                .addOnSuccessListener {
                    onSuccess.invoke()
                }
                .addOnFailureListener { e ->
                    onFailure.invoke(e)
                }
        } else {
            onFailure.invoke(NullPointerException("Current user ID is null"))
        }
    }
}
