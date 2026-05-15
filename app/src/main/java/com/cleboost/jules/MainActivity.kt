package com.cleboost.jules

import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.cleboost.jules.ui.theme.JulesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        
        WebView.setWebContentsDebuggingEnabled(true)
        
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            JulesTheme(darkTheme = isDarkTheme) {
                var webView by remember { mutableStateOf<WebView?>(null) }
                var canGoBack by remember { mutableStateOf(false) }
                var progress by remember { mutableStateOf(0) }
                
                BackHandler(enabled = canGoBack) {
                    webView?.goBack()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        WebViewScreen(
                            url = "https://jules.google.com/session",
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize(),
                            onWebViewCreated = { webView = it },
                            onProgressChanged = { progress = it },
                            onCanGoBackChanged = { canGoBack = it }
                        )
                        
                        if (progress < 100) {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = Color(0xFF4285F4),
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isDarkTheme, webViewInstance) {
        webViewInstance?.evaluateJavascript("if (window.syncTheme) window.syncTheme($isDarkTheme);", null)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = false
                settings.databaseEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                val originalUserAgent = settings.userAgentString
                settings.userAgentString = "$originalUserAgent JulesApp/1.0"

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onCanGoBackChanged(view?.canGoBack() == true)
                        CookieManager.getInstance().flush()
                        
                        injectBaseAssets(context, view)
                        handleConditionalInjection(context, view, url)
                    }
                    
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        onCanGoBackChanged(view?.canGoBack() == true)
                        handleConditionalInjection(context, view, url)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false 
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                        super.onReceivedSslError(view, handler, error)
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                    
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        return true
                    }
                }
                
                onWebViewCreated(this)
                webViewInstance = this
                overScrollMode = View.OVER_SCROLL_NEVER
                setBackgroundColor(0)
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}

private fun injectBaseAssets(context: Context, webView: WebView?) {
    try {
        val css = context.assets.open("jules-inject.css").bufferedReader().use { it.readText() }
        val js = context.assets.open("jules-inject.js").bufferedReader().use { it.readText() }
        
        val encodedCss = Base64.encodeToString(css.toByteArray(), Base64.NO_WRAP)
        
        val injectionJs = """
            (function() {
                if (!document.getElementById('jules-mobile-styles')) {
                    const style = document.createElement('style');
                    style.id = 'jules-mobile-styles';
                    style.textContent = atob('$encodedCss');
                    document.head.append(style);
                }
                $js
                if (window.syncTheme) window.syncTheme(${isSystemInDarkThemeStatic(context)});
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(injectionJs, null)
    } catch (e: Exception) {
        Log.e("Jules", "Error injecting base assets", e)
    }
}

private fun handleConditionalInjection(context: Context, webView: WebView?, url: String?) {
    if (url == null) return
    
    val isTaskPage = url.contains("/session/") && (url.contains("/code/") || url.contains("/task/"))
    val styleId = "jules-task-page-styles"
    
    if (isTaskPage) {
        try {
            val css = context.assets.open("jules-task-page.css").bufferedReader().use { it.readText() }
            val encodedCss = Base64.encodeToString(css.toByteArray(), Base64.NO_WRAP)
            val js = """
                (function() {
                    if (!document.getElementById('$styleId')) {
                        const style = document.createElement('style');
                        style.id = '$styleId';
                        style.textContent = atob('$encodedCss');
                        document.head.append(style);
                    }
                })();
            """.trimIndent()
            webView?.evaluateJavascript(js, null)
        } catch (e: Exception) {
            Log.e("Jules", "Error injecting task page CSS", e)
        }
    } else {
        val js = "if (document.getElementById('$styleId')) document.getElementById('$styleId').remove();"
        webView?.evaluateJavascript(js, null)
    }
}

private fun isSystemInDarkThemeStatic(context: Context): Boolean {
    return (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
}

@Preview(showBackground = true)
@Composable
fun WebViewPreview() {
    JulesTheme {
        Text("WebView Preview")
    }
}
