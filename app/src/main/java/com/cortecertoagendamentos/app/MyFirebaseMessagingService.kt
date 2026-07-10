package com.cortecertoagendamentos.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        const val CHANNEL_ID =
            "corte_certo_notificacoes"

        const val CHANNEL_NAME =
            "Notificações Corte Certo"

        private const val LOG_TAG = "CorteCertoFCM"
    }

    /*
    |--------------------------------------------------------------------------
    | INICIALIZAÇÃO DO SERVIÇO
    |--------------------------------------------------------------------------
    */

    override fun onCreate() {

        super.onCreate()

        criarCanalNotificacoes()
    }

    /*
    |--------------------------------------------------------------------------
    | TOKEN NOVO OU RENOVADO
    |--------------------------------------------------------------------------
    |
    | O Firebase pode renovar o token do aplicativo.
    |
    | Salvamos o token no mesmo SharedPreferences utilizado pela
    | AndroidBridge. Assim, o firebase-init.js sempre poderá obter
    | o token atual pela função:
    |
    | window.Android.getFirebaseToken()
    |
    |--------------------------------------------------------------------------
    */

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        salvarTokenFirebase(token)

        Log.d(
            LOG_TAG,
            "Token Firebase atualizado."
        )
    }

    private fun salvarTokenFirebase(token: String) {

        getSharedPreferences(
            AndroidBridge.PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                AndroidBridge.KEY_FCM_TOKEN,
                token
            )
            .apply()
    }

    /*
    |--------------------------------------------------------------------------
    | RECEBER NOTIFICAÇÃO
    |--------------------------------------------------------------------------
    |
    | O PHP envia as informações dentro do bloco "data":
    |
    | title
    | body
    | tipo
    | agendamento_id
    | barbearia_id
    | barbeiro_id
    |
    |--------------------------------------------------------------------------
    */

    override fun onMessageReceived(
        message: RemoteMessage
    ) {

        super.onMessageReceived(message)

        val dados = message.data

        val titulo =
            dados["title"]
                ?.takeIf { it.isNotBlank() }
                ?: "Corte Certo"

        val mensagem =
            dados["body"]
                ?.takeIf { it.isNotBlank() }
                ?: "Você recebeu uma nova notificação."

        val tipo =
            dados["tipo"] ?: ""

        val agendamentoId =
            dados["agendamento_id"] ?: ""

        Log.d(
            LOG_TAG,
            "Notificação recebida. Tipo: $tipo"
        )

        mostrarNotificacao(
            titulo = titulo,
            mensagem = mensagem,
            tipo = tipo,
            agendamentoId = agendamentoId
        )
    }

    /*
    |--------------------------------------------------------------------------
    | EXIBIR NOTIFICAÇÃO NATIVA
    |--------------------------------------------------------------------------
    */

    private fun mostrarNotificacao(
        titulo: String,
        mensagem: String,
        tipo: String,
        agendamentoId: String
    ) {

        criarCanalNotificacoes()

        /*
        |--------------------------------------------------------------------------
        | PERMISSÃO ANDROID 13+
        |--------------------------------------------------------------------------
        */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissao =

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permissao !=
                PackageManager.PERMISSION_GRANTED
            ) {

                Log.w(
                    LOG_TAG,
                    "Notificação não exibida: permissão não concedida."
                )

                return
            }
        }

        /*
        |--------------------------------------------------------------------------
        | ABRIR O APLICATIVO AO TOCAR
        |--------------------------------------------------------------------------
        */

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra(
                "tipo_notificacao",
                tipo
            )

            putExtra(
                "agendamento_id",
                agendamentoId
            )
        }

        val identificador =

            (
                System.currentTimeMillis()
                    and
                0x7FFFFFFF
            ).toInt()

        val pendingIntent =

            PendingIntent.getActivity(
                this,
                identificador,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
            )

        /*
        |--------------------------------------------------------------------------
        | MONTAR NOTIFICAÇÃO
        |--------------------------------------------------------------------------
        */

        val notificacao =

            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    titulo
                )
                .setContentText(
                    mensagem
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(mensagem)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        /*
        |--------------------------------------------------------------------------
        | PUBLICAR NOTIFICAÇÃO
        |--------------------------------------------------------------------------
        */

        if (
            !NotificationManagerCompat
                .from(this)
                .areNotificationsEnabled()
        ) {

            Log.w(
                LOG_TAG,
                "As notificações estão desativadas nas configurações do Android."
            )

            return
        }

        NotificationManagerCompat
            .from(this)
            .notify(
                identificador,
                notificacao
            )
    }

    /*
    |--------------------------------------------------------------------------
    | CANAL DE NOTIFICAÇÕES
    |--------------------------------------------------------------------------
    |
    | Obrigatório no Android 8 ou superior.
    |
    |--------------------------------------------------------------------------
    */

    private fun criarCanalNotificacoes() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {

            return
        }

        val canal = NotificationChannel(

            CHANNEL_ID,

            CHANNEL_NAME,

            NotificationManager.IMPORTANCE_HIGH

        ).apply {

            description =
                "Novos agendamentos e avisos do Corte Certo"

            enableVibration(true)

            setShowBadge(true)
        }

        val gerenciador =

            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        gerenciador.createNotificationChannel(
            canal
        )
    }
}
