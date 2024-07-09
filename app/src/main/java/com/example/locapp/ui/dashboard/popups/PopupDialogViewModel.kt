package com.example.locapp.ui.dashboard.popups

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PopupDialogViewModel : ViewModel(){
    val selectedStatus = MutableLiveData<String>()
    val additionalInfo = MutableLiveData<String>()
}