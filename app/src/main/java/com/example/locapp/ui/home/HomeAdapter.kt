package com.example.locapp.ui.dashboard.accepted_orders

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.utils.datas.Order
import com.example.locapp.R
import com.example.locapp.ui.home.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import java.util.Collections

class HomeAdapter(
    private val viewModel: HomeViewModel,
    private val onShowOnMapClickListener: (Order) -> Unit,
    private val onCancelClickListener: (Order) -> Unit,
    private val onDeliverClickListener: (Order) -> Unit,
    private val onCallButton: (Order) -> Unit
) : ListAdapter<Order, HomeAdapter.ViewHolder>(DiffCallback()) {

    private var nextOrderId: Int = -1
    private var orders: MutableList<Order> = mutableListOf()
    private var threshold = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_accepted_order, parent, false)
        return ViewHolder(view)
    }
    fun setThreshold(newThreshold: Int) {
        threshold = newThreshold
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), nextOrderId, onShowOnMapClickListener, onCancelClickListener, onDeliverClickListener, onCallButton)
        if (position >= threshold) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.yellow))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderNameTextView: TextView = itemView.findViewById(R.id.orderNameTextView)
        private val deliveryAddressTextView: TextView = itemView.findViewById(R.id.delivery_address)
        private val limitDateTextView: TextView = itemView.findViewById(R.id.limit_date)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.order_description)
        private val payTypeTextView: TextView = itemView.findViewById(R.id.pay_type)
        private val payStatusTextView: TextView = itemView.findViewById(R.id.pay_status)
        private val orderPriceTextView: TextView = itemView.findViewById(R.id.order_price)
        private val orderClientNameTextView: TextView = itemView.findViewById(R.id.client_name)
        private val orderClientPhoneNumberTextView: TextView = itemView.findViewById(R.id.client_phone_number)
        private val orderClientEmailTextView: TextView = itemView.findViewById(R.id.client_email)

        private val additionalInfoLayout: LinearLayout = itemView.findViewById(R.id.additional_info_layout)
        private val deliveredButton: Button = itemView.findViewById(R.id.delivered_button)
        private val showItOnMapButton: Button = itemView.findViewById(R.id.show_it_on_map)
        private val cancelButton: Button = itemView.findViewById(R.id.cancel_button)
        private val callButton: Button = itemView.findViewById(R.id.call_button)
        private val expandButton: ToggleButton = itemView.findViewById(R.id.expand_collapse_button)
        init {
            expandButton.setOnCheckedChangeListener { _, isChecked ->
                additionalInfoLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        fun bind(
            order: Order,
            nextOrderId:Int,
            onShowOnMapClickListener: (Order) -> Unit,
            onCancelClickListener: (Order) -> Unit,
            onDeliverClickListener: (Order) -> Unit,
            onCallButton: (Order) -> Unit
        )
        {
            when(order.payStatus){
                "Paid" -> "Package #${order.orderId}".also {
                    orderNameTextView.text = it
                    orderNameTextView.setTextColor(Color.GREEN)
                }
                else ->"Package #${order.orderId}".also {
                    orderNameTextView.text = it
                    orderNameTextView.setTextColor(Color.RED)
                }
            }

            if(nextOrderId == order.orderId) {
                setDeliveredButtonEnabled(true)
            }
            else {
                setDeliveredButtonEnabled(false)
            }
            cancelButton.isEnabled = !order.forceChecked
            if (order.forceChecked) {
                cancelButton.setBackgroundColor(ContextCompat.getColor(cancelButton.context, android.R.color.darker_gray))
            } else {
                cancelButton.setBackgroundColor(ContextCompat.getColor(cancelButton.context, R.color.Cancel_buttons))
            }

            descriptionTextView.text = order.description
            payTypeTextView.text = order.payType
            payStatusTextView.text = order.payStatus
            orderPriceTextView.text = order.orderPrice.toString()
            deliveryAddressTextView.text = order.takeToTheAddress
            limitDateTextView.text = order.limitOrderDate
            orderClientEmailTextView.text = order.orderClientEmail
            orderClientNameTextView.text = order.orderClientName
            orderClientPhoneNumberTextView.text = order.orderPhoneNumber
            deliveredButton.setOnClickListener {
                onDeliverClickListener.invoke(order)
                Log.d("Adapter", "Deliver button clicked for order: $order")
            }

            cancelButton.setOnClickListener {
                onCancelClickListener.invoke(order)
                Log.d("Adapter", "Cancel button clicked for order: $order")
            }

            showItOnMapButton.setOnClickListener {
                onShowOnMapClickListener.invoke(order)
                Log.d("Adapter", "ShowRoute button clicked for order: $order")
            }

            callButton.setOnClickListener {
                onCallButton.invoke(order)
            }
        }

        fun setDeliveredButtonEnabled(enabled: Boolean) {
            deliveredButton.isEnabled = enabled
            if (enabled) {
                deliveredButton.setBackgroundColor(ContextCompat.getColor(deliveredButton.context, R.color.Finish_buttons))
            } else {
                deliveredButton.setBackgroundColor(ContextCompat.getColor(deliveredButton.context, android.R.color.darker_gray))
            }
        }

    }

    fun setOrders(ordersList: List<Order>, gMap: GoogleMap) {
        orders.clear()
        orders.addAll(ordersList)
        notifyDataSetChanged()
        gMap.clear()
    }

    fun setNextOrder(orderId: Int) {
        nextOrderId = orderId
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (orders.isNotEmpty() && fromPosition < orders.size && toPosition < orders.size) {
            val fromOrderId = orders[fromPosition].orderId
            val toOrderId = orders[toPosition].orderId

            viewModel.updateOrderIndex(fromOrderId, toPosition)
            viewModel.updateOrderIndex(toOrderId, fromPosition)

            Collections.swap(orders, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
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
