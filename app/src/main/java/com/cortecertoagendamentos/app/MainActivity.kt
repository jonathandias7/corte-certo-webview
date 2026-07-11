package com.cortecertoagendamentos.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
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

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        val permissao =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

        if (
            permissao !=
            PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                notificationPermissionRequestCode
            )
        }
    }

    /*
    |--------------------------------------------------------------------------
    | FIREBASE
    |--------------------------------------------------------------------------
    */

    private fun configurarFirebase() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { tarefa ->

                if (!tarefa.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token =
                    tarefa.result
                        ?: return@addOnCompleteListener

                salvarTokenFirebase(token)
            }
    }

    private fun salvarTokenFirebase(
        token: String
    ) {

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

        val settings =
            webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode =
            WebSettings.LOAD_DEFAULT

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.LOLLIPOP
        ) {

            settings.mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            settings.safeBrowsingEnabled = true
        }

        /*
        |--------------------------------------------------------------------------
        | IDENTIFICAÇÃO DO APLICATIVO
        |--------------------------------------------------------------------------
        */

        settings.userAgentString =
            settings.userAgentString +
            " CorteCertoAndroid/1.0"

        /*
        |--------------------------------------------------------------------------
        | COOKIES E SESSÃO PHP
        |--------------------------------------------------------------------------
        */

        CookieManager.getInstance().apply {

            setAcceptCookie(true)

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP
            ) {

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
    */

    private fun configurarNavegacao() {

        webView.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {

                    val uri =
                        request?.url
                            ?: return false

                    return tratarUrl(uri)
                }

                @Deprecated(
                    "Compatibilidade com versões antigas do Android"
                )
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {

                    if (url.isNullOrBlank()) {
                        return false
                    }

                    return tratarUrl(
                        Uri.parse(url)
                    )
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler,
                    error: SslError?
                ) {

                    handler.cancel()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {

                    if (
                        request?.isForMainFrame !=
                        true
                    ) {
                        return
                    }

                    view?.loadDataWithBaseURL(
                        siteUrl,
                        """
                        <!doctype html>
                        <html lang="pt-BR">
                        <head>
                            <meta charset="UTF-8">
                            <meta
                                name="viewport"
                                content="width=device-width,initial-scale=1"
                            >
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
                            <p>
                                Verifique sua internet e abra
                                o aplicativo novamente.
                            </p>
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

    private fun tratarUrl(
        uri: Uri
    ): Boolean {

        val scheme =
            uri.scheme?.lowercase()

        val host =
            uri.host?.lowercase()

        /*
        |--------------------------------------------------------------------------
        | DOMÍNIO INTERNO
        |--------------------------------------------------------------------------
        */

        if (
            (scheme == "https" ||
             scheme == "http") &&
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

            val intent =
                Intent(
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
    | UPLOAD DE IMAGENS
    |--------------------------------------------------------------------------
    |
    | Android 13+:
    | - utiliza o seletor oficial de fotos.
    |
    | Android 12 ou inferior:
    | - utiliza ACTION_OPEN_DOCUMENT.
    |
    | Nenhuma das opções exige acesso amplo à galeria.
    |
    |--------------------------------------------------------------------------
    */

    private fun configurarUpload() {

        webView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowFileChooser(
                    webView: WebView?,
                    callback:
                        ValueCallback<Array<Uri>>?,
                    fileChooserParams:
                        FileChooserParams?
                ): Boolean {

                    if (callback == null) {
                        return false
                    }

                    /*
                    |--------------------------------------------------------------------------
                    | CANCELAR CALLBACK ANTERIOR
                    |--------------------------------------------------------------------------
                    */

                    filePathCallback
                        ?.onReceiveValue(null)

                    filePathCallback =
                        callback

                    val intent =
                        criarIntentSeletor(
                            fileChooserParams
                        )

                    return try {

                        startActivityForResult(
                            intent,
                            fileChooserRequestCode
                        )

                        true

                    } catch (
                        exception: Exception
                    ) {

                        filePathCallback
                            ?.onReceiveValue(null)

                        filePathCallback = null

                        false
                    }
                }
            }
    }

    /*
    |--------------------------------------------------------------------------
    | CRIAR SELETOR
    |--------------------------------------------------------------------------
    */

    private fun criarIntentSeletor(
        parametros:
            WebChromeClient.FileChooserParams?
    ): Intent {

        val tiposAceitos =
            obterTiposAceitos(
                parametros
            )

        val permiteMultiplos =
            parametros?.mode ==
            WebChromeClient
                .FileChooserParams
                .MODE_OPEN_MULTIPLE

        val somenteImagens =
            tiposAceitos.isEmpty() ||
            tiposAceitos.all {
                it.startsWith("image/")
            }

        /*
        |--------------------------------------------------------------------------
        | ANDROID 13 OU SUPERIOR
        |--------------------------------------------------------------------------
        |
        | Para seleção de uma única imagem, abre o Photo Picker oficial.
        |
        |--------------------------------------------------------------------------
        */

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU &&
            somenteImagens &&
            !permiteMultiplos
        ) {

            return Intent(
                MediaStore.ACTION_PICK_IMAGES
            ).apply {

                type =
                    if (
                        tiposAceitos.size == 1
                    ) {

                        tiposAceitos[0]

                    } else {

                        "image/*"
                    }

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        /*
        |--------------------------------------------------------------------------
        | ANDROID 12 OU INFERIOR / MÚLTIPLOS ARQUIVOS
        |--------------------------------------------------------------------------
        */

        return Intent(
            Intent.ACTION_OPEN_DOCUMENT
        ).apply {

            addCategory(
                Intent.CATEGORY_OPENABLE
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            type =
                when {

                    tiposAceitos.size == 1 -> {

                        tiposAceitos[0]
                    }

                    somenteImagens -> {

                        "image/*"
                    }

                    else -> {

                        "*/*"
                    }
                }

            if (
                tiposAceitos.size > 1
            ) {

                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    tiposAceitos
                )
            }

            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                permiteMultiplos
            )
        }
    }

    /*
    |--------------------------------------------------------------------------
    | TIPOS DE ARQUIVO ACEITOS
    |--------------------------------------------------------------------------
    |
    | Respeita o atributo accept do input HTML.
    | Quando o HTML não informa o tipo, utiliza imagens como padrão.
    |
    |--------------------------------------------------------------------------
    */

    private fun obterTiposAceitos(
        parametros:
            WebChromeClient.FileChooserParams?
    ): Array<String> {

        val tipos =
            parametros
                ?.acceptTypes
                ?.flatMap { valor ->

                    valor.split(",")
                }
                ?.mapNotNull { valor ->

                    normalizarTipoAceito(
                        valor
                    )
                }
                ?.distinct()
                ?.toTypedArray()
                ?: emptyArray()

        return tipos
    }

    private fun normalizarTipoAceito(
        valor: String
    ): String? {

        val tipo =
            valor
                .trim()
                .lowercase()

        if (
            tipo.isBlank() ||
            tipo == "*/*"
        ) {

            return null
        }

        /*
        |--------------------------------------------------------------------------
        | EXTENSÃO: .jpg, .png, .webp...
        |--------------------------------------------------------------------------
        */

        if (tipo.startsWith(".")) {

            val extensao =
                tipo.removePrefix(".")

            return MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(
                    extensao
                )
        }

        /*
        |--------------------------------------------------------------------------
        | MIME TYPE: image/jpeg, image/png, image/*
        |--------------------------------------------------------------------------
        */

        if (tipo.contains("/")) {

            return tipo
        }

        return null
    }

    /*
    |--------------------------------------------------------------------------
    | RESULTADO DO SELETOR
    |--------------------------------------------------------------------------
    |
    | parseResult trata corretamente:
    |
    | - seleção única;
    | - seleção múltipla;
    | - cancelamento;
    | - diferentes provedores de imagens.
    |
    |--------------------------------------------------------------------------
    */

    @Deprecated(
        "Compatibilidade com o seletor de arquivos atual"
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        if (
            requestCode ==
            fileChooserRequestCode
        ) {

            val resultado =
                WebChromeClient
                    .FileChooserParams
                    .parseResult(
                        resultCode,
                        data
                    )

            filePathCallback
                ?.onReceiveValue(
                    resultado
                )

            filePathCallback = null

            return
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

        onBackPressedDispatcher
            .addCallback(
                this,
                object :
                    OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {

                        if (
                            webView.canGoBack()
                        ) {

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

        filePathCallback
            ?.onReceiveValue(null)

        filePathCallback = null

        if (
            ::webView.isInitialized
        ) {

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
