package com.example.wificardscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cameraExecutor: ExecutorService
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scanQRCodeFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // التصميم الرئيسي مرتب في الأسفل مع تكبير العناصر وحقوقك
        val rootLayout = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#0b132b"))
            setPadding(16, 16, 16, 16)
        }

        // منطقة عرض الويب (الشبكة) بحجم كبير ومريح
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
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        buttonsLayout1.addView(btnScanQr)

        val btnScanGallery = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "صورة من المعرض"
            textSize = 14f
            setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
        buttonsLayout1.addView(btnScanGallery)
        rootLayout.addView(buttonsLayout1)

        // الصف الثاني من الأزرار في الأسفل (يشمل زر المشاركة وزر صفحة الشبكة)
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

        // حقوق التصميم والإعداد في الأسفل باسمك ورقمك
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

    private fun startCamera() {
        // تنفيذ تشغيل الكاميرا ومسح الباركود
    }

    private fun scanQRCodeFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            webView.loadUrl(qrValue)
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
