package com.mollatsystem.mollat

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.print.PrintManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var refresh: SwipeRefreshLayout
    private var chooser: ValueCallback<Array<Uri>>? = null
    private val appUrl = "https://mollatsystem.github.io/-/"
    private val trustedHosts = listOf("mollatsystem.github.io", "znomldkvmhbvvbzqldmi.supabase.co", "cdn.jsdelivr.net", "fonts.googleapis.com", "fonts.gstatic.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16,39,68)
        window.navigationBarColor = Color.rgb(16,39,68)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(237,242,248)) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16),0,dp(12),0); setBackgroundColor(Color.rgb(16,39,68)) }
        val title = TextView(this).apply { text = "سیستەمی مۆڵەت"; textSize=17f; setTextColor(Color.WHITE); typeface=android.graphics.Typeface.DEFAULT_BOLD }
        bar.addView(title, LinearLayout.LayoutParams(0,dp(58),1f))
        val reload = TextView(this).apply { text="⟳"; textSize=29f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setOnClickListener { webView.reload() } }
        bar.addView(reload, LinearLayout.LayoutParams(dp(48),dp(58)))
        root.addView(bar)
        refresh = SwipeRefreshLayout(this).apply { setColorSchemeColors(Color.rgb(8,126,139),Color.rgb(40,85,143)) }
        webView = WebView(this)
        refresh.addView(webView, ViewGroup.LayoutParams(-1,-1))
        refresh.setOnRefreshListener { webView.reload() }
        root.addView(refresh, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        configureWebView()
        if (savedInstanceState == null) webView.loadUrl(appUrl) else webView.restoreState(savedInstanceState)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true)
        webView.setBackgroundColor(Color.rgb(237,242,248))
        webView.isVerticalScrollBarEnabled=false
        webView.settings.apply {
            javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true
            loadsImagesAutomatically=true; javaScriptCanOpenWindowsAutomatically=true
            setSupportMultipleWindows(false); cacheMode=WebSettings.LOAD_DEFAULT
            mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess=false; allowContentAccess=true
            builtInZoomControls=false; displayZoomControls=false
        }
        webView.addJavascriptInterface(object {
            @JavascriptInterface fun printPage() { runOnUiThread {
                try {
                    val pm=getSystemService(Context.PRINT_SERVICE) as PrintManager
                    pm.print("فۆرمی سیستەمی مۆڵەت",webView.createPrintDocumentAdapter("فۆرمی مۆڵەت"),null)
                } catch (_:Exception) { Toast.makeText(this@MainActivity,"چاپکردن بەردەست نییە.",Toast.LENGTH_LONG).show() }
            } }
        },"AndroidBridge")
        webView.webChromeClient=object:WebChromeClient(){
            override fun onShowFileChooser(view:WebView?, callback:ValueCallback<Array<Uri>>?, params:FileChooserParams?):Boolean {
                chooser?.onReceiveValue(null); chooser=callback
                return try {
                    val i=Intent(Intent.ACTION_GET_CONTENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" }
                    startActivityForResult(Intent.createChooser(i,"فایل هەڵبژێرە"),4102); true
                } catch (_:Exception) { chooser?.onReceiveValue(null); chooser=null; false }
            }
        }
        webView.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(view:WebView?, request:WebResourceRequest?):Boolean {
                val u=request?.url ?: return false
                val h=u.host.orEmpty()
                if(u.scheme=="https" && trustedHosts.any { h==it || h.endsWith(".$it") }) return false
                if(u.scheme=="https" || u.scheme=="http") {
                    return try { startActivity(Intent(Intent.ACTION_VIEW,u)); true } catch (_:Exception) { true }
                }
                return true
            }
            override fun onPageFinished(view:WebView?, url:String?) {
                super.onPageFinished(view,url); refresh.isRefreshing=false; injectMobileDesign()
            }
            override fun onReceivedError(view:WebView?, request:WebResourceRequest?, error:WebResourceError?) {
                super.onReceivedError(view,request,error)
                if(request?.isForMainFrame==true) { refresh.isRefreshing=false; Toast.makeText(this@MainActivity,"پەیوەندی بە سیستەمەکەوە پچڕاوە؛ دووبارە هەوڵ بدەرەوە.",Toast.LENGTH_LONG).show() }
            }
        }
        webView.setDownloadListener { url, agent, disposition, mime, _ ->
            try {
                val name=URLUtil.guessFileName(url,disposition,mime)
                val req=DownloadManager.Request(Uri.parse(url))
                    .addRequestHeader("Cookie",CookieManager.getInstance().getCookie(url).orEmpty())
                    .addRequestHeader("User-Agent",agent).setMimeType(mime).setTitle(name)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name)
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                Toast.makeText(this,"داگرتن دەستی پێکرد.",Toast.LENGTH_SHORT).show()
            } catch (_:Exception) { Toast.makeText(this,"داگرتنی فایل سەرکەوتوو نەبوو.",Toast.LENGTH_LONG).show() }
        }
    }

    private fun injectMobileDesign() {
        val css=""" 
            html,body{width:100%!important;min-width:0!important;background:#edf2f8!important}
            body{font-family:system-ui,'Noto Sans Arabic',sans-serif!important}
            .wrap{width:100%!important;max-width:none!important;padding:12px!important}
            .top{border-radius:18px!important;padding:17px!important;background:linear-gradient(135deg,#102744,#1d5474 72%,#087e8b)!important;box-shadow:0 8px 24px rgba(16,39,68,.16)!important}
            .top h1{font-size:17px!important;line-height:1.7!important}
            .layout{display:flex!important;flex-direction:column!important;gap:12px!important;margin-top:12px!important}
            .nav{display:flex!important;flex-direction:row!important;overflow-x:auto!important;gap:6px!important;padding:8px!important;border-radius:14px!important;position:sticky!important;top:0!important;z-index:20!important;background:#142f4c!important;scrollbar-width:none!important}
            .nav button{flex:0 0 auto!important;width:auto!important;white-space:nowrap!important;min-height:42px!important;padding:9px 13px!important;border-radius:10px!important;color:#eaf2fb!important;background:transparent!important;font-size:13px!important}
            .nav button.active{background:#d9edf4!important;color:#123d5c!important}
            .page.active{padding:8px!important;border-radius:15px!important}
            .panel,.stat,.summary-group{border-radius:14px!important;padding:13px!important}
            .panel h2{font-size:16px!important;line-height:1.7!important}
            .stats{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:8px!important}
            .summary-groups{grid-template-columns:1fr!important;gap:9px!important}
            .summary-metrics{grid-template-columns:repeat(2,minmax(0,1fr))!important}
            .grid{grid-template-columns:1fr!important;gap:10px!important}
            .field{min-width:0!important}
            .field input,.field select,.field textarea,.search{min-height:45px!important;border-radius:10px!important;font-size:15px!important}
            .btn,button{min-height:42px;border-radius:10px!important}
            .actions{display:flex!important;flex-wrap:wrap!important;gap:8px!important}
            .actions .btn{flex:1 1 auto!important}
            .tablewrap{max-width:100%!important;overflow-x:auto!important;border-radius:11px!important}
            table{min-width:620px!important;font-size:12px!important}
            .login{width:calc(100% - 22px)!important;max-width:480px!important;margin:24px auto!important;padding:18px!important;border-radius:18px!important}
            .institution-heading{min-height:0!important;padding:14px!important;border-radius:0 0 16px 16px!important}
            body:has(#login:not(.hidden)) #login.login,body:has(#recovery:not(.hidden)) #recovery.login{width:calc(100% - 24px)!important;min-height:0!important;margin:18px auto!important;padding:20px!important;border-radius:18px!important;box-shadow:0 10px 28px rgba(20,45,75,.13)!important}
            input,select,textarea{max-width:100%!important}
            @media(min-width:700px){.layout{display:grid!important;grid-template-columns:190px minmax(0,1fr)!important}.nav{flex-direction:column!important;position:static!important}.grid{grid-template-columns:repeat(2,minmax(0,1fr))!important}}
        """.trimIndent()
        val js="""(function(){var id='mollat-android-mobile-style';var s=document.getElementById(id);if(!s){s=document.createElement('style');s.id=id;document.head.appendChild(s);}s.textContent=${JSONObject.quote(css)};try{if(window.AndroidBridge)window.print=function(){window.AndroidBridge.printPage();};}catch(e){}})();"""
        webView.evaluateJavascript(js,null)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==4102){val result=if(resultCode==RESULT_OK)data?.data?.let{arrayOf(it)}else null;chooser?.onReceiveValue(result);chooser=null}
    }
    override fun onSaveInstanceState(outState:Bundle){webView.saveState(outState);super.onSaveInstanceState(outState)}
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed(){if(::webView.isInitialized&&webView.canGoBack())webView.goBack() else super.onBackPressed()}
    override fun onDestroy(){if(::webView.isInitialized){webView.removeJavascriptInterface("AndroidBridge");webView.destroy()};super.onDestroy()}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}
