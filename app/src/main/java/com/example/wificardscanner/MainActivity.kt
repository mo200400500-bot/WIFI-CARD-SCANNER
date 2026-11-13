package com.example.wificardscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var cameraArea: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var webView: WebView
    private lateinit var cameraExecutor: ExecutorService

    private val processing = AtomicBoolean(false)

    private var scannerVisible = false
    private var pageLoaded = false
    private var lastUsername: String? = null

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startLiveScanner()
            } else {
                status.text = "يلزم السماح للكاميرا لاستخدام المسح المباشر."
            }
        }

    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) scanGalleryImage(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        buildScreen()
        openHotspotPage()
    }

    private fun buildScreen() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 8)
            setBackgroundColor(0xFFF4F7F6.toInt())
        }

        val heading = TextView(this).apply {
            text = "ماسح بطاقات الواي فاي - ستار نت"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFF123D36.toInt())
            setPadding(2, 4, 2, 6)
        }

        root.addView(
            heading,
            LinearLayout.LayoutParams(-1, -2)
        )

        status = TextView(this).apply {
            text = "جاهز. امسح QR أو اختر صورة من المعرض."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF263238.toInt())
            setPadding(4, 2, 4, 8)
        }

        root.addView(
            status,
            LinearLayout.LayoutParams(-1, -2)
        )

        // صف الأزرار الأول (المسح المباشر + اختيار صورة) بحجم مدمج
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }
        row1.addView(createCompactButton("مسح QR بالكاميرا") {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startLiveScanner()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        })
        row1.addView(createCompactButton("صورة من المعرض") {
            pickImage.launch("image/*")
        })
        root.addView(row1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 4 })

        // صف الأزرار الثاني (صفحة الشبكة + إيقاف الكاميرا + مشاركة التطبيق) بحجم مدمج
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }
        row2.addView(createCompactButton("صفحة الشبكة") {
            showWebPage()
        })
        row2.addView(createCompactButton("إيقاف الكاميرا") {
            stopScannerAndShowPage()
        })
        row2.addView(createCompactButton("مشاركة التطبيق") {
            shareAppWithClients()
        })
        root.addView(row2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })

        cameraArea = FrameLayout(this).apply {
            visibility = View.GONE
        }

        previewView = PreviewView(this)

        cameraArea.addView(
            previewView,
            FrameLayout.LayoutParams(-1, -1)
        )

        root.addView(
            cameraArea,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        webView = WebView(this).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false

            webViewClient = object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    pageLoaded = true
                    lastUsername?.let {
                        fillAndSubmitUsername(it)
                    }
                }
            }
        }

        root.addView(
            webView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
        showWebPage()
    }

    private fun createCompactButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setPadding(6, 4, 6, 4)
            minHeight = 0
            minimumHeight = 0
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(3, 0, 3, 0)
            }
            setOnClickListener {
                action()
            }
        }
    }

    private fun shareAppWithClients() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تطبيق ماسح بطاقات الواي فاي - ستار نت")
            putExtra(
                Intent.EXTRA_TEXT,
                "حمّل تطبيق شبكة ستار نت لتسجيل الدخول السريع لبطاقات الواي فاي بمسح الباركود مباشرة:\n" +
                "https://github.com/mo200400500-bot/WIFI-CARD-SCANNER/releases"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق مع عملاء الشبكة عبر"))
    }

    private fun openHotspotPage() {
        webView.loadUrl("http://l.net/index.html")
    }

    private fun showWebPage() {
        scannerVisible = false
        cameraArea.visibility = View.GONE
        webView.visibility = View.VISIBLE

        status.text =
            lastUsername?.let {
                "اسم المستخدم المقروء: $it"
            } ?: "صفحة شبكة الإنترنت جاهزة."
    }

    private fun startLiveScanner() {
        scannerVisible = true

        webView.visibility = View.GONE
        cameraArea.visibility = View.VISIBLE

        status.text = "وجّه الكاميرا إلى رمز QR الخاص بالكرت..."

        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({

            val provider = providerFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val barcodeScanner = BarcodeScanning.getClient()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->

                val mediaImage = imageProxy.image

                if (mediaImage == null || !processing.compareAndSet(false, true)) {
                    imageProxy.close()
                } else {

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            val raw = barcodes
                                .firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue

                            val username = raw?.let { extractUsername(it) }

                            if (!username.isNullOrBlank()) {
                                runOnUiThread {
                                    onUsernameRead(username)
                                }
                            }
                        }
                        .addOnCompleteListener {
                            processing.set(false)
                            imageProxy.close()
                        }
                }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                status.text = "تعذر تشغيل الكاميرا: " + (e.localizedMessage ?: "خطأ غير معروف")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun extractUsername(raw: String): String {
        val value = raw.trim()
        return value
            .lineSequence()
            .firstOrNull()
            ?.trim()
            .orEmpty()
    }

    private fun scanGalleryImage(uri: Uri) {
        status.text = "جارٍ فحص الصورة..."

        try {
            val image = InputImage.fromFilePath(this, uri)

            BarcodeScanning.getClient()
                .process(image)
                .addOnSuccessListener { barcodes ->

                    val raw = barcodes
                        .firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue

                    val username = raw?.let { extractUsername(it) }

                    if (!username.isNullOrBlank()) {
                        onUsernameRead(username)
                    } else {
                        status.text = "لم يتم العثور على رمز QR صالح في الصورة."
                    }
                }
                .addOnFailureListener {
                    status.text = "تعذر قراءة الصورة: " + (it.localizedMessage ?: "خطأ")
                }

        } catch (e: Exception) {
            status.text = "تعذر فتح الصورة المحددة."
        }
    }

    private fun onUsernameRead(username: String) {
        if (lastUsername == username && !scannerVisible) return

        lastUsername = username

        stopCameraOnly()
        showWebPage()

        status.text = "تمت قراءة الكرت: $username — جارٍ تسجيل الدخول تلقائياً..."

        if (pageLoaded) {
            fillAndSubmitUsername(username)
        }
    }

    private fun fillAndSubmitUsername(username: String) {
        val safe = org.json.JSONObject.quote(username)

        val js = """
            (function(){
                var el =
                    document.getElementById('myInput') ||
                    document.querySelector('input[name=username],input[type=text]');

                if(el){
                    el.value=$safe;
                    el.dispatchEvent(new Event('input',{bubbles:true}));
                    el.dispatchEvent(new Event('change',{bubbles:true}));

                    // البحث عن زر الدخول أو النموذج لتسجيل الدخول تلقائياً فوراً
                    var form = el.closest('form');
                    var submitBtn = document.querySelector('button[type=submit], input[type=submit], button');
                    
                    if (submitBtn) {
                        submitBtn.click();
                        return 'submitted-btn';
                    } else if (form) {
                        form.submit();
                        return 'submitted-form';
                    }
                    return 'filled-only';
                }

                return 'field-not-found';
            })()
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            when {
                result?.contains("submitted") == true -> {
                    status.text = "تم إدخال الكرت ($username) وتسجيل الدخول مباشرة!"
                }
                result?.contains("filled-only") == true -> {
                    status.text = "تم إدخال الكرت ($username). اضغط على زر الدخول."
                }
                else -> {
                    status.text = "تمت قراءة ($username)، تحقق من ظهور خانة الدخول في الصفحة."
                }
            }
        }
    }

    private fun stopCameraOnly() {
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (_: Exception) {
        }

        scannerVisible = false
        cameraArea.visibility = View.GONE
    }

    private fun stopScannerAndShowPage() {
        stopCameraOnly()
        showWebPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (_: Exception) {
        }
        cameraExecutor.shutdown()
        webView.destroy()
    }
}
