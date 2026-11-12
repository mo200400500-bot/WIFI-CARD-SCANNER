
package com.example.wificardscanner

import android.Manifest
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
                status.text =
                    "يلزم السماح للكاميرا لاستخدام المسح المباشر."
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
            setPadding(18, 18, 18, 12)
            setBackgroundColor(0xFFF4F7F6.toInt())
        }

        val heading = TextView(this).apply {
            text = "ماسح بطاقات الواي فاي"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(0xFF123D36.toInt())
            setPadding(4, 12, 4, 16)
        }

        root.addView(
            heading,
            LinearLayout.LayoutParams(-1, -2)
        )

        status = TextView(this).apply {
            text = "جاهز. امسح QR أو اختر صورة من المعرض."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF263238.toInt())
            setPadding(8, 8, 8, 12)
        }

        root.addView(
            status,
            LinearLayout.LayoutParams(-1, -2)
        )

        addButton("تشغيل الكاميرا والمسح المباشر") {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startLiveScanner()
            } else {
                cameraPermission.launch(
                    Manifest.permission.CAMERA
                )
            }
        }

        addButton("اختيار صورة QR من المعرض") {
            pickImage.launch("image/*")
        }

        addButton("فتح صفحة شبكة الإنترنت") {
            showWebPage()
        }

        addButton("إيقاف الكاميرا والعودة") {
            stopScannerAndShowPage()
        }

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
                        fillUsername(it)
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

    private fun addButton(
        label: String,
        action: () -> Unit
    ) {

        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener {
                action()
            }
        }

        root.addView(
            button,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = 5
            }
        )
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

        status.text =
            "وجّه الكاميرا إلى رمز QR الذي يحتوي على اسم المستخدم."

        val providerFuture =
            ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({

            val provider = providerFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        previewView.surfaceProvider
                    )
                }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()

            val barcodeScanner =
                BarcodeScanning.getClient()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->

                val mediaImage = imageProxy.image

                if (
                    mediaImage == null ||
                    !processing.compareAndSet(false, true)
                ) {
                    imageProxy.close()
                } else {

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            val raw = barcodes
                                .firstOrNull {
                                    !it.rawValue.isNullOrBlank()
                                }?.rawValue

                            val username =
                                raw?.let { extractUsername(it) }

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

                status.text =
                    "تعذر تشغيل الكاميرا: " +
                    (e.localizedMessage ?: "خطأ غير معروف")
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
                        .firstOrNull {
                            !it.rawValue.isNullOrBlank()
                        }?.rawValue

                    val username =
                        raw?.let { extractUsername(it) }

                    if (!username.isNullOrBlank()) {
                        onUsernameRead(username)
                    } else {
                        status.text =
                            "لم يتم العثور على رمز QR صالح في الصورة."
                    }
                }
                .addOnFailureListener {
                    status.text =
                        "تعذر قراءة الصورة: " +
                        (it.localizedMessage ?: "خطأ")
                }

        } catch (e: Exception) {

            status.text =
                "تعذر فتح الصورة المحددة."
        }
    }

    private fun onUsernameRead(username: String) {

        if (
            lastUsername == username &&
            !scannerVisible
        ) return

        lastUsername = username

        stopCameraOnly()
        showWebPage()

        status.text =
            "تمت قراءة اسم المستخدم: $username — جارٍ إدخاله في صفحة الدخول."

        if (pageLoaded) {
            fillUsername(username)
        }
    }

    private fun fillUsername(username: String) {

        val safe = org.json.JSONObject.quote(username)

        val js = """
            (function(){
                var el =
                    document.getElementById('myInput') ||
                    document.querySelector(
                        'input[name=username],input[type=text]'
                    );

                if(el){
                    el.value=$safe;

                    el.dispatchEvent(
                        new Event('input',{bubbles:true})
                    );

                    el.dispatchEvent(
                        new Event('change',{bubbles:true})
                    );

                    return 'ok';
                }

                return 'field-not-found';
            })()
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->

            if (result?.contains("ok") == true) {

                status.text =
                    "تم إدخال اسم المستخدم: $username. راجع الصفحة واضغط دخول بنفسك."

            } else {

                status.text =
                    "تمت قراءة $username، لكن لم نعثر على خانة اسم المستخدم. تحقق من صفحة الدخول."
            }
        }
    }

    private fun stopCameraOnly() {

        try {
            ProcessCameraProvider
                .getInstance(this)
                .get()
                .unbindAll()
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
            ProcessCameraProvider
                .getInstance(this)
                .get()
                .unbindAll()
        } catch (_: Exception) {
        }

        cameraExecutor.shutdown()
        webView.destroy()
    }
}
