package com.example.nfcevauth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object Api {
    const val URL = "https://ihtmnshkyskidewdxihd.supabase.co"
    const val SECRET = "e5cdf7e607d4deeda663af8151d8a4ffae84"
    const val PUBKEY = "sb_publishable_SvzykDmS2G0eCgfpR86XSA_zXHUbzle"
    private val http = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("$URL$path")
            .addHeader("x-device-secret", SECRET)
            .addHeader("apikey", PUBKEY)
            .post(body.toString().toRequestBody(JSON))
            .build()
        http.newCall(req).execute().use { res ->
            val b = res.body?.string() ?: "{}"
            return JSONObject(b)
        }
    }

    fun ev(action: String, init: JSONObject.() -> Unit = {}): JSONObject {
        val o = JSONObject().put("action", action)
        o.init()
        return post("/functions/v1/ev", o)
    }

    fun admin(action: String, init: JSONObject.() -> Unit = {}): JSONObject {
        val o = JSONObject().put("action", action)
        o.init()
        return post("/functions/v1/nfc-admin", o)
    }
}
