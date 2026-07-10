package com.cortecertoagendamentos.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "corte_certo_notificacoes"
        const val CHANNEL_NAME = "Notificações Corte Certo"
    }

    override fun onCreate() {
        super.onCreate()
        criarCanal()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FCM", "Novo Token:")
        Log.d("FCM", token)

        // Na próxima etapa enviaremos automaticamente para o PHP.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val dados = message.data

        val titulo = dados["title"] ?: "Corte Certo"
        val corpo = dados["body"] ?: ""

        Log.d("FCM", "Push recebido")

        mostrarNotificacao(titulo, corpo)
    }

    private fun mostrarNotificacao(
        titulo: String,
        mensagem: String
    ) {

        val intent = Intent(this, MainActivity::class.java)

        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        )

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(
                System.currentTimeMillis().toInt(),
                notification
            )
    }

    private fun criarCanal() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(

                CHANNEL_ID,

                CHANNEL_NAME,

                NotificationManager.IMPORTANCE_HIGH

            )

            channel.description =
                "Canal de notificações do Corte Certo"

            val manager =

                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}
