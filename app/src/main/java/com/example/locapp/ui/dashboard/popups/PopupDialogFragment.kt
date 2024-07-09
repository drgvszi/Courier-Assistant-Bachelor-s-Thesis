package com.example.locapp.ui.dashboard.popups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.locapp.R

class PopupDialogFragment : DialogFragment() {

    private lateinit var sharedViewModel: PopupDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity())[PopupDialogViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.popup_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectStatusRadioGroup = view.findViewById<RadioGroup>(R.id.select_status)
        val completeOrderButton = view.findViewById<Button>(R.id.complete_order)
        val additionalInfoEditText  = view.findViewById<EditText>(R.id.additional_info_layout)

        completeOrderButton.setOnClickListener {
            val selectedStatus = when (selectStatusRadioGroup.checkedRadioButtonId) {
                R.id.status1 -> "Succes"
                R.id.status2 -> "Adresa incompletă"
                R.id.status3 -> "Refuz comandă"
                R.id.status4 -> "Acces restricționat"
                R.id.status5 -> "Amânare"
                R.id.status6 -> "Alt status"
                else -> ""
            }

            val additionalInfo = additionalInfoEditText.text.toString()

            sharedViewModel.selectedStatus.value = selectedStatus
            sharedViewModel.additionalInfo.value = additionalInfo

            dismiss()
        }
    }
}
