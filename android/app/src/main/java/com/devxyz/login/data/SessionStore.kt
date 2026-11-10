package com.devxyz.login.data

import android.content.Context

/**
 * Persists the server login session in app-private SharedPreferences
 * (files are only readable by this app). Holds the `dl1.` session token
 * plus the account identity; [localModeChosen] remembers that the user
 * tapped "Continue without an account" so the login screen is not forced
 * on every launch — local mode remains a first-class, fully working mode.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("devxyz_session", Context.MODE_PRIVATE)

    fun load(): AuthSession? {
        val token = prefs.getString("token", null)?.takeIf { it.isNotBlank() } ?: return null
        return AuthSession(
            token = token,
            userId = prefs.getString("user_id", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            workspaceId = prefs.getString("workspace_id", null),
            workspaceName = prefs.getString("workspace_name", null)
        )
    }

    fun save(session: AuthSession) {
        prefs.edit()
            .putString("token", session.token)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putString("workspace_id", session.workspaceId)
            .putString("workspace_name", session.workspaceName)
            .apply()
    }

    /** Clears the session token only; local profiles are never touched. */
    fun clear() {
        prefs.edit()
            .remove("token")
            .remove("user_id")
            .remove("email")
            .remove("workspace_id")
            .remove("workspace_name")
            .apply()
    }

    var localModeChosen: Boolean
        get() = prefs.getBoolean("local_mode", false)
        set(value) {
            prefs.edit().putBoolean("local_mode", value).apply()
        }
}
