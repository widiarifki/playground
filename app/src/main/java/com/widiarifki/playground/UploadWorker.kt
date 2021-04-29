package com.widiarifki.playground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.runBlocking

class UploadWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val filename = inputData.getString(PARAM_FILENAME)

        filename?.let {
            val firebaseStorage = Firebase.storage
            val storageRef = firebaseStorage.reference
            val fileRef = storageRef.child("$filename.jpg") // image type masih hardcode
            val fileData = applicationContext.openFileInput(filename).readBytes()

            runBlocking {
                fileRef.putBytes(fileData)
                    .addOnFailureListener {
                        createNotification("File gagal diunggah: ${it.message}")
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                        createNotification("File ${taskSnapshot.metadata?.name} berhasil diunggah")
                    }
            }

            return Result.success()
        } ?: run {
            return Result.failure()
        }
    }

    private fun createNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val builder = NotificationCompat.Builder(applicationContext, BuildConfig.APPLICATION_ID)
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
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PARAM_FILENAME = "filename"
    }
}