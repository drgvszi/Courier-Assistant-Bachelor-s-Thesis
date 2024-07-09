package com.example.locapp.ui.dashboard.notifications

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

class NotificationsAdapter(private val onAcceptClickListener: (Order) -> Unit) :
    ListAdapter<Order, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onAcceptClickListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderNameTextView: TextView = itemView.findViewById(R.id.orderNameTextView)
        private val orderPayStatusTextView: TextView = itemView.findViewById(R.id.pay_status)
        private val orderPayTypeTextView: TextView = itemView.findViewById(R.id.pay_type)
        private val orderPriceTextView: TextView = itemView.findViewById(R.id.order_price)
        private val orderClientNameTextView: TextView = itemView.findViewById(R.id.client_name)
        private val orderClientPhoneNumberTextView: TextView = itemView.findViewById(R.id.client_phone_number)
        private val orderClientEmailTextView: TextView = itemView.findViewById(R.id.client_email)
        private val orderCreateDateTextView: TextView = itemView.findViewById(R.id.order_create_date)
        private val orderLimitDateTextView: TextView = itemView.findViewById(R.id.order_limit_date)
        private val deliveryAddressTextView: TextView = itemView.findViewById(R.id.delivery_address)
        private val acceptButton: Button = itemView.findViewById(R.id.accept_button)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.order_description)
        private val zoneDelivery: TextView = itemView.findViewById(R.id.zoneDelivery)

        fun bind(order: Order, onAcceptClickListener: (Order) -> Unit) {
            "Package #${order.orderId}".also { orderNameTextView.text = it }
            orderLimitDateTextView.text = order.limitOrderDate
            orderPayStatusTextView.text = order.payStatus
            orderPayTypeTextView.text = order.payType
            orderPriceTextView.text = order.orderPrice.toString()
            orderClientEmailTextView.text = order.orderClientEmail
            orderClientNameTextView.text = order.orderClientName
            orderClientPhoneNumberTextView.text = order.orderPhoneNumber
            orderCreateDateTextView.text = order.orderCreateDate
            descriptionTextView.text = order.description
            deliveryAddressTextView.text = order.takeToTheAddress
            zoneDelivery.text = order.zoneDelivery
            acceptButton.setOnClickListener {
                onAcceptClickListener.invoke(order)
                Log.d("Adapter", "Accept button clicked for order: $order")
            }
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
