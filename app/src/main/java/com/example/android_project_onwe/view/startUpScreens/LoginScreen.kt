package com.example.android_project_onwe.view.startUpScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignUpClick: () -> Unit // <-- added parameter for switching
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val buttonWidth = 120.dp

    // Collect events from the ViewModel
    LaunchedEffect(Unit) {
        authViewModel.authEvent.collectLatest { msg ->
            message = msg
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password
                )
            )

            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier.width(buttonWidth)
            ) {
                Text("Login")
            }

            // ----- SWITCH TO SIGNUP -----
            TextButton(onClick = onSignUpClick) {
                Text("Don't have an account? Sign Up")
            }
            // ----- END SWITCH TO SIGNUP -----

            if (message.isNotEmpty()) {
                Text(text = message, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
