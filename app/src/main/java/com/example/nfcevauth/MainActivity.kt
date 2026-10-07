package com.example.nfcevauth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val http = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUrl = findViewById<EditText>(R.id.etUrl)
        val etSecret = findViewById<EditText>(R.id.etSecret)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val tvClaim = findViewById<TextView>(R.id.tvClaim)
        val tvPass = findViewById<TextView>(R.id.tvPass)
        val tvLog = findViewById<TextView>(R.id.tvLog)

        findViewById<Button>(R.id.btnCreatePending).setOnClickListener {
            val url = etUrl.text.toString().trim() + "/functions/v1/nfc-admin"
            val body = JSONObject()
                .put("action", "create-pending")
                .put("username", etUsername.text.toString().trim())
                .put("email", etEmail.text.toString().trim())
                .toString()
            post(url, etSecret.text.toString().trim(), body) { ok, resp ->
                runOnUiThread {
                    if (ok) {
                        val code = JSONObject(resp).optString("claim_code", "?")
                        tvClaim.text = "CLAIM CODE: $code\nOn ESP32 serial: enroll $code, then tap blank card."
                    } else {
                        tvClaim.text = "Error: $resp"
                    }
                    tvLog.text = resp
                }
            }
        }

        findViewById<Button>(R.id.btnGetPass).setOnClickListener {
            val url = etUrl.text.toString().trim() + "/functions/v1/nfc-admin"
            val body = JSONObject()
                .put("action", "request-pass")
                .put("email", etEmail.text.toString().trim())
                .toString()
            post(url, etSecret.text.toString().trim(), body) { ok, resp ->
                runOnUiThread {
                    if (ok) {
                        val code = JSONObject(resp).optString("phone_code", "?")
                        HceService.currentCode = code
                        tvPass.text = "PHONE CODE: $code\nHCE active. Tap phone to reader, or on ESP32: phone $code"
                    } else {
                        tvPass.text = "Error: $resp"
                    }
                    tvLog.text = resp
                }
            }
        }
    }

    private fun post(url: String, secret: String, json: String, cb: (Boolean, String) -> Unit) {
        thread {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("x-device-secret", secret)
                    .addHeader("apikey", "sb_publishable_SvzykDmS2G0eCgfpR86XSA_zXHUbzle")
                    .post(json.toRequestBody(JSON))
                    .build()
                http.newCall(req).execute().use { res ->
                    val b = res.body?.string() ?: ""
                    cb(res.isSuccessful, b)
                }
            } catch (e: Exception) {
                cb(false, e.message ?: "net error")
            }
        }
    }
}
