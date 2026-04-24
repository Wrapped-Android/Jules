package com.cleboost.jules

import android.net.http.SslError
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            JulesTheme {
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
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit
) {
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

                        view?.evaluateJavascript("""
                            (function() {
                                if (window.JulesSwipeInitialized) return;
                                window.JulesSwipeInitialized = true;
                                
                                let startX = 0;
                                let startY = 0;
                                let lastActionTime = 0;
                                
                                window.addEventListener('touchstart', function(e) {
                                    startX = e.touches[0].clientX;
                                    startY = e.touches[0].clientY;
                                }, {passive: true, capture: true});
                                
                                window.addEventListener('touchend', function(e) {
                                    let now = Date.now();
                                    if (now - lastActionTime < 500) return;

                                    let endX = e.changedTouches[0].clientX;
                                    let endY = e.changedTouches[0].clientY;
                                    
                                    let dx = endX - startX;
                                    let dy = Math.abs(endY - startY);
                                    
                                    if (Math.abs(dx) > 80 && dy < 100) {
                                        let panel = document.getElementById('start-panel');
                                        let btn = document.querySelector('button.start-panel-button.is-left');
                                        
                                        if (!btn) return;

                                        let isClosed = !panel || panel.classList.contains('closed');
                                        
                                        if (dx > 0 && isClosed) {
                                            btn.click();
                                            lastActionTime = now;
                                        } else if (dx < 0 && !isClosed) {
                                            btn.click();
                                            lastActionTime = now;
                                        }
                                    }
                                }, {passive: true, capture: true});
                            })();
                        """.trimIndent(), null)
                    }
                    
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        onCanGoBackChanged(view?.canGoBack() == true)
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
                setBackgroundColor(0)
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WebViewPreview() {
    JulesTheme {
        Text("WebView Preview")
    }
}