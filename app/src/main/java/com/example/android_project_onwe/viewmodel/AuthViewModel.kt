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
    private val _authEvent = MutableStateFlow("")
    val authEvent: StateFlow<String> = _authEvent

    private val _isLoggedIn = MutableStateFlow(false)

    fun signUp(email: String, password: String, firstName: String, lastName: String) {
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
                                // Both Auth & Firestore succeeded
                                _isLoggedIn.value = true
                                _authEvent.value = "Sign up successful!"
                            }
                            .addOnFailureListener { e ->
                                // Firestore failed -> delete the Auth user
                                currentUser.delete()
                                    .addOnCompleteListener { deleteTask ->
                                        if (deleteTask.isSuccessful) {
                                            _authEvent.value = "Failed to save user data. Account rolled back."
                                        } else {
                                            _authEvent.value = "Failed to save user data. Could not rollback Auth account."
                                        }
                                    }
                            }
                    } else {
                        _authEvent.value = "Failed to get current user"
                    }
                } else {
                    _authEvent.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }





    fun login(email: String, password: String) {
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