package com.example.nfcevauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import kotlin.concurrent.thread

class UserActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var polling = false
    private var sessionId: String? = null
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        val prefs = getSharedPreferences("ev", Context.MODE_PRIVATE)
        val uid = prefs.getString("user_id", "")!!
        val uname = prefs.getString("username", "")!!
        tvInfo = findViewById(R.id.tvInfo)
        tvInfo.text = "Hello $uname — loading..."

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { refresh(uid) }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            thread {
                try {
                    val r = Api.ev("tap-start") { put("charger", "Bench-01"); put("user_id", uid) }
                    runOnUiThread {
                        if (r.optBoolean("ok")) { sessionId = r.getString("session_id"); tvInfo.text = "CHARGING as $uname" } else tvInfo.text = "Start failed: " + r.optString("error")
                    }
                } catch (e: Exception) { runOnUiThread { tvInfo.text = "Net: ${e.message}" } }
            }
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopSession() }
        findViewById<Button>(R.id.btnPhone).setOnClickListener {
            // static token already in HCE — just show hint, no codes to type
            tvInfo.text = "Phone ready: tap phone to charger reader. Token enrolled once at login."
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            polling = false
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
        polling = true
        poll(uid)
    }

    private fun refresh(uid: String) {
        thread {
            try {
                val me = Api.ev("my-profile") { put("user_id", uid) }
                runOnUiThread { tvInfo.text = "Balance: %.3f kWh".format(me.optDouble("kwh_balance", 0.0)) }
            } catch (e: Exception) { runOnUiThread { tvInfo.text = "Net: ${e.message}" } }
        }
    }

    private fun poll(uid: String) {
        if (!polling) return
        thread {
            try {
                val me = Api.ev("my-profile") { put("user_id", uid) }
                val st = Api.ev("state") { put("charger", "Bench-01") }
                val sess = if (!st.isNull("session")) st.getJSONObject("session") else null
                val hist = Api.ev("my-sessions") { put("user_id", uid) }
                val arr = hist.optJSONArray("sessions")
                val last = if (arr != null && arr.length() > 0) arr.getJSONObject(0) else null
                runOnUiThread {
                    val bal = me.optDouble("kwh_balance", 0.0)
                    val live = if (sess != null) "\nLIVE: ${sess.optInt("elapsed_s")}s, ${sess.optDouble("kwh_used")} kWh by ${sess.optJSONObject("profiles")?.optString("username")}" else "\nCharger idle"
                    val h = if (last != null) "\nLast: ${last.optString("status")} ${last.optDouble("kwh_used")} kWh" else ""
                    tvInfo.text = "Balance: %.3f kWh%s%s".format(bal, live, h)
                    if (sess != null) sessionId = sess.getString("id")
                }
            } catch (_: Exception) { }
            handler.postDelayed({ poll(uid) }, 5000)
        }
    }

    private fun stopSession() {
        val sid = sessionId ?: return
        thread {
            try {
                val r = Api.ev("stop") { put("session_id", sid); put("reason", "cancel") }
                runOnUiThread { tvInfo.text = "Stopped. Used ${r.optDouble("kwh_used")} kWh"; sessionId = null }
            } catch (e: Exception) { runOnUiThread { tvInfo.text = "Net: ${e.message}" } }
        }
    }

    override fun onDestroy() { polling = false; super.onDestroy() }
}
