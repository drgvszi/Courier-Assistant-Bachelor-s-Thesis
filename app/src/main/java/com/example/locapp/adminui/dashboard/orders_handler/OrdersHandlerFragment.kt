package com.example.locapp.adminui.dashboard.orders_handler

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Color.BLUE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.R
import com.example.locapp.databinding.FragmentOrdersHandlerBinding
import com.example.locapp.ui.home.HomeViewModel
import com.example.locapp.utils.SharedViewModel
import com.example.locapp.utils.datas.Order
import com.example.locapp.utils.datas.Users
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrdersHandlerFragment : Fragment() {

    private var _binding: FragmentOrdersHandlerBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersHandlerViewModel: OrdersHandlerViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var searchButton: Button
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sortDropdown: Spinner
    private lateinit var filterDropdown: Spinner
    private lateinit var recyclerView: RecyclerView
    private var sharedUsers:List<Users> = emptyList()
    private val searchQuery = MutableLiveData<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ordersHandlerViewModel = ViewModelProvider(this)[OrdersHandlerViewModel::class.java]
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        _binding = FragmentOrdersHandlerBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerViewOrdersHandler
        sortDropdown = binding.sortDropdown
        filterDropdown = binding.filterDropdown
        searchButton = binding.searchButton

        val adapter = OrdersHandlerAdapter(
            onDeleteClickListener = { selectedOrder ->
                ordersHandlerViewModel.deleteOrder(selectedOrder)
            },
            onEditClickListener = { selectedOrder ->
                showEditPopup(selectedOrder)
                Log.d("EDIT BUTTON", "Order: $selectedOrder")
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        ordersHandlerViewModel = ViewModelProvider(this)[OrdersHandlerViewModel::class.java]

        sharedViewModel.users.observe(viewLifecycleOwner) { users->
            sharedUsers = users
            adapter.setUsers(sharedUsers)
        }
        sharedViewModel.orders.observe(viewLifecycleOwner) { allOrders ->
            adapter.submitList(allOrders)
            Log.d("Observer OrdersHandlerFragment", "allOrders : $/allOrders")
        }

        sortDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position) as String
                sharedViewModel.orders.value?.let { allOrders ->
                    val sortedOrders = sharedViewModel.sortOrders(allOrders, selectedOption)
                    adapter.submitList(sortedOrders) {
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        filterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position) as String
                sharedViewModel.orders.value?.let { allOrders ->
                    val filteredOrders = sharedViewModel.filterOrders(allOrders, selectedOption)
                    adapter.submitList(filteredOrders) {
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        searchQuery.observe(viewLifecycleOwner) { query ->
            sharedViewModel.orders.value?.let { allOrders ->
                val filterOrdersBySearchedId = allOrders.filter {
                            it.orderId.toString().contains(query) ||
                            it.orderClientName.contains(query, ignoreCase = true) ||
                            it.orderClientEmail.contains(query, ignoreCase = true) ||
                            it.orderPhoneNumber.contains(query) ||
                            it.payStatus.contains(query, ignoreCase = true) ||
                            it.zoneDelivery.contains(query, ignoreCase = true) ||
                            it.payType.contains(query, ignoreCase = true) ||
                            it.takeToTheAddress.contains(query, ignoreCase = true) ||
                            it.orderStatus.contains(query, ignoreCase = true) ||
                            it.takeToTheAddress.contains(query, ignoreCase = true)
                }
                adapter.submitList(filterOrdersBySearchedId) {
                    recyclerView.scrollToPosition(0)
                }
            }
        }

        searchButton.setOnClickListener {
            val editText = requireView().findViewById<EditText>(R.id.search_bar_edit)
            searchQuery.value = editText.text.toString()
        }

        return binding.root
    }

    private fun showEditPopup(selectedOrder: Order) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_admin_edit_order, null)
        builder.setView(dialogView)

        val limitOrderDateEditText = dialogView.findViewById<EditText>(R.id.edit_text_limit_order_date)
        limitOrderDateEditText.setText(selectedOrder.limitOrderDate)

        limitOrderDateEditText.setOnClickListener {
            showDateTimePicker(limitOrderDateEditText)
        }

        populateOrderDetails(dialogView, selectedOrder)

        val alertDialog = builder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.complete_order).setOnClickListener {
            handleCompleteOrder(dialogView, selectedOrder, alertDialog)
        }
    }

    private fun showDateTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->

            val dateSet = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthOfYear)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }

            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                dateSet.set(Calendar.HOUR_OF_DAY, hourOfDay)
                dateSet.set(Calendar.MINUTE, minute)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                editText.setText(dateFormat.format(dateSet.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePickerDialog.show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun populateOrderDetails(dialogView: View, selectedOrder: Order) {
        dialogView.findViewById<TextView>(R.id.order_name).text = selectedOrder.orderId.toString()
        dialogView.findViewById<EditText>(R.id.pay_type).setText(selectedOrder.payType)
        dialogView.findViewById<EditText>(R.id.pay_status).setText(selectedOrder.payStatus)
        dialogView.findViewById<EditText>(R.id.order_price).setText(selectedOrder.orderPrice.toString())
        dialogView.findViewById<EditText>(R.id.order_description).setText(selectedOrder.description)
        dialogView.findViewById<EditText>(R.id.delivery_address).setText(selectedOrder.takeToTheAddress)
        dialogView.findViewById<EditText>(R.id.client_email).setText(selectedOrder.orderClientEmail)
        dialogView.findViewById<EditText>(R.id.order_phone_number).setText(selectedOrder.orderPhoneNumber.toString())
        dialogView.findViewById<EditText>(R.id.order_client_name).setText(selectedOrder.orderClientName)
        dialogView.findViewById<EditText>(R.id.order_status).setText(selectedOrder.orderStatus)
        dialogView.findViewById<EditText>(R.id.additional_information).setText(selectedOrder.additionalInfo)
        dialogView.findViewById<CheckBox>(R.id.force_checkbox).apply {
            isChecked = selectedOrder.forceChecked
            setOnCheckedChangeListener { _, isChecked ->
                selectedOrder.forceChecked = isChecked
            }
        }

        for (user in sharedUsers) {
            if (user.userId == selectedOrder.deliverymanId) {
                dialogView.findViewById<EditText>(R.id.user_mail).apply {
                    setText(user.userMail)
                    setTextColor(Color.BLUE)
                }
                break
            }
        }
    }

    private fun handleCompleteOrder(dialogView: View, selectedOrder: Order, alertDialog: AlertDialog) {

        val selectedOrderUserMail = dialogView.findViewById<EditText>(R.id.user_mail).text.toString()
        val selectedOrderUserId = sharedUsers.find { it.userMail == selectedOrderUserMail }?.userId.orEmpty()
        val selectedOrderPayType = dialogView.findViewById<EditText>(R.id.pay_type).text.toString()
        val selectedOrderPayStatus = dialogView.findViewById<EditText>(R.id.pay_status).text.toString()
        val selectedOrderPrice = dialogView.findViewById<EditText>(R.id.order_price).text.toString().toIntOrNull()
        val selectedOrderDescription = dialogView.findViewById<EditText>(R.id.order_description).text.toString()
        val selectedOrderDeliveryAddress = dialogView.findViewById<EditText>(R.id.delivery_address).text.toString()
        val selectedOrderStatus = dialogView.findViewById<EditText>(R.id.order_status).text.toString()
        val selectedOrderAdditionalInfo = dialogView.findViewById<EditText>(R.id.additional_information).text.toString()
        val selectedOrderClientEmail = dialogView.findViewById<EditText>(R.id.client_email).text.toString()
        val selectedOrderClientPhoneNumber = dialogView.findViewById<EditText>(R.id.order_phone_number).text.toString()
        val selectedOrderClientName = dialogView.findViewById<EditText>(R.id.order_client_name).text.toString()
        val limitOrderDate = dialogView.findViewById<EditText>(R.id.edit_text_limit_order_date).text.toString()

        homeViewModel.zones.observe(viewLifecycleOwner) { zones ->
            if (zones != null && zones.isNotEmpty()) {
                ordersHandlerViewModel.editOrder(
                    selectedOrder,
                    zones,
                    selectedOrderClientName,
                    selectedOrderClientEmail,
                    selectedOrderClientPhoneNumber,
                    selectedOrderPayType,
                    selectedOrderPayStatus,
                    selectedOrderPrice,
                    selectedOrderUserId,
                    selectedOrderDescription,
                    selectedOrderDeliveryAddress,
                    selectedOrderStatus,
                    selectedOrderAdditionalInfo,
                    limitOrderDate,
                    selectedOrder.forceChecked
                )
            }
        }
        alertDialog.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
