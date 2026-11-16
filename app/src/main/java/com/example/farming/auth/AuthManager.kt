package com.example.farming.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    val isUserSignedIn: Boolean
        get() = prefs.contains("user_email")

    fun setCustomUser(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun setGoogleAccount(account: GoogleSignInAccount) {
        val email = account.email ?: account.displayName ?: "Google User"
        prefs.edit().putString("user_email", email).apply()
    }

    fun getUserDisplayName(): String {
        return prefs.getString("user_email", null) ?: "Demo Farmer"
    }

    fun signOut() {
        prefs.edit().clear().apply()
    }
}
