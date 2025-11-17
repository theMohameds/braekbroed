package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {


    //  DEV MODE, ALWAYS LOG IN AS TEST USER

    private val DEV_MODE = true
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
                    _authEvent.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }



    // LOGIN
    fun login(email: String, password: String) {

        if (DEV_MODE) {
            _isLoggedIn.value = true
            _authEvent.value = "DEV MODE LOGIN SUCCESSFUL"
            return
        }

        // LOGIN (if dev mode is off)
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _isLoggedIn.value = true
                            _authEvent.value = "Login successful!"
                        } else {
                            _authEvent.value = task.exception?.message ?: "Login failed"
                        }
                    }
            } catch (e: Exception) {
                _authEvent.value = e.message ?: "Unexpected error"
            }
        }
    }
}
