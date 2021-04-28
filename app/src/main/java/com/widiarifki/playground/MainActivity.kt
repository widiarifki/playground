package com.widiarifki.playground

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {

    lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storage = Firebase.storage
        reqPermission()
        setEvent()
    }

    private fun reqPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            1001
        )
    }

    private fun setEvent() {
        btnUpload.setOnClickListener {
            setupWorker()
        }
    }

    private fun saveFile(bitmap: Bitmap, fileName: String) {
        openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
    }

    private fun setupWorker() {
        // Local work
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val filename = System.currentTimeMillis().toString()
        saveFile(bitmap, filename)

        // workmanager - 0. (optional) assign param data
        val inputData = Data.Builder().apply {
            putString(UploadWorker.PARAM_FILENAME, filename)
        }.build()

        // workmanager - 0. (optional) set constraint
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // workmanager - 1. create OneTimeWorkRequestBuilder (that will only execute once).
        //val uploadWorkRequest = OneTimeWorkRequest.from(UploadWorker::class.java)
        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        // workmanager - 2. enqueue the workrequest
        WorkManager.getInstance(applicationContext).enqueue(uploadWorkRequest)
    }
}