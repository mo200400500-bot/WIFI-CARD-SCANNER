package com.example.wificardscanner

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(this, "تم اختيار الصورة بنجاح", Toast.LENGTH_SHORT).show()
            webView.loadUrl("http://l.net/login")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this)
        rootLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setBackgroundColor(Color.parseColor("#0b132b"))
        rootLayout.setPadding(16, 16, 16, 16)

        webView = WebView(this)
        val webParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        )
        webParams.bottomMargin = 12
        webView.layoutParams = webParams
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("http://l.net/login")
        rootLayout.addView(webView)

        val buttonsLayout1 = LinearLayout(this)
        buttonsLayout1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonsLayout1.orientation = LinearLayout.HORIZONTAL
        buttonsLayout1.weightSum = 2f
        buttonsLayout1.setPadding(0, 0, 0, 8)

        val btnScanQr = Button(this)
        val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        p1.marginEnd = 6
        btnScanQr.layoutParams = p1
        btnScanQr.text = "مسح QR بالكاميرا"
        btnScanQr.textSize = 14f
        btnScanQr.setOnClickListener {
            Toast.makeText(this, "جاري فتح صفحة الشبكة", Toast.LENGTH_SHORT).show()
            webView.loadUrl("http://l.net/login")
        }
        buttonsLayout1.addView(btnScanQr)

        val btnScanGallery = Button(this)
        btnScanGallery.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        btnScanGallery.text = "صورة من المعرض"
        btnScanGallery.textSize = 14f
        btnScanGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        buttonsLayout1.addView(btnScanGallery)
        rootLayout.addView(buttonsLayout1)

        val buttonsLayout2 = LinearLayout(this)
        buttonsLayout2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonsLayout2.orientation = LinearLayout.HORIZONTAL
        buttonsLayout2.weightSum = 2f
        buttonsLayout2.setPadding(0, 0, 0, 12)

        val btnShare = Button(this)
        val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        p2.marginEnd = 6
        btnShare.layoutParams = p2
        btnShare.text = "مشاركة التطبيق"
        btnShare.textSize = 14f
        btnShare.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "تطبيق ماسح بطاقات ستار نت - إعداد وتصميم: محمد الجنوبي (771610510)")
            startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"))
        }
        buttonsLayout2.addView(btnShare)

        val btnNetworkPage = Button(this)
        btnNetworkPage.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        btnNetworkPage.text = "صفحة الشبكة"
        btnNetworkPage.textSize = 14f
        btnNetworkPage.setOnClickListener {
            webView.loadUrl("http://l.net/login")
        }
        buttonsLayout2.addView(btnNetworkPage)
        rootLayout.addView(buttonsLayout2)

        val creditText = TextView(this)
        creditText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        creditText.text = "إعداد وتصميم / محمد الجنوبي 771610510"
        creditText.setTextColor(Color.WHITE)
        creditText.textSize = 13f
        creditText.gravity = Gravity.CENTER
        creditText.setPadding(0, 4, 0, 4)
        rootLayout.addView(creditText)

        setContentView(rootLayout)
    }
}
