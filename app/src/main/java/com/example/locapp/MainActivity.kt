package com.example.locapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.locapp.databinding.ActivityBinding
import com.example.locapp.utils.FirebaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding
    private val authManager: FirebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
        val navMenu = navView.menu
        authManager.checkIfUserIsAdmin { isAdmin ->
            if (!isAdmin) {
                navMenu.removeItem(R.id.navigation_dashboard_admin)
            } else {
                Log.d("Auth", "Admin logged")
            }
        }
    }
}

