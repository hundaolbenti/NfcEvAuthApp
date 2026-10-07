package com.example.nfcevauth

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

// HCE: serves the latest phone_code as NDEF-text-like APDU response.
// ESP32 Phase-2: SELECT F039413031 -> GET_CODE -> returns code bytes + 9000.
// MVP: ESP32 uses manual "phone <code>"; this service makes taps work once
// the ESP32 DEP reader is completed.
class HceService : HostApduService() {

    companion object {
        var currentCode: String = ""
        private const val SELECT_PREFIX = "00A40400"
        private const val GET_CODE = "00CA0000"
        private const val OK = "9000"
    }

    override fun onCreate() {
        super.onCreate()
        // static phone token enrolled once at login — survives reboot, no codes to type
        currentCode = getSharedPreferences("ev", MODE_PRIVATE).getString("phone_token", "") ?: ""
    }

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        val hex = apdu.joinToString("") { "%02X".format(it) }
        return if (hex.startsWith(SELECT_PREFIX) || hex.startsWith(GET_CODE)) {
            (currentCode.toByteArray() + hexToBytes(OK))
        } else {
            hexToBytes("6A82") // file not found
        }
    }

    override fun onDeactivated(reason: Int) {}

    private fun hexToBytes(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        for (i in out.indices) out[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }
}
