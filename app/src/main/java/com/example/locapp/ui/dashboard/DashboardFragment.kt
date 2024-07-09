package com.example.locapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.example.locapp.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)

        // Setup toolbar
        val toolbarNavHost = childFragmentManager.findFragmentById(R.id.toolbar_nav_host_fragment) as NavHostFragment
        val toolbarNavController = toolbarNavHost.navController

        toolbarNavController.addOnDestinationChangedListener{ _, destination, _ ->
            toolbar.title = destination.label
        }

        toolbar.setOnMenuItemClickListener { item ->
            val currentDestination = toolbarNavController.currentDestination
            val startDestination = R.id.navigation_dashboard

            if (currentDestination?.id != startDestination) {
                // Navigate to the destination if it's not the start destination
                toolbarNavController.navigate(item.itemId)
                true
            } else {
                false
            }
        }
    }
}
