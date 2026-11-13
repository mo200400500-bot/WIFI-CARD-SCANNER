package com.example.wificardscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // التصميم الرئيسي للشاشة بلون خلفية متناسق ومريح
        val rootLayout = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#0b132b"))
            setPadding(16, 16, 16, 16)
        }

        // منطقة عرض الويب (صفحة الشبكة) بحجم كبير ومريح تملأ المساحة
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = 12
            }
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("http://l.net/login")
        }
        rootLayout.addView(webView)

        // الصف الأول من الأزرار في الأسفل
        val buttonsLayout1 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2
            setPadding(0, 0, 0, 8)
        }

        val btnScanQr = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6
            }
            text = "مسح QR بالكاميرا"
            textSize = 14f
            setOnClickListener {
                // فتح صفحة الشبكة أو تسجيل الدخول عند الضغط
                webView.loadUrl("http://l.net/login")
            }
        }
        buttonsLayout1.addView(btnScanQr)

        val btnScanGallery = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "صورة من المعرض"
            textSize = 14f
            setOnClickListener {
                webView.loadUrl("http://l.net/login")
            }
        }
        buttonsLayout1.addView(btnScanGallery)
        rootLayout.addView(buttonsLayout1)

        // الصف الثاني من الأزرار في الأسفل (مشاركة التطبيق وصفحة الشبكة)
        val buttonsLayout2 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2
            setPadding(0, 0, 0, 12)
        }

        val btnShare = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6
            }
            text = "مشاركة التطبيق"
            textSize = 14f
            setOnClickListener {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "تطبيق ماسح بطاقات ستار نت - إعداد وتصميم: محمد الجنوبي (771610510)")
                }
                startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"))
            }
        }
        buttonsLayout2.addView(btnShare)

        val btnNetworkPage = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "صفحة الشبكة"
            textSize = 14f
            setOnClickListener {
                webView.loadUrl("http://l.net/login")
            }
        }
        buttonsLayout2.addView(btnNetworkPage)
        rootLayout.addView(buttonsLayout2)

        // حقوق التصميم والإعداد في الأسفل باسمك ورقمك بوضوح تام
        val creditText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "إعداد وتصميم / محمد الجنوبي 771610510"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
        }
        rootLayout.addView(creditText)

        setContentView(rootLayout)
    }
}
