package com.example.locapp.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.locapp.utils.datas.Users
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel : ViewModel() {

    private val _user = MutableLiveData<Users>()
    val userObj: LiveData<Users> = _user
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _userEmail = MutableLiveData<String>().apply {
        value = auth.currentUser?.email ?: ""
    }

    val userEmail: LiveData<String> = _userEmail

    private val _text = MutableLiveData<String>().apply {
        value = "Setari legate de contul personal/ aplicatie"
    }
    val text: LiveData<String> = _text


    fun signOut() {
        auth.signOut()
    }
}
