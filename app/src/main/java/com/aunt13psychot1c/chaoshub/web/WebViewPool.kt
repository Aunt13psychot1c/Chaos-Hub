package com.aunt13psychot1c.chaoshub.web

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.aunt13psychot1c.chaoshub.HubState
import com.aunt13psychot1c.chaoshub.Notifier
import com.aunt13psychot1c.chaoshub.model.ChatService
import com.aunt13psychot1c.chaoshub.model.isInside
import com.aunt13psychot1c.chaoshub.model.parseUnread

/**
 * Every enabled tab's page, kept alive for the life of the process so switching tabs is instant
 * and pages in the background keep their connection (and their notifications) going.
 *
 * Pages are built on a [MutableContextWrapper] around the application. While the hub is on
 * screen the wrapper points at the Activity, which pop-ups like <select> lists need; when the
 * Activity goes away it points back at the application so nothing holds on to a dead screen.
 */
object WebViewPool {

    private const val TAG = "WebViewPool"
    private val views = LinkedHashMap<String, WebView>()

    /** Set by the Activity while it's on screen, so pages can ask for a file to upload. */
    var fileChooser: ((ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null

    fun get(context: Context, service: ChatService): WebView =
        views.getOrPut(service.id) { create(context.applicationContext, service) }

    fun peek(id: String?): WebView? = id?.let { views[it] }

    /** Opens every enabled tab so background tabs can notify before you've visited them. */
    fun warmUp(context: Context) {
        HubState.services.filter { it.enabled }.forEach { get(context, it) }
    }

    fun attachTo(host: Context) {
        views.values.forEach { (it.context as? MutableContextWrapper)?.baseContext = host }
    }

    fun detach(appContext: Context) {
        views.values.forEach { (it.context as? MutableContextWrapper)?.baseContext = appContext.applicationContext }
    }

    /** Closes pages for tabs that were switched off or removed. */
    fun prune(keep: Set<String>) {
        views.keys.filter { it !in keep }.forEach { id -> destroy(id) }
    }

    fun destroy(id: String) {
        views.remove(id)?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        HubState.unread.remove(id)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun create(appContext: Context, service: ChatService): WebView {
        val wv = WebView(MutableContextWrapper(appContext))
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            val default = userAgentString
            userAgentString = if (service.desktop) UserAgents.desktop(default) else UserAgents.mobile(default)
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.addJavascriptInterface(Bridge(appContext, service.id), "ChaosHub")
        val earlyShim = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (earlyShim) WebViewCompat.addDocumentStartJavaScript(wv, NOTIFICATION_SHIM, setOf("*"))

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    if (isInside(uri.host, service.domains)) return false
                    openOutside(view.context, uri)
                    return true
                }
                if (scheme == "about" || scheme == "blob" || scheme == "data") return false
                // whatsapp:, tg:, intent: and friends: hand to whatever app claims them.
                openOutside(view.context, uri)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                // A fresh page reports its count from scratch; don't treat that as "new".
                HubState.unread.remove(service.id)
                if (!earlyShim) view.evaluateJavascript(NOTIFICATION_SHIM, null)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String?) {
                val count = parseUnread(title)
                val before = HubState.unread[service.id]
                HubState.unread[service.id] = count
                if (before != null && count > before) Bridge.onCountRose(appContext, service.id, count)
                if (count == 0) Notifier.clear(appContext, service.id)
            }

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean = fileChooser?.invoke(callback, params) ?: false

            override fun onPermissionRequest(request: PermissionRequest) {
                // Camera and mic for calls come in a later version.
                request.deny()
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            download(wv.context, url, userAgent, contentDisposition, mimeType)
        }

        wv.loadUrl(service.url)
        return wv
    }

    private fun openOutside(context: Context, uri: Uri) {
        val intent = if (uri.scheme == "intent") {
            val parsed = try {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } catch (_: Exception) {
                return
            }
            // Only let a web page open what a normal browser link could.
            parsed.addCategory(Intent.CATEGORY_BROWSABLE)
            parsed.component = null
            parsed.selector = null
            parsed
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Log.i(TAG, "nothing on this phone opens $uri")
        }
    }

    private fun download(context: Context, url: String, userAgent: String, disposition: String?, mime: String?) {
        if (!URLUtil.isNetworkUrl(url)) {
            Toast.makeText(context, "That file can't be saved from here yet", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val name = URLUtil.guessFileName(url, disposition, mime)
            val request = DownloadManager.Request(Uri.parse(url))
                .setMimeType(mime)
                .addRequestHeader("User-Agent", userAgent)
                .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                .setTitle(name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            context.getSystemService(DownloadManager::class.java).enqueue(request)
            Toast.makeText(context, "Saving $name to Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(TAG, "download failed", e)
            Toast.makeText(context, "Couldn't save that file", Toast.LENGTH_SHORT).show()
        }
    }

    /** What the pages reach as window.ChaosHub. Called on a WebView background thread. */
    private class Bridge(private val appContext: Context, private val serviceId: String) {
        @JavascriptInterface
        fun notify(title: String, body: String) {
            main.post {
                lastDetailed[serviceId] = SystemClock.elapsedRealtime()
                if (HubState.isWatching(serviceId)) return@post
                val service = HubState.service(serviceId) ?: return@post
                Notifier.show(appContext, service, title.take(200), body.take(2000))
            }
        }

        companion object {
            private val main = Handler(Looper.getMainLooper())
            private val lastDetailed = HashMap<String, Long>()

            /**
             * Backup for sites that only update their title: say how many are waiting, unless
             * the site just sent a proper notification with the message itself.
             */
            fun onCountRose(appContext: Context, serviceId: String, count: Int) {
                if (HubState.isWatching(serviceId)) return
                val recent = lastDetailed[serviceId]?.let { SystemClock.elapsedRealtime() - it < 5_000 } ?: false
                if (recent) return
                val service = HubState.service(serviceId) ?: return
                Notifier.show(appContext, service, service.name,
                    if (count == 1) "1 unread message" else "$count unread messages")
            }
        }
    }
}
