package com.example.nfcevauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        val prefs = getSharedPreferences("ev", Context.MODE_PRIVATE)
        val admin = prefs.getString("username", "")!!
        val tv = findViewById<TextView>(R.id.tvAdmin)
        val etUser = findViewById<EditText>(R.id.etNewUser)
        val etPass = findViewById<EditText>(R.id.etNewPass)
        val etKwh = findViewById<EditText>(R.id.etKwh)
        val etUid = findViewById<EditText>(R.id.etUid)

        fun run(msg: String, fn: () -> String) {
            tv.text = "$msg..."
            thread {
                val r = try { fn() } catch (e: Exception) { "Net: ${e.message}" }
                runOnUiThread { tv.text = r }
            }
        }
        findViewById<Button>(R.id.btnCreateUser).setOnClickListener {
            run("Creating user...") {
                val r = Api.ev("create-user") {
                    put("admin_username", admin); put("username", etUser.text.toString().trim())
                    put("password", etPass.text.toString()); put("kwh", etKwh.text.toString().toDoubleOrNull() ?: 20.0)
                }
                if (r.optBoolean("ok")) "User ${etUser.text} created" else "Error: " + r.optString("error")
            }
        }
        findViewById<Button>(R.id.btnTopup).setOnClickListener {
            run("Topping up...") {
                val r = Api.ev("top-up") {
                    put("admin_username", admin); put("username", etUser.text.toString().trim())
                    put("kwh", etKwh.text.toString().toDoubleOrNull() ?: 5.0)
                }
                if (r.optBoolean("ok")) "Topped up, new bal ${r.optDouble("kwh_balance")}" else "Error: " + r.optString("error")
            }
        }
        findViewById<Button>(R.id.btnEnrollCode).setOnClickListener {
            run("Making claim code...") {
                // reuse nfc-admin pending flow (tap-to-enroll kept)
                val o = org.json.JSONObject().put("action", "create-pending")
                    .put("username", etUser.text.toString().trim()).put("email", etUser.text.toString().trim() + "@ev.local")
                val r = Api.post("/functions/v1/nfc-admin", o)
                "On ESP32 serial: enroll ${r.optString("claim_code")}, tap card. Then Assign card below."
            }
        }
        findViewById<Button>(R.id.btnAssign).setOnClickListener {
            run("Assigning card...") {
                val r = Api.ev("assign-card") {
                    put("admin_username", admin); put("username", etUser.text.toString().trim())
                    put("card_uid", etUid.text.toString().trim().uppercase())
                }
                if (r.optBoolean("ok")) "Card ${etUid.text} → ${etUser.text}" else "Error: " + r.optString("error")
            }
        }
        findViewById<Button>(R.id.btnUnlink).setOnClickListener {
            run("Unlinking...") {
                val r = Api.ev("unlink-card") { put("admin_username", admin); put("card_uid", etUid.text.toString().trim().uppercase()) }
                if (r.optBoolean("ok")) "Card unlinked (blank it with ESP32 clear too)" else "Error: " + r.optString("error")
            }
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }
}
