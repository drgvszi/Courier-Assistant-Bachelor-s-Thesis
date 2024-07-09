package com.example.locapp.adminui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.example.locapp.R

class DashboardAdminFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the toolbar
        val toolbar: Toolbar = view.findViewById(R.id.toolbar_admin_dashboard)
        val toolbarNavHost = childFragmentManager.findFragmentById(R.id.toolbar_nav_host_fragment) as NavHostFragment
        val toolbarNavController = toolbarNavHost.navController

        // Set up the toolbar menu
        toolbarNavController.addOnDestinationChangedListener{ _, destination, _ ->
            toolbar.title = destination.label
        }

        toolbar.setOnMenuItemClickListener { item ->
            val currentDestination = toolbarNavController.currentDestination
            val startDestination = R.id.navigation_dashboard_admin
            if (currentDestination?.id != startDestination) {
                toolbarNavController.navigate(item.itemId)
                true
            } else {
                false
            }
        }
    }
}
