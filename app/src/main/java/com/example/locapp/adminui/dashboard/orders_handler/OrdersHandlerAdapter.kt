package com.example.locapp.adminui.dashboard.orders_handler


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
import com.example.locapp.utils.datas.Users
import com.google.android.gms.maps.GoogleMap
import org.w3c.dom.Text

class OrdersHandlerAdapter(private val onDeleteClickListener: (Order) -> Unit, private val onEditClickListener: (Order) -> Unit) :
    ListAdapter<Order, OrdersHandlerAdapter.ViewHolder>(DiffCallback()) {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_orders_admin_handler, parent, false)
            return ViewHolder(view)
        }

        private var users: MutableList<Users> = mutableListOf()

        fun setUsers(usersList: List<Users>) {
            users.clear()
            users.addAll(usersList)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), users, onDeleteClickListener, onEditClickListener)
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val userMail: TextView = itemView.findViewById(R.id.user_mail)
            private val orderIdTextView: TextView = itemView.findViewById(R.id.orderNameTextView)
            private val orderStatusTextView: TextView = itemView.findViewById(R.id.orderStatusTextView)
            private val addInfoTextView: TextView = itemView.findViewById(R.id.additional_information_text)
            private val dateDeliveredTextView: TextView = itemView.findViewById(R.id.date_delivered)
            private val createdDateTextView: TextView = itemView.findViewById(R.id.created_date)
            private val limitDateTextView: TextView = itemView.findViewById(R.id.limit_date)
            private val orderClientNameTextView: TextView = itemView.findViewById(R.id.client_name)
            private val orderClientPhoneNumberTextView: TextView = itemView.findViewById(R.id.client_phone_number)
            private val orderClientEmailTextView: TextView = itemView.findViewById(R.id.client_email)
            private val deliveryAddressTextView: TextView = itemView.findViewById(R.id.delivery_address)
            private val deliveryCoordinatesTextView: TextView = itemView.findViewById(R.id.delivery_coordinates)
            private val descriptionTextView: TextView = itemView.findViewById(R.id.order_description)
            private val payTypeTextView: TextView = itemView.findViewById(R.id.pay_type)
            private val payStatusTextView: TextView = itemView.findViewById(R.id.pay_status)
            private val orderPriceTextView: TextView = itemView.findViewById(R.id.order_price)
            private val orderPinTextView: TextView = itemView.findViewById(R.id.order_pin)
            private val userAssigned: TextView = itemView.findViewById(R.id.user_assigned)
            private val zoneDelivery: TextView = itemView.findViewById(R.id.zoneDelivery)
            private val deleteButton: Button = itemView.findViewById(R.id.delete_order)
            private val editButton: Button = itemView.findViewById(R.id.edit_order)
            private val expandButton: ToggleButton = itemView.findViewById(R.id.expand_collapse_button)
            private val additionalInfoLayout: LinearLayout = itemView.findViewById(R.id.additional_info_layout)

            init {
                expandButton.setOnCheckedChangeListener { _, isChecked ->
                    additionalInfoLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
            }
            fun bind(order: Order, users: List<Users>, onDeleteClickListener: (Order) -> Unit, onEditClickListener: (Order) -> Unit) {
                orderIdTextView.text = order.orderId.toString()
                orderStatusTextView.text = order.orderStatus
                for(user in users){
                    if(order.deliverymanId.isEmpty()) {
                        userMail.text = ""
                    }
                    else if(user.userId == order.deliverymanId)
                    {
                        userMail.text = user.userMail
                        break;
                    }
                }
                when(order.orderStatus){
                    "Accepted" -> orderStatusTextView.setTextColor(Color.BLUE)
                    "In progress" -> orderStatusTextView.setTextColor(Color.WHITE)
                    "Delivered" -> orderStatusTextView.setTextColor(Color.GREEN)
                    "Another status" -> orderStatusTextView.setTextColor(Color.YELLOW)
                    else -> orderStatusTextView.setTextColor(Color.RED)
                }
                addInfoTextView.text = order.additionalInfo
                dateDeliveredTextView.text = order.orderDeliveredDate
                createdDateTextView.text = order.orderCreateDate
                limitDateTextView.text = order.limitOrderDate
                descriptionTextView.text = order.description
                payTypeTextView.text = order.payType
                payStatusTextView.text = order.payStatus
                orderClientNameTextView.text = order.orderClientName
                orderClientEmailTextView.text = order.orderClientEmail
                orderClientPhoneNumberTextView.text = order.orderPhoneNumber
                when(order.payStatus){
                    "Paid" -> payStatusTextView.setTextColor(Color.GREEN)
                    else -> payStatusTextView.setTextColor(Color.RED)
                }
                orderPriceTextView.text = order.orderPrice.toString()
                orderPinTextView.text = order.orderPin.toString()
                deliveryAddressTextView.text = order.takeToTheAddress
                deliveryCoordinatesTextView.text = buildString {
                    append("( ")
                    append(order.takeToCoords.longitude.toString())
                    append(", ")
                    append(order.takeToCoords.latitude.toString())
                    append(" )")
                }
                zoneDelivery.text = order.zoneDelivery
                userAssigned.text = order.deliverymanId
                deleteButton.setOnClickListener {
                    onDeleteClickListener.invoke(order)
                    Log.d("Admin", "Delete button clicked for order: $order")
                }

                editButton.setOnClickListener {
                    onEditClickListener.invoke(order)
                    Log.d("Admin", "Edit button clicked for order: $order")
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
