package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android_project_onwe.model.ProfileModel
import com.example.android_project_onwe.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {


    private val _authEvent = MutableStateFlow("")
    val authEvent: StateFlow<String> = _authEvent
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn


    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String = "" // optional if you want
    ) {

        val normalizedEmail = email.trim().lowercase()

        // --- Validation ---
        when {
            firstName.isBlank() || lastName.isBlank() -> {
                _authEvent.value = "Please enter your first and last name"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> {
                _authEvent.value = "Invalid email format"
                return
            }
            password.length < 8 -> {
                _authEvent.value = "Password must be at least 8 characters"
                return
            }
        }

        // --- Firebase sign up ---
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(normalizedEmail, password)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    val message = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                            "Email already in use."
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                            "Password is too weak."
                        else -> "Sign up failed. Please try again."
                    }

                    _authEvent.value = message
                    return@addOnCompleteListener
                }

                // --- User created successfully ---
                val currentUser = FirebaseAuth.getInstance().currentUser
                    ?: run {
                        _authEvent.value = "Failed to get current user"
                        return@addOnCompleteListener
                    }

                // Build Firestore profile
                val profile = ProfileModel(
                    firstName = firstName,
                    lastName = lastName,
                    phone = phoneNumber,
                    email = normalizedEmail,
                    notificationsEnabled = true
                )

                // Save to Firestore under "profile" collection
                FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(currentUser.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        _isLoggedIn.value = true
                        _authEvent.value = "Sign up successful!"
                    }
                    .addOnFailureListener {
                        currentUser.delete() // rollback user creation
                        _authEvent.value = "Failed to save profile. Rolled back."
                    }
            }
    }


    fun login(email: String, password: String) {

        if (email.isBlank() && password.isBlank()) {
            _authEvent.value = "Please enter your email & password"
            return
        }
        if (email.isBlank()) {
            _authEvent.value = "Please enter your email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authEvent.value = "Invalid email format"
            return
        }
        if (password.isBlank()) {
            _authEvent.value = "Please enter your password"
            return
        }

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    _authEvent.value = "Login successful!"
                } else {
                    _authEvent.value = task.exception?.message ?: "Invalid email or password."
                }
            }
        }
    }


