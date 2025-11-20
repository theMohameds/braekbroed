package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android_project_onwe.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {


    //  DEV MODE, ALWAYS LOG IN AS TEST USER
    private val DEV_MODE = false
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    val currentUserId: String?
        get() = if (DEV_MODE) DEV_USER_ID else FirebaseAuth.getInstance().currentUser?.uid


    private val _authEvent = MutableStateFlow("")
    val authEvent: StateFlow<String> = _authEvent
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn


    // SIGN UP (real, untouched)
    fun signUp(email: String, password: String, firstName: String, lastName: String) {
        if (DEV_MODE) {
            _authEvent.value = "Sign up disabled in DEV MODE"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authEvent.value = "Invalid email format"
            return
        }
        if (password.length < 8) {
            _authEvent.value = "Password must be at least 8 characters"
            return
        }
        if (firstName.isBlank() || lastName.isBlank()) {
            _authEvent.value = "Please enter your first and last name"
            return
        }

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        val db = FirebaseFirestore.getInstance()
                        val userData = User(firstName, lastName, email)

                        db.collection("user").document(currentUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                _isLoggedIn.value = true
                                _authEvent.value = "Sign up successful!"
                            }
                            .addOnFailureListener { e ->
                                currentUser.delete()
                                _authEvent.value = "Failed to save user data. Rolled back."
                            }
                    } else {
                        _authEvent.value = "Failed to get current user"
                    }
                } else {
                    // --- Safe Error Messages (C) ---
                    val message = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email already in use."
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password is too weak."
                        else -> "Sign up failed. Please try again."
                    }
                    _authEvent.value = message
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


            // DEV MODE LOGIN (bypass Firebase)
            if (DEV_MODE) {
                _isLoggedIn.value = true
                _authEvent.value = "DEV MODE LOGIN SUCCESSFUL"
                return
            }

            // NORMAL FIREBASE LOGIN
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


