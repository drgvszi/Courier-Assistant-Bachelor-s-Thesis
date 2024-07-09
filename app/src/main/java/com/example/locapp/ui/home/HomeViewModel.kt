package com.example.locapp.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.BuildConfig
import com.example.locapp.R
import com.example.locapp.utils.DBLoadData
import com.example.locapp.utils.FirebaseManager
import com.example.locapp.utils.Timer
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.Points
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.Location
import com.example.locapp.utils.googleapis.LocationWrapper
import com.example.locapp.utils.googleapis.MyLatLng
import com.example.locapp.utils.googleapis.RetrofitClient
import com.example.locapp.utils.googleapis.RouteLeg
import com.example.locapp.utils.googleapis.RoutePolyline
import com.example.locapp.utils.googleapis.RoutesRequestBodyType
import com.example.locapp.utils.googleapis.RoutesRequestBodyWithWaypoints
import com.example.locapp.utils.googleapis.RoutesResponse
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class HomeViewModel : ViewModel() {

    val firebaseManager = FirebaseManager()
    private val timer = Timer()
    private var activeSegmentIndex: Int = -1 // indicates thats no active segment initially
    private var currentOrderIndex = -1
    private val dbLoadDataManager = DBLoadData()
    private val displayedPolygons = mutableListOf<Polygon>()
    private val displayedMarkers = mutableListOf<Marker>()
    private val orderIds = mutableListOf<Int>()
    private val markerPoints = mutableListOf<MarkerOptions>()
    private val routeLegs = mutableListOf<RouteLeg>()

    private val _nextOrderId = MutableLiveData<Int>()
    private val _zones = MutableLiveData<List<Zones>>()
    private val _zonesName = MutableLiveData<List<String>>()


    val nextOrderId: LiveData<Int> get() = _nextOrderId
    val zones: LiveData<List<Zones>> = _zones
    val zonesName: LiveData<List<String>> = _zonesName


    /*private val _routeResp = MutableLiveData<RoutesResponse>()
    private val _orderIdsList = MutableLiveData<List<Int>>()
    private val _intermediateWaypointsMap= MutableLiveData<HashMap<Int, LocationWrapper>>()
    val routes: LiveData<RoutesResponse> = _routeResp
    val orderIdsList: LiveData<List<Int>> = _orderIdsList
    val intermediateWaypointsMap: LiveData<HashMap<Int, LocationWrapper>> = _intermediateWaypointsMap*/


    fun startTimer() {
        timer.start()
    }

    fun pauseTimer() {
        timer.pause()
    }

    fun stopTimer(totalDistance: Float) {

        firebaseManager.updateTimeWorked(timer.getTotalTimeElapsed(), timer.getStartTime(), totalDistance, {}, {})
        timer.stop()
    }

    fun getTimeLiveData(): LiveData<String> {
        return timer.getTimeLiveData()
    }

    init {
        dbLoadDataManager.loadZones(_zones)
        Log.d("Polygon Points zones","Polygon $_zones, ")
        /*dbLoadDataManager.loadRoutes(_routeResp, _orderIdsList, _intermediateWaypointsMap)
        Log.d("_routeResp, _orderIdsList","RouteResp $_routeResp, $_orderIdsList, ")
        Log.d("_routeResp, _orderIdsList","Ord List, $_orderIdsList, ")
        Log.d("_intermediateWaypointsMap","waypoints $_intermediateWaypointsMap")*/
        _zones.observeForever { zonesList ->
            getAvailableZones(zonesList)
        }
    }

    private val _routesResponse = MutableLiveData<RoutesResponse?>()
    val routesResponse: LiveData<RoutesResponse?> get() = _routesResponse

    private fun setRoutesResponse(response: RoutesResponse?) {
        _routesResponse.value = response
    }


    fun cancelOrder(orderId: Int) {
        firebaseManager.cancelOrder(
            orderId,
            onSuccess = {
                Log.d("AccOrdViewModel", "Order canceled successfully.")
            },
            onFailure = { e ->
                Log.e("AccOrdViewModel", "Failed to cancel the order: $e")
            }
        )
    }

    fun finishOrder(orderId: Int, orderStatus: String, addInfo: String, onSuccess: () -> Unit){
        firebaseManager.deliverOrder(
            orderId, orderStatus, addInfo,
            onSuccess = {
                onSuccess()
                Log.d("AccOrdViewModel", "Order delivered successfully.")
            },
            onFailure = { e ->
                Log.e("AccOrdViewModel", "Failed to deliver the order: $e")
            }
        )
    }

    fun updateOrderIndex(orderId: Int, newIndex: Int) {
        firebaseManager.updateOrderIndex(orderId, newIndex)
    }

    private fun getAvailableZones(zonesList: List<Zones>) {
        val zoneNames = zonesList.map { it.zoneName}
        _zonesName.postValue(zoneNames)
    }

    fun displayZones(zones: List<Zones>, map: GoogleMap) {
        hideZones()
        for (zone in zones) {
            val zoneName = zone.zoneName
            Log.d("ZoneName", "$zoneName, ")
            val red = (0..255).random()
            val green = (0..255).random()
            val blue = (0..255).random()
            val polygonOptions = PolygonOptions()
                .addAll(zone.zonePoints.map { LatLng(it.latitude, it.longitude) })
                .fillColor(Color.argb(95, red, green, blue))
                .strokeColor(Color.TRANSPARENT)
            val polygon = map.addPolygon(polygonOptions)
            displayedPolygons.add(polygon)

            val latLngPoints = zone.zonePoints.map { LatLng(it.latitude, it.longitude) }
            val center = calculateZoneCenter(latLngPoints)

            val canvasWidth = 20
            val canvasHeight = 20
            val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
            val radius = (canvasWidth / 2).toFloat()
            canvas.drawCircle(radius, radius, radius, paint)

            val markerOptions = MarkerOptions()
                .position(center)
                .title(zoneName)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            val marker = map.addMarker(markerOptions)
            marker?.let { displayedMarkers.add(it) }
        }
    }

    fun hideZones() {
        for (polygon in displayedPolygons) {
            polygon.remove()
        }
        displayedPolygons.clear()

        for (marker in displayedMarkers) {
            marker.remove()
        }
        displayedMarkers.clear()
    }

    private fun calculateZoneCenter(zonePoints: List<LatLng>): LatLng {
        var totalLatitude = 0.0
        var totalLongitude = 0.0
        for (point in zonePoints) {
            totalLatitude += point.latitude
            totalLongitude += point.longitude
        }
        val centerLatitude = totalLatitude / zonePoints.size
        val centerLongitude = totalLongitude / zonePoints.size
        return LatLng(centerLatitude, centerLongitude)
    }

    fun saveZoneToDB(zone: Zones) {
        firebaseManager.saveZone(zone, {
            Log.d("HomeViewModel", "Zone saved successfully.")
        }, { e ->
            Log.e("HomeViewModel", "Failed to save zone: $e")
        })
    }

    fun getRoutesResponse(apiKey:String, requestBody: RoutesRequestBodyType, header: String, viewModelScope: CoroutineScope){
        _routesResponse.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try{
                val response = when (requestBody) {
                    is RoutesRequestBodyType.WithWaypoints -> RetrofitClient.routesServices.getRoutes(apiKey, header, requestBody.body).execute()
                    is RoutesRequestBodyType.WithoutWaypoints -> RetrofitClient.routesServices.getRoutes(apiKey, header, requestBody.body).execute()
                }
                val apiResponse = if (response.isSuccessful) response.body() else {
                    Log.d("API RESPONSE ERROR FOR MULTIPLE ROUTES", "API error: ${response.errorBody()?.string()}")
                    null
                }
                withContext(Dispatchers.Main) {
                    setRoutesResponse(apiResponse)
                }
            }
            catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    setRoutesResponse(null)
                    Log.e("getRoutesResponse Error", "$e")
                }
            }
        }
    }

    fun displayRouteLegs(routeResp: RoutesResponse?, gMap: GoogleMap) {
        if (routeResp != null) {
            val route = routeResp.routes.firstOrNull()

            if (route != null) {
                if (route.legs != null && route.legs.isNotEmpty()) {

                    routeLegs.clear()
                    activeSegmentIndex = -1
                    currentOrderIndex = 0
                    _nextOrderId.value = orderIds.getOrNull(currentOrderIndex)
                    for(marker in markerPoints) {
                        gMap.addMarker(marker)
                    }
                    var isFirstLeg = true
                    for (leg in route.legs) {
                        val instruction = "Travel ${leg.distanceMeters} meters to ${leg.endLocation} for (${leg.staticDuration}) then:"
                        Log.d("instructions", "!!! $instruction")
                        val stepPolyline = PolyUtil.decode(leg.polyline.encodedPolyline)
                        val color = if (isFirstLeg) Color.GREEN else Color.parseColor("#70940829")
                        val polyline = gMap.addPolyline(
                            PolylineOptions()
                                .addAll(stepPolyline)
                                .width(7f)
                                .color(color)
                        )
                        leg.polylineReference = polyline
                        leg.isActive = isFirstLeg
                        routeLegs.add(leg)
                        if (isFirstLeg) {
                            activeSegmentIndex = 0
                        }
                        isFirstLeg = false
                    }
                } else {
                    val stepPolyline = PolyUtil.decode(route.polyline.encodedPolyline)
                    currentOrderIndex = 0
                    _nextOrderId.value = orderIds.getOrNull(currentOrderIndex)
                    for(marker in markerPoints) {
                        gMap.addMarker(marker)
                    }
                    val polyline = gMap.addPolyline(
                        PolylineOptions()
                            .addAll(stepPolyline)
                            .width(7f)
                            .color(Color.BLUE)
                    )
                }
            } else {
                Log.d("API ERROR", "Routes API response does not contain any routes")
            }
        } else {
            Log.d("API ERROR", "Routes API response is null")
        }
    }
    private val _distance = MutableLiveData<Double>()
    val distance: LiveData<Double> = _distance

    private val _duration = MutableLiveData<String>()
    val duration: LiveData<String> = _duration

    private val _orderList = MutableLiveData<String>()
    val orderList: LiveData<String> = _orderList

    fun setRouteInfo(distance: Double, duration: String, orderList: String) {
        _distance.value = distance
        _duration.value = duration
        _orderList.value = orderList
    }

    fun routesApiResponse(routeResp: RoutesResponse?, gMap: GoogleMap, orderId: Int, endLatLng: LatLng){
        if (routeResp != null) {
            val encodedPolyline = routeResp.routes[0].polyline.encodedPolyline
            val polylineOptions = PolylineOptions()
                .addAll(PolyUtil.decode(encodedPolyline))
                .width(7f)
                .color(Color.BLUE)
            gMap.addPolyline(polylineOptions)
            this.orderIds.clear()
            this.orderIds.add(orderId)
            _nextOrderId.value = orderIds.getOrNull(currentOrderIndex)
            this.markerPoints.clear()
            val markerOptions = MarkerOptions().position(endLatLng).title("Package #${orderId}")
            gMap.addMarker(markerOptions)
            this.markerPoints.add(markerOptions)
        } else {
            Log.d("API ERROR", "Routes API response is null")
        }
    }

    fun routesApiResponseWaypoints(routeResp: RoutesResponse?, gMap: GoogleMap, orderIdsList: List<Int>, intermediateMap: HashMap<Int, LocationWrapper>) {
        if (routeResp != null) {
            Log.d("RoutesLEgs", "${routeResp.routes.firstOrNull()}")
            val route = routeResp.routes.firstOrNull()
            if (route?.legs != null) {
                routeLegs.clear()
                activeSegmentIndex = -1
                var isFirstLeg = true
                this.orderIds.clear()
                this.orderIds.addAll(orderIdsList)
                currentOrderIndex = 0

                _nextOrderId.value = orderIdsList.getOrNull(currentOrderIndex)

                for (leg in route.legs) {
                    val instruction = "Travel ${leg.distanceMeters} meters to ${leg.endLocation} for (${leg.staticDuration}) then:"
                    Log.d("instructions", "!!! $instruction")
                    val stepPolyline = PolyUtil.decode(leg.polyline.encodedPolyline)
                    val color = if (isFirstLeg) Color.GREEN else Color.parseColor("#70940829")
                    val polyline = gMap.addPolyline(
                        PolylineOptions()
                            .addAll(stepPolyline)
                            .width(7f)
                            .color(color)
                    )
                    leg.polylineReference = polyline
                    leg.isActive = isFirstLeg
                    routeLegs.add(leg)
                    if (isFirstLeg) {
                        activeSegmentIndex = 0
                    }
                    isFirstLeg = false
                    this.markerPoints.clear()
                    for (orderId in orderIdsList) {
                        intermediateMap[orderId]?.let { locationWrapper ->
                            val endLatLng = LatLng(locationWrapper.location.latLng.latitude, locationWrapper.location.latLng.longitude)
                            val markerOptions = MarkerOptions().position(endLatLng).title("Package #$orderId")
                            gMap.addMarker(markerOptions)
                            markerOptions.let { markerPoints.add(it) }
                        }
                    }
                }

            } else {
                Log.d("API ERROR", "Routes API response does not contain legs or route is null")
            }
        } else {
            Log.d("API ERROR", "Routes API response is null")
        }
    }

    /*fun onUserReachedDestination(order: Order) {
        val orderIndex = orderIds.indexOf(order.orderId)
        if (orderIndex != -1 && order.orderStatus != "Accepted") {
            if (orderIndex == currentOrderIndex) {
                if (activeSegmentIndex < routeLegs.size - 1) {
                    routeLegs[activeSegmentIndex].isCompleted = true
                    routeLegs[activeSegmentIndex].isActive = false
                    activeSegmentIndex++
                    routeLegs[activeSegmentIndex].isActive = true
                    updatePolylineColors()
                    currentOrderIndex++
                    _nextOrderId.value = orderIds.getOrNull(currentOrderIndex)
                }
            } else {
                Log.d("Order Index Mismatch", "Current order index: $currentOrderIndex, Order index in list: $orderIndex")
            }
        } else {
            Log.d("Order Status", "Order status is not Accepted or order is not in the accepted list")
        }
    }*/

    private fun updatePolylineColors() {
        routeLegs.forEachIndexed { index, leg ->
            leg.polylineReference?.color = when {
                index == activeSegmentIndex -> Color.GREEN
                index < activeSegmentIndex -> Color.parseColor("#00FFFFFF")
                else -> Color.parseColor("#5500ccff")
            }
        }
    }

    fun sortNearestNeighb(
        startLat: Double, startLng: Double,
        intermediates: MutableList<LocationWrapper>,
        intermediateMap: HashMap<Int, LocationWrapper>,
        orderIds: MutableList<Int>
    ) {
        intermediates.sortBy { loc ->
            val lat = loc.location.latLng.latitude
            val lng = loc.location.latLng.longitude
            sqrt((startLat - lat).pow(2) + (startLng - lng).pow(2))
        }

        for (i in 1 until intermediates.size) {
            val prevLocation = intermediates[i - 1].location.latLng
            intermediates.subList(i, intermediates.size).sortBy { loc ->
                val dist = distanceHaversine(prevLocation.latitude, prevLocation.longitude, loc.location.latLng.latitude, loc.location.latLng.longitude)
                dist
            }
        }

        val reorderedOrderIds = mutableListOf<Int>()
        for (intermediate in intermediates) {
            val originalOrder = intermediateMap.filterValues { it == intermediate }.keys.firstOrNull()
            originalOrder?.let { reorderedOrderIds.add(it) }
        }
        orderIds.clear()
        orderIds.addAll(reorderedOrderIds)
    }

    // https://www.movable-type.co.uk/scripts/latlong.html haversine formula
    private fun distanceHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rad = 6378 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return rad * c
    }

}
