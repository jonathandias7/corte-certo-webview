package com.cortecertoagendamentos.app

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // 🔥 ESSENCIAL para evitar bloqueios
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // 🔥 melhora performance
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 🔥 evita abrir navegador externo
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            // 🔥 ignora erros SSL (importante pra não travar)
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
                handler.proceed()
            }

            // 🔥 evita tela branca em erro
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                view?.loadData(
                    "<h2>Sem conexão</h2><p>Verifique sua internet.</p>",
                    "text/html",
                    "UTF-8"
                )
            }
        }

        webView.webChromeClient = WebChromeClient()

        // 🔥 URL principal
        webView.loadUrl("https://cortecertoagendamentos.com.br/")
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // 🔥 evita vazamento de memória
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
