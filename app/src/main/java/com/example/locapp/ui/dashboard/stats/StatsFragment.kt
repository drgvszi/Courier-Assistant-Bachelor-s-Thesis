package com.example.locapp.ui.dashboard.stats

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.locapp.databinding.FragmentStatsBinding
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var datePickedTextView: TextView
    private lateinit var statsViewModel: StatsViewModel
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        statsViewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        datePickedTextView = binding.datePickedTextview

        val deliveryProgressBar: ProgressBar = binding.deliveredOrdersTarget
        val deliveredVsFailedStatsProgressBar: ProgressBar = binding.deliveredVsFailedStats
        val selectDateButton: Button = binding.selectDateButton
        val resetDateButton: Button = binding.resetDateButton
        val numberTargetTextView: TextView = binding.numberTarget
        val deliveredVsFailed: TextView = binding.deliveredVsFailed
        val deliveredOrders: TextView = binding.deliveredOrdersStats
        val textView: TextView = binding.userNameStats
        val refusedOrders: TextView = binding.refusedOrdersStats
        val failedOrders:TextView = binding.failedOrdersStats
        val restrictedAreaOrders: TextView = binding.restrictedAreaOrdersStats
        val expiredOrders: TextView = binding.expiredOrdersStats
        val anotherStatusOrders: TextView = binding.anotherStatusOrdersStats
        val incompleteAddressOrders: TextView = binding.incompleteAddressOrdersStats
        val distanceTraveled: TextView = binding.distanceTraveledStats
        val timeWorked: TextView = binding.timeWorkedStats

        statsViewModel.userName.observe(viewLifecycleOwner)
        {
            textView.text = it
        }

        statsViewModel.deliveryProgress.observe(viewLifecycleOwner) {
            deliveryProgressBar.progress = it.toInt()
        }

        statsViewModel.deliveryProgressSuccesVsFailed.observe(viewLifecycleOwner) {
            deliveredVsFailedStatsProgressBar.progress = it.toInt()
        }

        statsViewModel.orderText.observe(viewLifecycleOwner) {
            numberTargetTextView.text = it
        }

        statsViewModel.deliveredVsFailed.observe(viewLifecycleOwner) {
            deliveredVsFailed.text = it
        }

        statsViewModel.deliveredOrdersCount.observe(viewLifecycleOwner) {
            deliveredOrders.text = it
        }

        statsViewModel.failedOrdersCount.observe(viewLifecycleOwner) {
            failedOrders.text = it
        }
        statsViewModel.restrictedAreaOrdersCount.observe(viewLifecycleOwner) {
            restrictedAreaOrders.text = it
        }
        statsViewModel.refusedOrdersCount.observe(viewLifecycleOwner) {
            refusedOrders.text = it
        }
        statsViewModel.expiredOrdersCount.observe(viewLifecycleOwner) {
            expiredOrders.text = it
        }
        statsViewModel.anotherStatusOrdersCount.observe(viewLifecycleOwner) {
            anotherStatusOrders.text = it
        }
        statsViewModel.incompleteAddressOrdersCount.observe(viewLifecycleOwner) {
            incompleteAddressOrders.text = it
        }
        statsViewModel.distanceTraveledCount.observe(viewLifecycleOwner) {
            distanceTraveled.text = it + " km"
        }
        statsViewModel.timeWorkedCount.observe(viewLifecycleOwner) {
            timeWorked.text = it
        }

        selectDateButton.setOnClickListener {
            showDatePickerDialog(resetDateButton)
        }

        resetDateButton.setOnClickListener {
            resetDateFilter()
            resetDateButton.visibility = View.GONE
        }

        return root
    }

    private fun resetDateFilter() {
        startDate = null
        endDate = null
        statsViewModel.setDateRange(null, null)
        datePickedTextView.text = "All-Time Statistics"
    }

    private fun showDatePickerDialog(resetDateButton: Button) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                startDate = cal.time

                showEndDatePickerDialog(resetDateButton)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showEndDatePickerDialog(resetDateButton: Button) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                endDate = cal.time

                statsViewModel.setDateRange(startDate, endDate)
                setDateText(startDate, endDate)
                resetDateButton.visibility = View.VISIBLE
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setDateText(startDate: Date?, endDate: Date?) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateString = startDate?.let { dateFormat.format(it) } ?: ""
        val endDateString = endDate?.let { dateFormat.format(it) } ?: ""
        datePickedTextView.text = "$startDateString - $endDateString"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
