package com.example.locapp.adminui.dashboard.users_handler

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.R
import com.example.locapp.databinding.FragmentUsersHandlerBinding
import com.example.locapp.ui.home.HomeViewModel
import com.example.locapp.utils.SharedViewModel
import com.example.locapp.utils.datas.Users
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsersHandlerFragment : Fragment() {

    private var _binding: FragmentUsersHandlerBinding? = null
    private val binding get() = _binding!!
    private lateinit var usersHandlerViewModel: UsersHandlerViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var homeViewModel: HomeViewModel
    private var availableZones: List<String> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersHandlerBinding.inflate(inflater, container, false)
        val recyclerView: RecyclerView = binding.recyclerViewUsersHandler
        usersHandlerViewModel = ViewModelProvider(this)[UsersHandlerViewModel::class.java]
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        homeViewModel.zonesName.observe(viewLifecycleOwner) { zones ->
            availableZones = zones
        }

        val adapter = UsersHandlerAdapter (
            onDeleteClickListener = { selectedUser ->
                usersHandlerViewModel.deleteUser(selectedUser)
            },
            onEditClickListener = { selectedUser ->
                showEditPopup(selectedUser)
            },
            onStatsClickListener = { selectedUser ->
                showStatsPopup(selectedUser)
            },
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        sharedViewModel.users.observe(viewLifecycleOwner) { allUsers ->
            adapter.submitList(allUsers)
            Log.d("Observer OrdersHandlerFragment","$/allUsers")
        }

        return binding.root
    }
    private fun showStatsPopup(selectedUser: Users) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_admin_users_stats, null)
        builder.setView(dialogView)

        val datePickedTextView: TextView = dialogView.findViewById(R.id.date_picked_textview)
        val selectDateButton: Button = dialogView.findViewById(R.id.select_date_button)
        val resetDateButton: Button = dialogView.findViewById(R.id.reset_date_button)
        val deliveredOrdersProgressBar: ProgressBar = dialogView.findViewById(R.id.delivered_orders_target)
        val deliveredVsFailedProgressBar: ProgressBar = dialogView.findViewById(R.id.delivered_vs_failed_stats)
        val numberTargetTextView: TextView = dialogView.findViewById(R.id.number_target)
        val deliveredVsFailedTextView: TextView = dialogView.findViewById(R.id.delivered_vs_failed)
        val distanceTraveledTextView: TextView = dialogView.findViewById(R.id.distance_traveled_stats)
        val timeWorkedTextView: TextView = dialogView.findViewById(R.id.time_worked_stats)

        val alertDialog = builder.create()
        alertDialog.show()

        usersHandlerViewModel.fetchUserStats(selectedUser.userId)
        usersHandlerViewModel.userName.observe(viewLifecycleOwner) {

        }
        usersHandlerViewModel.deliveryProgress.observe(viewLifecycleOwner) {
            deliveredOrdersProgressBar.progress = it.toInt()
        }
        usersHandlerViewModel.deliveredVsFailed.observe(viewLifecycleOwner) {
            deliveredVsFailedTextView.text = it

        }
        usersHandlerViewModel.deliveryProgressSuccesVsFailed.observe(viewLifecycleOwner) {
            deliveredVsFailedProgressBar.progress = it.toInt()
        }
        usersHandlerViewModel.distanceTraveledCount.observe(viewLifecycleOwner) {
            distanceTraveledTextView.text = it
        }
        usersHandlerViewModel.timeWorkedCount.observe(viewLifecycleOwner) {
            timeWorkedTextView.text = it
        }
        usersHandlerViewModel.orderText.observe(viewLifecycleOwner) {
            numberTargetTextView.text = it
        }

        usersHandlerViewModel.startStatsListener(selectedUser.userId, null, null)

        selectDateButton.setOnClickListener {
            showDatePickerDialog(datePickedTextView, resetDateButton) { startDate, endDate ->
                usersHandlerViewModel.startStatsListener(selectedUser.userId, startDate, endDate)
            }
        }

        resetDateButton.setOnClickListener {
            resetDateFilter(datePickedTextView)
            resetDateButton.visibility = View.GONE
            usersHandlerViewModel.startStatsListener(selectedUser.userId, null, null)
        }
    }

    private fun showDatePickerDialog(
        datePickedTextView: TextView,
        resetDateButton: Button,
        onDatesSelected: (startDate: Date, endDate: Date) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                val startDate = cal.time

                showEndDatePickerDialog(startDate, datePickedTextView, resetDateButton, onDatesSelected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showEndDatePickerDialog(
        startDate: Date,
        datePickedTextView: TextView,
        resetDateButton: Button,
        onDatesSelected: (startDate: Date, endDate: Date) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                val endDate = cal.time

                datePickedTextView.text = "${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate)}"
                resetDateButton.visibility = View.VISIBLE

                onDatesSelected(startDate, endDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun resetDateFilter(datePickedTextView: TextView) {
        datePickedTextView.text = "Stats of all time"
    }

    private fun showEditPopup(selectedUser: Users) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_admin_edit_users, null)
        builder.setView(dialogView)

        val alertDialog = builder.create()
        alertDialog.show()

        // Set initial values
        dialogView.findViewById<EditText>(R.id.user_name).setText(selectedUser.userCompleteName)
        dialogView.findViewById<EditText>(R.id.user_phonenumber).setText(selectedUser.userPhonenumber)
        dialogView.findViewById<EditText>(R.id.user_target).setText(selectedUser.deliverymanDetails.targetOrders.toString())

        // Set up zones
        val selectedZones = selectedUser.deliverymanDetails.zonesAssigned.toMutableList()
        val selectedZonesBooleanArray = BooleanArray(availableZones.size) { index ->
            selectedZones.contains(availableZones[index])
        }

        dialogView.findViewById<Button>(R.id.select_zones_button).setOnClickListener {
            val selectorBuilder = AlertDialog.Builder(requireContext())
            selectorBuilder.setTitle("Select Delivery Zones")
            selectorBuilder.setMultiChoiceItems(availableZones.toTypedArray(), selectedZonesBooleanArray) { _, index, isChecked ->
                if (isChecked) {
                    selectedZones.add(availableZones[index])
                } else {
                    selectedZones.remove(availableZones[index])
                }
            }
            selectorBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            selectorBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            selectorBuilder.create().show()
        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.complete).setOnClickListener {
            val selectedUserName = dialogView.findViewById<EditText>(R.id.user_name).text.toString()
            val selectedUserPhoneNumber = dialogView.findViewById<EditText>(R.id.user_phonenumber).text.toString()
            val selectedUserOrdersTarget = dialogView.findViewById<EditText>(R.id.user_target).text.toString().toIntOrNull()

            usersHandlerViewModel.editUsers(
                user = selectedUser,
                userName = selectedUserName,
                selectedUserPhoneNumber = selectedUserPhoneNumber,
                selectedUserOrdersTarget = selectedUserOrdersTarget,
                selectedUserDeliveryZones = selectedZones
            )
            alertDialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
