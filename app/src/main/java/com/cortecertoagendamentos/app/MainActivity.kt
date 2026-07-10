package com.cortecertoagendamentos.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserRequestCode = 100
    private val notificationPermissionRequestCode = 500

    private val siteUrl =
        "https://cortecertoagendamentos.com.br/"

    private val siteHost =
        "cortecertoagendamentos.com.br"

    /*
    |--------------------------------------------------------------------------
    | INICIALIZAÇÃO
    |--------------------------------------------------------------------------
    */

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        solicitarPermissaoNotificacoes()

        configurarFirebase()

        webView = WebView(this)

        setContentView(webView)

        configurarWebView()

        webView.loadUrl(siteUrl)

        configurarBotaoVoltar()
    }

    /*
    |--------------------------------------------------------------------------
    | PERMISSÃO DE NOTIFICAÇÕES
    |--------------------------------------------------------------------------
    */

    private fun solicitarPermissaoNotificacoes() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissao = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (permissao != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationPermissionRequestCode
            )
        }
    }

    /*
    |--------------------------------------------------------------------------
    | FIREBASE
    |--------------------------------------------------------------------------
    |
    | Obtém o token atual e o guarda no armazenamento privado do aplicativo.
    | A ponte AndroidBridge disponibilizará o token ao JavaScript.
    |
    |--------------------------------------------------------------------------
    */

    private fun configurarFirebase() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { tarefa ->

                if (!tarefa.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token = tarefa.result ?: return@addOnCompleteListener

                salvarTokenFirebase(token)
            }
    }

    private fun salvarTokenFirebase(token: String) {

        getSharedPreferences(
            AndroidBridge.PREFS_NAME,
            MODE_PRIVATE
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
    | CONFIGURAÇÃO DO WEBVIEW
    |--------------------------------------------------------------------------
    */

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarWebView() {

        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            settings.mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            settings.safeBrowsingEnabled = true
        }

        /*
        |--------------------------------------------------------------------------
        | IDENTIFICAÇÃO DO APLICATIVO
        |--------------------------------------------------------------------------
        */

        settings.userAgentString =
            settings.userAgentString + " CorteCertoAndroid/1.0"

        /*
        |--------------------------------------------------------------------------
        | COOKIES E SESSÃO PHP
        |--------------------------------------------------------------------------
        */

        CookieManager.getInstance().apply {

            setAcceptCookie(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                setAcceptThirdPartyCookies(
                    webView,
                    true
                )
            }
        }

        /*
        |--------------------------------------------------------------------------
        | PONTE ANDROID → JAVASCRIPT
        |--------------------------------------------------------------------------
        |
        | O JavaScript poderá chamar:
        |
        | window.Android.isAndroidApp()
        | window.Android.getFirebaseToken()
        |
        |--------------------------------------------------------------------------
        */

        webView.addJavascriptInterface(
            AndroidBridge(this),
            "Android"
        )

        webView.setLayerType(
            WebView.LAYER_TYPE_HARDWARE,
            null
        )

        configurarNavegacao()

        configurarUpload()
    }

    /*
    |--------------------------------------------------------------------------
    | NAVEGAÇÃO
    |--------------------------------------------------------------------------
    |
    | Somente o domínio oficial abre dentro do WebView.
    | Links externos são enviados ao aplicativo correspondente.
    |
    |--------------------------------------------------------------------------
    */

    private fun configurarNavegacao() {

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                val uri = request?.url ?: return false

                return tratarUrl(uri)
            }

            @Deprecated("Compatibilidade com versões antigas do Android")
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {

                if (url.isNullOrBlank()) {
                    return false
                }

                return tratarUrl(Uri.parse(url))
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler,
                error: SslError?
            ) {

                // Nunca ignora erro de certificado.
                handler.cancel()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {

                if (request?.isForMainFrame != true) {
                    return
                }

                view?.loadDataWithBaseURL(
                    siteUrl,
                    """
                    <!doctype html>
                    <html lang="pt-BR">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport"
                              content="width=device-width,initial-scale=1">
                        <title>Sem conexão</title>
                    </head>
                    <body style="
                        background:#07090d;
                        color:#fff;
                        font-family:Arial,sans-serif;
                        padding:40px 24px;
                        text-align:center;
                    ">
                        <h2>Sem conexão</h2>
                        <p>Verifique sua internet e abra o aplicativo novamente.</p>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    }

    private fun tratarUrl(uri: Uri): Boolean {

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()

        /*
        |--------------------------------------------------------------------------
        | DOMÍNIO INTERNO
        |--------------------------------------------------------------------------
        */

        if (
            (scheme == "https" || scheme == "http") &&
            (
                host == siteHost ||
                host == "www.$siteHost"
            )
        ) {

            return false
        }

        /*
        |--------------------------------------------------------------------------
        | LINKS EXTERNOS
        |--------------------------------------------------------------------------
        */

        return try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                uri
            )

            startActivity(intent)

            true

        } catch (exception: Exception) {

            true
        }
    }

    /*
    |--------------------------------------------------------------------------
    | UPLOAD DE ARQUIVOS
    |--------------------------------------------------------------------------
    */

    private fun configurarUpload() {

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                filePathCallback?.onReceiveValue(null)

                filePathCallback = callback

                val intent = Intent(
                    Intent.ACTION_GET_CONTENT
                ).apply {

                    type = "*/*"

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )
                }

                return try {

                    startActivityForResult(
                        Intent.createChooser(
                            intent,
                            "Selecionar arquivo"
                        ),
                        fileChooserRequestCode
                    )

                    true

                } catch (exception: Exception) {

                    filePathCallback = null

                    false
                }
            }
        }
    }

    /*
    |--------------------------------------------------------------------------
    | RESULTADO DO UPLOAD
    |--------------------------------------------------------------------------
    */

    @Deprecated("Compatibilidade com o seletor de arquivos atual")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        if (requestCode == fileChooserRequestCode) {

            val resultado = if (
                resultCode == Activity.RESULT_OK &&
                data?.data != null
            ) {

                arrayOf(data.data!!)

            } else {

                null
            }

            filePathCallback?.onReceiveValue(resultado)

            filePathCallback = null
        }

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )
    }

    /*
    |--------------------------------------------------------------------------
    | BOTÃO VOLTAR
    |--------------------------------------------------------------------------
    */

    private fun configurarBotaoVoltar() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (webView.canGoBack()) {

                        webView.goBack()

                    } else {

                        finish()
                    }
                }
            }
        )
    }

    /*
    |--------------------------------------------------------------------------
    | ENCERRAMENTO
    |--------------------------------------------------------------------------
    */

    override fun onDestroy() {

        if (::webView.isInitialized) {

            webView.removeJavascriptInterface(
                "Android"
            )

            webView.stopLoading()

            webView.webChromeClient = null

            webView.webViewClient =
                object : WebViewClient() {}

            webView.destroy()
        }

        super.onDestroy()
    }
}
