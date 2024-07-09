package com.example.locapp.ui.dashboard.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locapp.databinding.FragmentHistoryBinding
import com.example.locapp.utils.SharedViewModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerViewHistory
        val adapter = HistoryAdapter()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel.orders.observe(viewLifecycleOwner) { orders ->
            val filterOrders = orders.filter {
                it.deliverymanId == sharedViewModel.currentUserId && it.orderStatus != "In progress"
                        && it.orderStatus!="Accepted"}
            adapter.submitList(filterOrders)
            Log.d("Observer","History")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
