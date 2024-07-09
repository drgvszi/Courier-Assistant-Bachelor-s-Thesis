package com.example.locapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.locapp.AuthActivity
import com.example.locapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSettings
        val userEmailText: TextView = binding.userEmailText
        val signOutButton: Button = binding.signOutButton

        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        settingsViewModel.userEmail.observe(viewLifecycleOwner) {
            userEmailText.text = it
        }

        signOutButton.setOnClickListener {
            settingsViewModel.signOut()
            val intent = Intent(activity, AuthActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
