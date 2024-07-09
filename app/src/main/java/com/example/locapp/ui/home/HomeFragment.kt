package com.example.locapp.ui.home

import ItemMoveCallback
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.BuildConfig
import com.example.locapp.R
import com.example.locapp.databinding.FragmentHomeBinding
import com.example.locapp.ui.dashboard.accepted_orders.HomeAdapter
import com.example.locapp.utils.SharedViewModel
import com.example.locapp.utils.datas.Coordinates
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.Points
import com.example.locapp.utils.datas.Zones
import com.example.locapp.utils.googleapis.*
import com.google.android.gms.common.api.Response
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import java.lang.StringBuilder

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var copyOrders: List<Order>? = null
    private var startLat: Double = 0.0
    private var startLng: Double = 0.0
    private var _binding: FragmentHomeBinding? = null
    private var isMapReady = false
    private var isEditing = false
    private var isHiding = false
    private var countingDistance = false
    private var isVisibleWorkLayout = false
    private var bestRouteFlag: Boolean = false
    private var isOverlayActive= false
    private val markersList = mutableListOf<LatLng>()
    private var userMarker: Marker? = null
    private var userPosition : LatLng = LatLng(0.0, 0.0)
    private val binding get() = _binding!!
    private var totalDistanceTraveled: Float = 0f
    private var lastLocation: Location? = null

    private val viewModelScope: CoroutineScope by lazy { CoroutineScope(viewModelJob + Dispatchers.Main) }
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var viewModelJob: Job
    private lateinit var mapView: MapView
    private lateinit var zonesMap: List<Zones>
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var homeAdapter: HomeAdapter
    private lateinit var timeTextView: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var showOverlay: Button
    private lateinit var spinner: Spinner
    private var selectedThreshold = 2
    private var observer: Observer<RoutesResponse?>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        viewModelJob = Job()

        mapView = root.findViewById(R.id.mapFragment)
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)  {
            requestLocationPermission()
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            setupLocationCallback()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ordersRecyclerView = binding.ordersRecyclerView
        ordersRecyclerView.layoutManager = LinearLayoutManager(context)

        spinner = binding.numberOfOrdersSpinner

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, selectedView: View?, position: Int, id: Long) {
                selectedView?.let {
                    selectedThreshold = parent.getItemAtPosition(position).toString().toInt()
                    homeAdapter.setThreshold(selectedThreshold)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {// Do nothing
            }

        }

        homeAdapter = HomeAdapter(
            homeViewModel,
            onShowOnMapClickListener = { order ->
                observer?.let { homeViewModel.routesResponse.removeObserver(it) }
                viewModelScope.launch {
                    handleSingleRoute(order)
                }
                googleMap.clear()
            },
            onCancelClickListener = { order ->
                homeViewModel.cancelOrder(order.orderId)
            },
            onDeliverClickListener = { order ->
                showConfirmationDialog(order)
            },
            onCallButton = { order ->
                callTheClient(order)
            }
        )

        ordersRecyclerView.adapter = homeAdapter
        ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val itemTouchHelper = ItemTouchHelper(ItemMoveCallback(homeAdapter, homeViewModel))
        itemTouchHelper.attachToRecyclerView(ordersRecyclerView)

        sharedViewModel.acceptedOrders.observe(viewLifecycleOwner) { orders ->
            if (orders.isEmpty()) {
                return@observe
            }
            copyOrders = orders
            val sortedOrders = orders.sortedBy {it.index}
            homeAdapter.submitList(sortedOrders)
            if (isMapReady) {
                homeAdapter.setOrders(sortedOrders, googleMap)
            }
        }

        homeViewModel.nextOrderId.observe(viewLifecycleOwner) { orderId ->
            binding.routeNextStopTextView.text = "Next stop: ${orderId ?: "No more orders"}"
            homeAdapter.setNextOrder(orderId?: -1)
        }

        binding.showAllRoutesButton.setOnClickListener {
            googleMap.clear()
            bestRouteFlag = false
            observer?.let { obs -> homeViewModel.routesResponse.removeObserver(obs) }
            sharedViewModel.acceptedOrders.observe(viewLifecycleOwner) { orders ->
                if (orders.isEmpty()) {
                    Toast.makeText(requireContext(), "No accepted orders found", Toast.LENGTH_SHORT).show()
                    return@observe
                } else {

                    val sortedOrders = orders.sortedBy{ it.index }.take(selectedThreshold)
                    handleMultipleRoutes(sortedOrders, bestRouteFlag)
                }
            }

        }

        binding.showTheBestRouteButton.setOnClickListener {
            googleMap.clear()
            bestRouteFlag = true
            observer?.let { obs -> homeViewModel.routesResponse.removeObserver(obs) }
            sharedViewModel.acceptedOrders.observe(viewLifecycleOwner) { orders ->
                if (orders.isEmpty()) {
                    Toast.makeText(requireContext(), "No accepted orders found", Toast.LENGTH_SHORT).show()
                    return@observe
                } else {
                    val sortedOrders = orders.sortedBy{ it.index }.take(selectedThreshold)
                    handleMultipleRoutes(sortedOrders, bestRouteFlag)
                }
            }
        }

        /*val updateButton: Button = requireView().findViewById(R.id.update_button)
        updateButton.setOnClickListener {
        }*/

        binding.reloadButton.setOnClickListener {
            reloadMap()
        }

        homeViewModel.firebaseManager.checkIfUserIsAdmin { isAdmin ->
            binding.editZonesButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
            if (isAdmin) {
                binding.editZonesButton.setOnClickListener {
                    isEditing = !isEditing
                    binding.editZonesButton.text = if (isEditing) "Save" else "Edit Zones"
                    if (!isEditing) {
                        saveZones()
                        completeZone()
                    } else {
                        markersList.clear()
                    }
                }
            }
        }
        binding.showZonesButton.text = "Show zones"
        homeViewModel.zones.observe(viewLifecycleOwner) { zones ->
            zonesMap = zones
        }
        binding.showZonesButton.setOnClickListener {
            isHiding = !isHiding
            if (isHiding) {
                binding.showZonesButton.text = "Hide zones"
                homeViewModel.displayZones(zonesMap, googleMap)
            } else {
                binding.showZonesButton.text = "Show zones"
                homeViewModel.hideZones()
            }
        }

        binding.showWorkLayoutButton.setOnClickListener {
            isVisibleWorkLayout = !isVisibleWorkLayout
            if (isVisibleWorkLayout) {
                binding.workLayout.visibility = View.VISIBLE
            } else {
                binding.workLayout.visibility = View.GONE
            }
        }

        timeTextView = binding.timeTextView
        startButton = binding.startWork
        pauseButton = binding.startPauseWork
        stopButton = binding.stopWork

        homeViewModel.getTimeLiveData().observe(viewLifecycleOwner) { time ->
            timeTextView.text = time
        }

        startButton.setOnClickListener {
            totalDistanceTraveled = 0f
            lastLocation = null
            countingDistance = true
            homeViewModel.startTimer()

        }

        pauseButton.setOnClickListener {
            countingDistance = false
            homeViewModel.pauseTimer()
        }

        stopButton.setOnClickListener {
            countingDistance = false
            homeViewModel.stopTimer(totalDistanceTraveled)
        }

        showOverlay = binding.overlayButton
        showOverlay.text = "Show orders"
        showOverlay.setOnClickListener {
            isOverlayActive = !isOverlayActive
            if (isOverlayActive) {
                showOverlay.text = "Hide orders"
                binding.ordersLayout.visibility = View.VISIBLE
            } else {
                showOverlay.text = "Show orders"
                binding.ordersLayout.visibility = View.GONE
            }
        }
    }

    private fun callTheClient(selectedOrder: Order) {
        val phoneNumber = selectedOrder.orderPhoneNumber
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        context?.startActivity(intent)
    }

    private fun showConfirmationDialog(selectedOrder: Order) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_dialog, null)
        builder.setView(dialogView)

        val alertDialog = builder.create()
        alertDialog.show()

        dialogView.findViewById<TextView>(R.id.order_name).text = selectedOrder.orderId.toString()
        dialogView.findViewById<TextView>(R.id.pay_status).text = selectedOrder.payStatus

        val isPaid = selectedOrder.payStatus == "Paid"
        val selectedOrderPrice = dialogView.findViewById<EditText>(R.id.order_price)

        val orderPinField = dialogView.findViewById<EditText>(R.id.order_pin)
        val statusGroup = dialogView.findViewById<RadioGroup>(R.id.select_status)

        statusGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedStatus = dialogView.findViewById<RadioButton>(checkedId).text.toString()
            orderPinField.visibility = if (selectedStatus == "Delivered") View.VISIBLE else View.GONE
            if (selectedStatus == "Delivered"){
                if (isPaid)
                    selectedOrderPrice.visibility = View.GONE
                else
                    selectedOrderPrice.visibility =  View.VISIBLE
            }
            else
                selectedOrderPrice.visibility = View.GONE


        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.complete_order).setOnClickListener {
            val selectedStatus = statusGroup.run {
                findViewById<RadioButton>(checkedRadioButtonId)?.text?.toString().orEmpty()
            }
            val selectedOrderPin = orderPinField.text.toString()
            val selectedOrderPinInt = selectedOrderPin.toIntOrNull() ?: 0
            val additionalInfo = dialogView.findViewById<EditText>(R.id.additional_information).text.toString()

            if (selectedStatus == "Delivered") {
                if (selectedOrder.orderPin == selectedOrderPinInt) {
                    if(isPaid) {
                        homeViewModel.finishOrder(selectedOrder.orderId, selectedStatus, additionalInfo) {
                            showAlertDialog("Success!", "Order #${selectedOrder.orderId} is delivered!")
                        }
                        alertDialog.dismiss()
                    }
                    else{
                        if(selectedOrder.orderPrice == selectedOrderPrice.text.toString().toIntOrNull())
                        {
                            homeViewModel.finishOrder(selectedOrder.orderId, selectedStatus, additionalInfo) {
                                reloadMap()
                            }
                            alertDialog.dismiss()
                        }
                        else
                        {
                            showAlertDialog("Something went wrong!", "The order is not paid!")
                        }
                    }
                } else {
                    showAlertDialog("Something went wrong!", "The PIN is incorrect!")
                }
            } else {
                homeViewModel.finishOrder(selectedOrder.orderId, selectedStatus, additionalInfo) {
                    reloadMap()
                }
                alertDialog.dismiss()
            }
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }


    private fun reloadMap() {
        googleMap.clear()
        startLocationUpdates()
        onMapReady(googleMap)
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        isMapReady = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }

        googleMap.setOnMapClickListener { latLng ->
            if (isEditing) {
                addMarker(latLng)
            }
        }
        homeViewModel.routesResponse.observe(viewLifecycleOwner) { response ->
            Log.d("observer", "Observe the routes \n $response ")
            homeViewModel.displayRouteLegs(response, googleMap)
            homeViewModel.distance.observe(viewLifecycleOwner) { distance ->
                homeViewModel.duration.observe(viewLifecycleOwner){ duration ->
                    updateRouteInfo(distance/1000, duration)
                }
            }

            homeViewModel.orderList.observe(viewLifecycleOwner){ orderList ->
                binding.routeOrderTextView.text = orderList
            }
        }
    }

    private var lastMarker: LatLng? = null

    private fun addMarker(latLng: LatLng) {
        markersList.add(latLng)
        googleMap.addMarker(AdvancedMarkerOptions().position(latLng).draggable(true))

        if (lastMarker != null) {
            val lineOptions = PolylineOptions()
                .add(lastMarker)
                .add(latLng)
                .width(5f)
                .color(Color.RED)
            googleMap.addPolyline(lineOptions)
        }
        lastMarker = latLng
    }

    private fun completeZone() {
        googleMap.clear()
        lastMarker = null
        markersList.clear()
    }

    private fun saveZones() {
        if (markersList.size >= 3) {
            val points = markersList.map { Points(it.latitude, it.longitude) }
            // Show popup to enter the zone name
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_zone, null)
            val editText = view.findViewById<EditText>(R.id.zone_name_edit_text)

            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Save") { dialog, _ ->
                    val zoneName = editText.text.toString().trim()
                    if (zoneName.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter a zone name", Toast.LENGTH_SHORT).show()
                    } else {
                        val zone = Zones(zoneName = zoneName, zonePoints = points)
                        homeViewModel.saveZoneToDB(zone)
                        Toast.makeText(requireContext(), "Zone created!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
            alertDialog.show()

        } else {
            Toast.makeText(requireContext(), "Not enough points to create a zone!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateLocationOnMap(location)
                    if(countingDistance)
                        calculateDistance(location)
                    Log.d("KM", "$totalDistanceTraveled metri")
                }
            }
        }
    }

    private fun calculateDistance(location: Location) {
        lastLocation?.let {
            val distance = it.distanceTo(location)
            totalDistanceTraveled += distance
        }
        lastLocation = location
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(10000)
                .build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationOnMap(location: Location) {
        userPosition = LatLng(location.latitude, location.longitude)
        if (userMarker == null) {
            val pinConfigBuilder: PinConfig.Builder = PinConfig.builder()
            pinConfigBuilder.setBackgroundColor(Color.BLUE)
            val pinConfig: PinConfig = pinConfigBuilder.build()
            val markerOptions = AdvancedMarkerOptions().position(userPosition).title("User pos")
                .icon(BitmapDescriptorFactory.fromPinConfig(pinConfig))
            userMarker = googleMap.addMarker(markerOptions)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))
        } else {
            userMarker?.position = userPosition
        }
        startLat = location.latitude
        startLng = location.longitude
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission to function properly")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun handleSingleRoute(order: Order) {
        val endLat = order.takeToCoords.latitude
        val endLng = order.takeToCoords.longitude
        val orderId = order.orderId
        if (isMapReady) {
            val startLatLng = LatLng(startLat, startLng)
            val endLatLng = LatLng(endLat, endLng)
            val originLocation = LocationWrapper(Location(MyLatLng(startLat, startLng)))
            val destinationLocation = LocationWrapper(Location(MyLatLng(endLat, endLng)))

            val requestBody = RoutesRequestBody(
                origin = originLocation,
                destination = destinationLocation,
                travelMode = "DRIVE",
                computeAlternativeRoutes = false,
                units = "IMPERIAL"
            )
            val requestBodyType = RoutesRequestBodyType.WithoutWaypoints(requestBody)
            val header = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline"
            homeViewModel.getRoutesResponse(sharedViewModel.getApiKey(), requestBodyType, header, viewModelScope)
            observer = Observer { response: RoutesResponse? ->
                response?.let {
                    googleMap.clear()
                    homeViewModel.routesApiResponse(it, googleMap, orderId, endLatLng )
                    val distance = it.routes.firstOrNull()?.distanceMeters?.toDouble() ?: 0.0
                    val duration = it.routes.firstOrNull()?.duration ?: ""
                    updateRouteInfo(distance/1000, duration)
                    binding.routeOrderTextView.text = orderId.toString()
                    homeViewModel.setRouteInfo(distance, duration, orderId.toString())
                }
            }
            homeViewModel.routesResponse.observe(viewLifecycleOwner, observer!!)
        }
    }

    private fun handleMultipleRoutes(orders: List<Order>?, routeType: Boolean) {
        val intermediates = mutableListOf<LocationWrapper>()
        val orderIdsList = mutableListOf<Int>()
        val intermediateMap = HashMap<Int, LocationWrapper>()
        orders?.forEach { order ->
            val locationWrapper = LocationWrapper(Location(MyLatLng(order.takeToCoords.latitude, order.takeToCoords.longitude)))
            intermediates.add(locationWrapper)
            orderIdsList.add(order.orderId)
            intermediateMap[order.orderId] = locationWrapper
        }
        if (routeType) {
            homeViewModel.sortNearestNeighb(startLat, startLng, intermediates, intermediateMap, orderIdsList)
        }

        val endLat = intermediates.lastOrNull()?.location?.latLng?.latitude ?: 0.0
        val endLng = intermediates.lastOrNull()?.location?.latLng?.longitude ?: 0.0
        val originLocation = LocationWrapper(Location(MyLatLng(startLat, startLng)))
        val destinationLocation = LocationWrapper(Location(MyLatLng(endLat, endLng)))
        val requestBody = RoutesRequestBodyWithWaypoints(
            origin = originLocation,
            destination = destinationLocation,
            intermediates = intermediates,
            travelMode = "DRIVE",
            computeAlternativeRoutes = false,
            units = "IMPERIAL"
        )
        val requestBodyType = RoutesRequestBodyType.WithWaypoints(requestBody)
        val header = "routes.duration,routes.distanceMeters,routes.legs"
        homeViewModel.getRoutesResponse(sharedViewModel.getApiKey(), requestBodyType, header, viewModelScope)
        observer = Observer { response: RoutesResponse? ->
            response?.let {
                googleMap.clear()
                homeViewModel.routesApiResponseWaypoints(it, googleMap, orderIdsList, intermediateMap)
                val distance = it.routes.firstOrNull()?.distanceMeters?.toDouble() ?: 0.0
                val duration = it.routes.firstOrNull()?.duration ?: ""
                updateRouteInfo(distance / 1000, duration)
                val orderList = orderIdsList.joinToString("->")
                binding.routeOrderTextView.text = orderList
                homeViewModel.setRouteInfo(distance, duration, orderList)
            }
        }
        homeViewModel.routesResponse.observe(viewLifecycleOwner, observer!!)
    }

    @SuppressLint("SetTextI18n")
    private fun updateRouteInfo(distance: Double, duration: String) {
        val durationInSeconds = duration.removeSuffix("s").toIntOrNull()
        val hours: Int
        val minutes: Int
        val seconds: Int

        if (durationInSeconds != null) {
            hours = durationInSeconds / 3600
            minutes = (durationInSeconds % 3600) / 60
            seconds = durationInSeconds % 60
        } else {
            hours = 0
            minutes = 0
            seconds = 0
        }

        // Format hours, minutes, and seconds to be always two digits
        val formattedHours = String.format("%02d", hours)
        val formattedMinutes = String.format("%02d", minutes)
        val formattedSeconds = String.format("%02d", seconds)

        binding.routeDistanceTextView.text = "Distance: ${String.format("%.2f", distance)} KM"
        binding.routeDurationTextView.text = "Duration: $formattedHours:$formattedMinutes:$formattedSeconds"
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModelJob.cancel()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        viewModelJob.cancel()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}