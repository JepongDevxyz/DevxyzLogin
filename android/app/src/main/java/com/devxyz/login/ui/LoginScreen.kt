package com.devxyz.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devxyz.login.data.ApiResult
import com.devxyz.login.data.AuthSession
import com.devxyz.login.data.ServerApi
import kotlinx.coroutines.launch

/**
 * Account login / registration (v4) — layout follows the GoLogin Android
 * docs login screenshot: "Log in" title, "Your email", "Password" with a
 * visibility eye, one solid green full-width button, account links below.
 *
 * Deliberately ABSENT (no dead controls): "Continue with Google" and
 * "Forgot password?" — the DevxyzLogin server has no OAuth and no
 * password-reset backend, so those buttons would do nothing. They can
 * appear the day the server grows those endpoints.
 *
 * "Continue without an account" keeps the app fully usable in local
 * mode — local profiles work exactly as before; only sync, API tokens
 * and server proxy checks need an account.
 */
@Composable
fun LoginScreen(
    onLoggedIn: (AuthSession) -> Unit,
    onContinueLocal: () -> Unit
) {
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()

    var registerMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val e = email.trim()
        when {
            e.isBlank() || password.isBlank() -> {
                error = "Enter your email and password"; return
            }
            registerMode && password.length < 6 -> {
                error = "Password must be at least 6 characters"; return
            }
        }
        busy = true
        error = null
        scope.launch {
            val result = if (registerMode) api.register(e, password, name.trim())
            else api.login(e, password)
            busy = false
            when (result) {
                is ApiResult.Success -> onLoggedIn(result.data)
                is ApiResult.Failure -> error = when {
                    // Server messages are honest and specific; surface them,
                    // with friendlier wording for the two common cases.
                    result.httpCode == 401 -> "Invalid email or password"
                    result.httpCode == 409 -> "This email is already registered — try logging in instead"
                    else -> result.message
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand mark (same teal "D" tile as the drawer header)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrandTeal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "D",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (registerMode) "Create account" else "Log in",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "DevxyzLogin — antidetect profiles, synced to your account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        if (registerMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Your email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (registerMode) "Create account" else "Log In")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                registerMode = !registerMode
                error = null
            },
            enabled = !busy
        ) {
            Text(
                if (registerMode) "Already have an account? Log in"
                else "Create account",
                color = BrandTealLink
            )
        }
        TextButton(onClick = onContinueLocal, enabled = !busy) {
            Text(
                "Continue without an account",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Local mode keeps profiles on this device only. Sync, API tokens and server proxy checks need an account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
