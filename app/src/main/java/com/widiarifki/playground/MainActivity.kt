package com.widiarifki.playground

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
            if (isConnected() == true) {
                immediateUpload()
            } else {
                setupUploadWorker()
            }
        }
    }

    private fun setupUploadWorker() {
        // Local work
        val filename = getBitmapFilename()

        createNotification("Upload pending untuk file ${filename}")

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

    private fun immediateUpload() {
        val filename = getBitmapFilename()

        val firebaseStorage = Firebase.storage
        val storageRef = firebaseStorage.reference
        val fileRef = storageRef.child("$filename.jpg") // image type masih hardcode
        val fileData = applicationContext.openFileInput(filename).readBytes()

        fileRef.putBytes(fileData)
            .addOnFailureListener {
                setupUploadWorker()
            }
            .addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                createNotification("File ${taskSnapshot.metadata?.name} berhasil diunggah")
            }
    }

    private fun getBitmapFilename(): String? {
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val filename = System.currentTimeMillis().toString()
        saveFile(bitmap, filename)
        return filename
    }

    private fun saveFile(bitmap: Bitmap, fileName: String) {
        openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
    }

    private fun createNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val builder = NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kabar upload")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        createNotificationChannel()
        notificationManager?.notify(0, builder.build())
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = BuildConfig.APPLICATION_ID
            val descriptionText = BuildConfig.APPLICATION_ID
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(BuildConfig.APPLICATION_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isConnected() : Boolean? {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager?.activeNetworkInfo?.isConnected
    }
}