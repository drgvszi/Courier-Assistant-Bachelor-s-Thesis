package com.example.locapp.adminui.dashboard.order_creator

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.locapp.R
import com.example.locapp.databinding.FragmentOrderCreatorBinding
import com.example.locapp.ui.home.HomeViewModel
import com.example.locapp.utils.datas.Order
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class OrderCreatorFragment : Fragment() {

    private var progressDialog: ProgressDialog? = null
    private var _binding: FragmentOrderCreatorBinding? = null
    private val binding get() = _binding!!
    private val orderCreatorScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var ordersHandlerView: OrdersCreatorViewModel
    private lateinit var homeViewModel: HomeViewModel
    private val orderDetails = listOf(
        "Client name",
        "Client phone number",
        "Client email",
        "Delivery description",
        "Delivery address",
        "Days for delivery",
        "Order price"
    )

    private val orderTags = listOf(
        "orderClientName",
        "orderClientPhoneNumber",
        "orderClientEmail",
        "orderDescription",
        "takeToTheAddress",
        "numberDaysForDelivery",
        "orderPrice"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ordersHandlerView = ViewModelProvider(this)[OrdersCreatorViewModel::class.java]
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        _binding = FragmentOrderCreatorBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val editTextContainer = root.findViewById<LinearLayout>(R.id.editTextContainer)

        ordersHandlerView.text.observe(viewLifecycleOwner){
            binding.introTextview.text = it
        }

        for (i in orderDetails.indices) {
            val editText = EditText(requireContext())
            editText.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            editText.hint = orderDetails[i]
            editText.tag = orderTags[i]
            editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            editText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            val linearLayout = LinearLayout(requireContext())
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.addView(editText)
            editTextContainer.addView(linearLayout)
        }

        // Pay Status section
        val payStatusLayout = LinearLayout(requireContext())
        payStatusLayout.orientation = LinearLayout.HORIZONTAL
        val payStatusTextView = TextView(requireContext())
        payStatusTextView.text = "Pay status: "
        payStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        val payStatusGroup = RadioGroup(requireContext())
        payStatusGroup.orientation = RadioGroup.HORIZONTAL
        payStatusGroup.id = View.generateViewId()

        val payStatusOptions = listOf("Paid", "Unpaid")
        for (status in payStatusOptions) {
            val radioButton = RadioButton(requireContext())
            radioButton.text = status
            radioButton.tag = "orderPayStatus_$status"
            payStatusGroup.addView(radioButton)
        }

        payStatusLayout.addView(payStatusTextView)
        payStatusLayout.addView(payStatusGroup)

        // Pay Type section
        val payTypeLayout = LinearLayout(requireContext())
        payTypeLayout.orientation = LinearLayout.HORIZONTAL
        val payTypeTextView = TextView(requireContext())
        payTypeTextView.text = "Pay type: "
        payTypeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        val payTypeGroup = RadioGroup(requireContext())
        payTypeGroup.orientation = RadioGroup.HORIZONTAL
        payTypeGroup.id = View.generateViewId()

        val payTypeOptions = listOf("Card", "Cash")
        for (type in payTypeOptions) {
            val radioButton = RadioButton(requireContext())
            radioButton.text = type
            radioButton.tag = "orderPayType_$type"
            payTypeGroup.addView(radioButton)
        }

        binding.addOrdersFromCsv.setOnClickListener {
            Log.d("OrderCreatorFragment", "addOrdersFromCsv button clicked")
            openFileChooser()
        }

        payTypeLayout.addView(payTypeTextView)
        payTypeLayout.addView(payTypeGroup)

        editTextContainer.addView(payStatusLayout)
        editTextContainer.addView(payTypeLayout)

        binding.completeCreateOrder.setOnClickListener {
            showProgressDialog("Creating order...")
            val orderClientName = editTextContainer.findViewWithTag<EditText>("orderClientName")?.text.toString()
            val orderPhoneNumber = editTextContainer.findViewWithTag<EditText>("orderClientPhoneNumber")?.text.toString()
            val orderClientEmail = editTextContainer.findViewWithTag<EditText>("orderClientEmail")?.text.toString()
            val description = editTextContainer.findViewWithTag<EditText>("orderDescription")?.text.toString()
            val orderPrice = editTextContainer.findViewWithTag<EditText>("orderPrice")?.text.toString().toIntOrNull() ?: 0
            val nrDaysForDelivery = editTextContainer.findViewWithTag<EditText>("numberDaysForDelivery")?.text.toString().toIntOrNull() ?: 0
            val takeToTheAddress = editTextContainer.findViewWithTag<EditText>("takeToTheAddress")?.text.toString()

            val selectedPayStatusId = payStatusGroup.checkedRadioButtonId
            val payStatus = payStatusGroup.findViewById<RadioButton>(selectedPayStatusId)?.text.toString()

            val selectedPayTypeId = payTypeGroup.checkedRadioButtonId
            val payType = payTypeGroup.findViewById<RadioButton>(selectedPayTypeId)?.text.toString()
            val forceChecked = false
            homeViewModel.zones.observe(viewLifecycleOwner) { zones ->
                if (zones != null && zones.isNotEmpty()) {
                    orderCreatorScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val coordinates = ordersHandlerView.fetchedCoordinates(takeToTheAddress)
                                val zoneDelivery = ordersHandlerView.checkTheZoneOfAddress(zones, coordinates)
                                ordersHandlerView.createOrder(
                                    orderClientName,
                                    orderPhoneNumber,
                                    orderClientEmail,
                                    description,
                                    payType,
                                    payStatus,
                                    orderPrice,
                                    coordinates,
                                    nrDaysForDelivery,
                                    takeToTheAddress,
                                    zoneDelivery,
                                    forceChecked
                                )
                            }
                            withContext(Dispatchers.Main) {
                                hideProgressDialog()
                                clearInputFields(editTextContainer)
                                showAlertDialog("Success", "Order created successfully!")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                hideProgressDialog()
                                Log.e("OrderCreatorFragment", "Error creating order: $e")
                                showAlertDialog("Error", "Failed to create order. Please try again.")
                            }
                        }
                    }
                } else {
                    Log.e("OrderCreatorFragment", "Zones not loaded yet")
                }
            }
        }

        return root
    }

    private fun clearInputFields(container: LinearLayout) {
        for (i in orderTags.indices) {
            val editText = container.findViewWithTag<EditText>(orderTags[i])
            editText?.setText("")
        }
    }

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            handleCsvFile(it)
        }?: run {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileChooser() {
        openFileLauncher.launch(arrayOf("*/*"))
    }

    private fun handleCsvFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            inputStream?.close()
            val ordersList = mutableListOf<Order>()

            // Skip header row
            for (i in 1 until lines.size) {
                val columns = lines[i].split(";")
                if (columns.size >= 10) {
                    val order = Order(
                        orderClientName = columns[0].trim(),
                        orderPhoneNumber = columns[1].trim(),
                        orderClientEmail = columns[2].trim(),
                        description = columns[3].trim(),
                        takeToTheAddress = columns[4].trim(),
                        daysForDelivery = columns[5].trim().toInt(),
                        orderPrice = columns[6].trim().toInt(),
                        payStatus = columns[7].trim(),
                        payType = columns[8].trim(),
                        forceChecked = columns[9].trim().toBoolean()
                    )
                    ordersList.add(order)
                } else {
                    Log.e("OrderCreatorFragment", "Invalid CSV row: ${lines[i]}")
                }
            }
            // Process ordersList
            Log.d("CSVOrdersList", "$ordersList")
            Toast.makeText(requireContext(), "Data was uploaded successfully!", Toast.LENGTH_SHORT).show()
            processOrders(ordersList)
        } catch (e: Exception) {
            Log.e("OrderCreatorFragment", "Error reading CSV file: $e")
            Toast.makeText(requireContext(), "Error reading CSV file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processOrders(ordersList: List<Order>) {
        homeViewModel.zones.observe(viewLifecycleOwner) { zones ->
            if (zones != null && zones.isNotEmpty()) {
                orderCreatorScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            for (order in ordersList) {
                                val coordinates = ordersHandlerView.fetchedCoordinates(order.takeToTheAddress)
                                val zoneDelivery = ordersHandlerView.checkTheZoneOfAddress(zones, coordinates)
                                ordersHandlerView.createOrder(
                                    order.orderClientName,
                                    order.orderPhoneNumber,
                                    order.orderClientEmail,
                                    order.description,
                                    order.payType,
                                    order.payStatus,
                                    order.orderPrice,
                                    coordinates,
                                    order.daysForDelivery,
                                    order.takeToTheAddress,
                                    zoneDelivery,
                                    order.forceChecked
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            showAlertDialog("Success", "Order created successfully!")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            hideProgressDialog()
                            Log.e("OrderCreatorFragment", "Error creating order: $e")
                            showAlertDialog("Error", "Failed to create order. Please try again.")
                        }
                    }
                }
            } else {
                Log.e("OrderCreatorFragment", "Zones not loaded yet")
            }
        }
    }

    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
