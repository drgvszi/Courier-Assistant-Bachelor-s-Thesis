package com.example.locapp.ui.dashboard.history

import android.graphics.Color.GREEN
import android.graphics.Color.RED
import android.graphics.Color.YELLOW
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.utils.datas.Order
import com.example.locapp.R
import org.w3c.dom.Text

class HistoryAdapter() :
    ListAdapter<Order, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_orders_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderNameTextView: TextView = itemView.findViewById(R.id.orderNameTextView)
        private val orderStatusTextView: TextView = itemView.findViewById(R.id.orderStatusTextView)
        private val addInfoTextView: TextView = itemView.findViewById(R.id.additional_information_text)
        private val dateDeliveredTextView: TextView = itemView.findViewById(R.id.date_delivered)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.order_description)
        private val payTypeTextView: TextView = itemView.findViewById(R.id.payType)
        private val zoneDelivery: TextView = itemView.findViewById(R.id.zoneDelivery)
        private val deliveryAddressTextView: TextView = itemView.findViewById(R.id.delivery_address)
        private val limitDate: TextView = itemView.findViewById(R.id.limit_date)
        fun bind(order: Order) {
            "Package #${order.orderId}".also { orderNameTextView.text = it }
            orderStatusTextView.text = order.orderStatus
            when(order.orderStatus){
                "Delivered" -> orderStatusTextView.setTextColor(GREEN)
                "Another status" -> orderStatusTextView.setTextColor(YELLOW)
                else -> orderStatusTextView.setTextColor(RED)
            }

            addInfoTextView.text = order.additionalInfo
            dateDeliveredTextView.text = order.orderDeliveredDate
            limitDate.text = order.limitOrderDate
            descriptionTextView.text = order.description
            payTypeTextView.text = order.payType
            deliveryAddressTextView.text = order.takeToTheAddress
            zoneDelivery.text = order.zoneDelivery

        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}
