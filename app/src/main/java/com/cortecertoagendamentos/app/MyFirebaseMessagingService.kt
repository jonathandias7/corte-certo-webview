package com.cortecertoagendamentos.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FCM", "Novo token: $token")

        // Depois vamos enviar esse token para o PHP.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "Mensagem recebida")

        message.data.forEach {
            Log.d("FCM", "${it.key} = ${it.value}")
        }
    }
}
