package com.khalidsultan.visionowl

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.khalidsultan.cataracts.Constants
import com.khalidsultan.cataracts.Utils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    var pickImage = 100
    var classifier: Classifier? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var lastImageTaken: Uri? = null


    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigationHowItWorks -> {
                    val dialog = Dialog(this, R.style.df_dialog)
                    dialog.setContentView(R.layout.results_dialog)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.findViewById<ImageView>(R.id.result_image).setImageDrawable(resources.getDrawable(R.drawable.ic_undraw_artificial_intelligence_re_enpp))
                    dialog.findViewById<TextView>(R.id.prediction).text = "How it works"
                    dialog.findViewById<TextView>(R.id.percentages).text = "Pay up then"
                    dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
                        dialog.dismiss()
                    }
                    dialog.show()

                }
                R.id.navigationCreator -> {
                    val dialog = Dialog(this, R.style.df_dialog)
                    dialog.setContentView(R.layout.results_dialog)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.findViewById<ImageView>(R.id.result_image).setImageDrawable(resources.getDrawable(R.drawable.ic_undraw_animating_1rgh))
                    dialog.findViewById<TextView>(R.id.prediction).text = "About Us"
                    dialog.findViewById<TextView>(R.id.percentages).text = "It's just me sadly"
                    dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
                        dialog.dismiss()
                    }
                    dialog.show()

                }
                R.id.navigationHome -> return@OnNavigationItemSelectedListener true
            }
            false
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(REQUEST_CODE_PERMISSIONS, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = this.resources.getColor(R.color.contentStatusBar)
        }
        setContentView(R.layout.activity_main)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        startCamera()

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

//        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
//        layoutParams.behavior = BottomNavigationBehavior()
        bottomNavigationView.selectedItemId = R.id.navigationHome

        val modelPath = Utils.assetFilePath(this, "model.pt")
        classifier = Classifier(modelPath)
        findViewById<View>(R.id.browse).setOnClickListener {
            intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_PICK
            startActivityForResult(Intent.createChooser(intent, null), pickImage)
        }
        findViewById<View>(R.id.iv_capture).setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = savedUri
                    sendBroadcast(mediaScanIntent)
                    lastImageTaken = savedUri
                    generateResults(BitmapFactory.decodeFile(savedUri.path))
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = Environment.getExternalStorageDirectory().let {
            File(it, "DCIM/Vision_Owl/").apply { mkdirs() }
        }
        return if (mediaDir.exists())
            mediaDir else filesDir
    }

    private fun generateResults(bitmap: Bitmap) {
        val percentage = classifier!!.predict(bitmap)
        var prediction = Constants.PT_CLASSES[1]
        if (percentage < 0.5) {
            prediction = Constants.PT_CLASSES[0]
        }

        val dialog = Dialog(this, R.style.df_dialog)
        dialog.setContentView(R.layout.results_dialog)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.findViewById<ImageView>(R.id.result_image).setImageBitmap(bitmap)
        dialog.findViewById<TextView>(R.id.prediction).text = prediction
        dialog.findViewById<TextView>(R.id.percentages).text = percentage.toString()
        dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val uri = data.data!!
                val inputStream = this.contentResolver.openInputStream(uri)
                val cursor = this.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst()) {
                        val name = c.getString(nameIndex)
                        inputStream?.let { inputStream ->
                            // create same file with same name
                            val file = File(this.cacheDir, name)
                            val os = file.outputStream()
                            os.use {
                                inputStream.copyTo(it)
                            }
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            generateResults(bitmap)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val MODE_DARK = 0
        private const val MODE_LIGHT = 1
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )
    }
}
