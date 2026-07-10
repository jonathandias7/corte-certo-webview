package com.cortecertoagendamentos.app

import android.content.Context
import android.webkit.JavascriptInterface

class AndroidBridge(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /*
    |--------------------------------------------------------------------------
    | IDENTIFICA O APLICATIVO
    |--------------------------------------------------------------------------
    */

    @JavascriptInterface
    fun isAndroidApp(): Boolean {
        return true
    }

    /*
    |--------------------------------------------------------------------------
    | DEVOLVE O TOKEN NATIVO DO FIREBASE
    |--------------------------------------------------------------------------
    */

    @JavascriptInterface
    fun getFirebaseToken(): String {
        return preferences.getString(KEY_FCM_TOKEN, "") ?: ""
    }

    companion object {

        const val PREFS_NAME = "corte_certo_firebase"
        const val KEY_FCM_TOKEN = "fcm_token"

    }
}
