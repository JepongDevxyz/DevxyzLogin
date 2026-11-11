package com.devxyz.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.data.ApiResult
import com.devxyz.login.data.AuthSession
import com.devxyz.login.data.ServerApi
import kotlinx.coroutines.launch

/**
 * Account login / registration — v2.6.0 parity pass against the
 * July-2026 evidenced GoLogin login layout (spec v1 appendix):
 * white screen, the logo mark + app name centered, "Your email" /
 * "Password" (eye) fields — here in the v2.4.0 outlined-box style —
 * one solid green full-width button, a plan/trial banner card under
 * it carrying OUR real plan state, account links below, and a
 * footer version line (the installed package's real versionName).
 *
 * Deliberately ABSENT (no dead controls): "Continue with Google" —
 * the DevxyzLogin server has no OAuth backend, so that button would
 * do nothing; it can appear the day the server grows the endpoint.
 * "Forgot password?" is absent for the same reason (no reset flow).
 *
 * "Continue without an account" keeps the app fully usable in local
 * mode — local profiles work exactly as before; only sync, API tokens
 * and server proxy checks need an account.
 */
@Composable
fun LoginScreen(
    plan: String,
    onLoggedIn: (AuthSession) -> Unit,
    onContinueLocal: () -> Unit
) {
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var registerMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

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
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        // Brand mark + app name, centered (evidenced layout).
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "D",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "DevxyzLogin",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (registerMode) "Create an account" else "Log in",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        if (registerMode) {
            SpecTextField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        SpecTextField(
            label = "Your email",
            value = email,
            onValueChange = { email = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        SpecTextField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailing = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = BrandGreen,
                    strokeWidth = 2.dp
                )
            }
        } else {
            GreenButton(
                text = if (registerMode) "Create account" else "Sign in",
                onClick = { submit() }
            )
        }

        // Plan/trial banner card — same shape as the profile list's
        // banner, carrying our REAL plan state. Informational here:
        // plan changes live in Settings → Billing Plans after login.
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.Filled.Schedule, contentDescription = null,
                tint = StatusBlue, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = StatusBlue)) {
                        append(
                            if (plan == "Free") "You're on the Free plan."
                            else "Current plan: $plan."
                        )
                    }
                    append(" Sign in to sync your profiles, proxies and API tokens across devices.")
                },
                style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
            )
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
                color = BrandGreenDeep
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
        Spacer(Modifier.height(28.dp))
        // Footer version line — the installed build's real version.
        Text(
            "DevxyzLogin" + (versionName?.let { " v$it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
