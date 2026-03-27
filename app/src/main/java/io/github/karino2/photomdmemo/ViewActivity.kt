package io.github.karino2.photomdmemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.InputStream

class ViewActivity : AppCompatActivity() {

    private val webView by lazy { findViewById<WebView>(R.id.webView) }
    private lateinit var parentDir: DocumentFile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val uri = intent.data ?: return
        val docFile = DocumentFile.fromSingleUri(this, uri) ?: return
        
        // We need the parent directory to find images. 
        // For SAF tree URIs, we can usually get the parent if we have the root.
        // For simplicity, let's assume images are in the same folder as the .md file.
        val lastUri = MainActivity.lastUri(this) ?: return
        parentDir = DocumentFile.fromTreeUri(this, lastUri) ?: return

        val content = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
        val html = renderMarkdown(content)

        setupWebView()
        webView.loadDataWithBaseURL("https://local.app/", html, "text/html", "utf-8", null)
    }

    private fun renderMarkdown(markdown: String): String {
        val parser = Parser.builder().build()
        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder().build()
        val body = renderer.render(document)
        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; padding: 16px; line-height: 1.6; }
                    img { max-width: 100%; height: auto; display: block; margin: 1em 0; cursor: pointer; }
                </style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                if (url.host == "local.app") {
                    val fileName = url.path?.removePrefix("/") ?: return false
                    val file = parentDir.findFile(fileName)
                    if (file != null) {
                        val photoIntent = Intent(this@ViewActivity, PhotoViewActivity::class.java).apply {
                            data = file.uri
                        }
                        startActivity(photoIntent)
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                if (url.host == "local.app") {
                    val fileName = url.path?.removePrefix("/") ?: return null
                    val file = parentDir.findFile(fileName)
                    if (file != null) {
                        val inputStream = contentResolver.openInputStream(file.uri)
                        val mimeType = contentResolver.getType(file.uri) ?: "image/jpeg"
                        return WebResourceResponse(mimeType, "UTF-8", inputStream)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Add click listener to images via JS
                view?.loadUrl("javascript:(function(){ " +
                        "var imgs = document.getElementsByTagName('img'); " +
                        "for (var i = 0; i < imgs.length; i++) {" +
                        "  imgs[i].onclick = function() {" +
                        "    window.location.href = this.src;" +
                        "  };" +
                        "}" +
                        "})()")
            }
        }
        webView.settings.javaScriptEnabled = true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
