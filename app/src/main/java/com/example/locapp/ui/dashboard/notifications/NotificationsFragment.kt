package com.example.locapp.ui.dashboard.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locapp.databinding.FragmentNotificationsBinding
import com.example.locapp.ui.dashboard.stats.StatsViewModel
import com.example.locapp.ui.home.HomeViewModel
import com.example.locapp.utils.SharedViewModel
import com.example.locapp.utils.datas.Order

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var statsViewModel: StatsViewModel
    private lateinit var filterDropdown: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notificationsViewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        statsViewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        filterDropdown = binding.filterZonesDropdown
        filterDropdown.setSelection(0)

        val recyclerView = binding.recyclerViewNotifications
        val adapter = NotificationsAdapter(
            onAcceptClickListener = {selectedOrder ->
                notificationsViewModel.acceptOrder(selectedOrder.orderId)
            })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        statsViewModel.zonesAssigned.observe(viewLifecycleOwner){ zones ->
            sharedViewModel.orders.observe(viewLifecycleOwner) { allOrders ->
                if (allOrders.isEmpty()) {
                    return@observe
                }
                adapter.submitList(allOrders.filter { it.deliverymanId == "" && zones.contains(it.zoneDelivery) && it.orderStatus == "In progress"})
            }
        }


        filterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position) as String
                statsViewModel.zonesAssigned.observe(viewLifecycleOwner) { zones ->
                    sharedViewModel.orders.value?.let { allOrders ->
                        val allOrdersFiltered = allOrders.filter { it.deliverymanId == "" && zones.contains(it.zoneDelivery) && it.orderStatus == "In progress"}
                        val sortedOrders = sharedViewModel.sortOrdersNotf(allOrdersFiltered, selectedOption)
                        adapter.submitList(sortedOrders) {
                            recyclerView.scrollToPosition(0)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
