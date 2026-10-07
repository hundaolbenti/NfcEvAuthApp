package com.example.nfcevauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val prefs = getSharedPreferences("ev", Context.MODE_PRIVATE)
        // auto-route if already logged in
        val savedRole = prefs.getString("role", null)
        if (prefs.contains("user_id") && savedRole != null) {
            route(savedRole); return
        }
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val tvErr = findViewById<TextView>(R.id.tvErr)
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val u = etUser.text.toString().trim()
            val p = etPass.text.toString()
            tvErr.text = "Logging in..."
            thread {
                try {
                    val r = Api.ev("login") { put("username", u); put("password", p) }
                    if (!r.has("user_id")) {
                        runOnUiThread { tvErr.text = "Login failed: " + r.optString("error") }
                        return@thread
                    }
                    // ensure static phone token exists for tap-to-phone (one-time enroll)
                    var token = prefs.getString("phone_token", null)
                    if (token.isNullOrEmpty()) {
                        try {
                            val t = Api.ev("issue-phone-token") { put("user_id", r.getString("user_id")) }
                            token = t.optString("phone_token", "")
                        } catch (_: Exception) { }
                    }
                    prefs.edit()
                        .putString("user_id", r.getString("user_id"))
                        .putString("username", r.getString("username"))
                        .putString("role", r.getString("role"))
                        .putString("phone_token", token ?: "")
                        .apply()
                    HceService.currentCode = token ?: ""
                    runOnUiThread { route(r.getString("role")) }
                } catch (e: Exception) {
                    runOnUiThread { tvErr.text = "Network: ${e.message}" }
                }
            }
        }
    }

    private fun route(role: String) {
        val me = if (role == "admin") AdminActivity::class.java else UserActivity::class.java
        startActivity(Intent(this, me))
        finish()
    }
}
