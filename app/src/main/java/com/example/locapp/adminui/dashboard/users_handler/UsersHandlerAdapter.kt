package com.example.locapp.adminui.dashboard.users_handler

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.R
import com.example.locapp.utils.datas.Users

class UsersHandlerAdapter(private val onDeleteClickListener: (Users) -> Unit,
                          private val onEditClickListener: (Users) -> Unit,
                          private val onStatsClickListener: (Users) -> Unit) :
    ListAdapter<Users, UsersHandlerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_users_handler, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClickListener, onEditClickListener, onStatsClickListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val userNameTextView: TextView = itemView.findViewById(R.id.user_name)
        private val zoneAssignedTextView: TextView = itemView.findViewById(R.id.zone_assigned)
        private val userIDTextView: TextView = itemView.findViewById(R.id.user_id)
        private val userMailTextView: TextView = itemView.findViewById(R.id.user_mail)
        private val phoneNumber: TextView = itemView.findViewById(R.id.phone_number)
        private val deleteButton: Button = itemView.findViewById(R.id.delete_order)
        private val editButton: Button = itemView.findViewById(R.id.edit_order)
        private val showStats: Button = itemView.findViewById(R.id.show_stats)

        fun bind(user: Users, onDeleteClickListener: (Users) -> Unit, onEditClickListener: (Users) -> Unit,
                 onStatsClickListener:(Users) -> Unit) {
            userNameTextView.text = user.userCompleteName
            zoneAssignedTextView.text = user.deliverymanDetails.zonesAssigned.toString()
            userIDTextView.text = user.userId
            userMailTextView.text = user.userMail
            phoneNumber.text = user.userPhonenumber

            deleteButton.setOnClickListener {
                onDeleteClickListener.invoke(user)
                Log.d("Admin", "Delete button clicked for user: $user")
            }

            editButton.setOnClickListener {
                onEditClickListener.invoke(user)
                Log.d("Admin", "Edit button clicked for user: $user")
            }

            showStats.setOnClickListener{
                onStatsClickListener.invoke(user)
                Log.d("Admin", "Show stats button clicked for user: $user")
            }

        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Users>() {
        override fun areItemsTheSame(oldItem: Users, newItem: Users): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: Users, newItem: Users): Boolean {
            return oldItem == newItem
        }
    }
}