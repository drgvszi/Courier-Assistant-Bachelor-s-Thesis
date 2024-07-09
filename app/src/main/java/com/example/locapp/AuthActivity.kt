package com.example.locapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.example.locapp.utils.FirebaseManager
import com.google.firebase.auth.*

class AuthActivity : AppCompatActivity() {
    //Auth
    private lateinit var usernameInput: EditText
    private lateinit var passInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var signupBtn: Button
    
    //Popup
    private lateinit var completeButton: Button
    private lateinit var userCompleteNameEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var cityEditText: EditText
    private lateinit var addressEditText: EditText

    private val authManager: FirebaseManager = FirebaseManager()

    override fun onStart() {
        super.onStart()
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            updateUI(currentUser, null, isLoginAttempt = true, isSignupAttempt = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_layout)
        usernameInput = findViewById(R.id.username_input)
        passInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        signupBtn = findViewById(R.id.signup_btn)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passInput.text.toString()
            if (isValidInput(username, password)) {
                showLoadingIndicator()
                authManager.login(username, password) { user, exception ->
                    hideLoadingIndicator()
                    updateUI(user, exception, isLoginAttempt = true, isSignupAttempt = false)
                }
            }
        }

        signupBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.popup_dialog_signup, null)
            if (dialogView.parent != null) {
                (dialogView.parent as ViewGroup).removeView(dialogView)
            }
            builder.setView(dialogView)
            userCompleteNameEditText = dialogView.findViewById(R.id.user_name)
            phoneNumberEditText = dialogView.findViewById(R.id.phone_number)
            addressEditText = dialogView.findViewById(R.id.user_address)
            completeButton = dialogView.findViewById(R.id.complete_order)

            val username = usernameInput.text.toString()
            val password = passInput.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                val alertDialog = builder.create()
                alertDialog.show()
                completeButton.setOnClickListener {
                    val userCompleteName = userCompleteNameEditText.text.toString()
                    val phoneNumber = phoneNumberEditText.text.toString()
                    if (isValidInput(userCompleteName, phoneNumber)) {
                        showLoadingIndicator()
                        authManager.signUp(username, password) { user, exception ->
                            hideLoadingIndicator()
                            if (user != null && exception == null) {
                                authManager.createUserDB(
                                    username, userCompleteName, phoneNumber,
                                    onSuccess = {
                                        showAlertDialog("Success", "The account was created! Now you can use the account to login")
                                    },
                                    onFailure = { e ->
                                        Log.e("UserDB", "Failed to create the user: $e")
                                        showAlertDialog("Error", "Failed to create user")
                                    }
                                )
                                alertDialog.dismiss()
                            } else {
                                updateUI(user, exception, isLoginAttempt = false, isSignupAttempt = true)
                            }
                        }
                    } else {
                        showAlertDialog("Something went wrong!", "Please, complete the following form!")
                    }
                }
            }
            else {
                showAlertDialog("Something went wrong!", "Please, complete the labels!")
            }
        }
    }

    private fun isValidInput(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }
    private fun isValidInput(userCompleteName: String, phoneNumber: String, city: String): Boolean {
        return userCompleteName.isNotEmpty() && phoneNumber.isNotEmpty() && city.isNotEmpty()
    }

    private fun updateUI(user: FirebaseUser?, exception: Exception?, isLoginAttempt: Boolean, isSignupAttempt: Boolean) {
        hideLoadingIndicator()
        if(isSignupAttempt && exception == null){
            showAlertDialog("Succes", "The account was created! Now you can use the account created to login")
        }
        if (user != null && isLoginAttempt) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else {
            when (exception) {
                //Firebase exception types
                is FirebaseAuthWeakPasswordException -> showAlertDialog("Error", "Weak password")
                is FirebaseAuthInvalidCredentialsException -> showAlertDialog("Error", "Invalid credentials")
                is FirebaseAuthUserCollisionException -> showAlertDialog("Error", "An account already exists with this email address.")
                else -> showAlertDialog("Error", "Authentication failed.")
            }
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showLoadingIndicator() {
        findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.GONE
    }
}