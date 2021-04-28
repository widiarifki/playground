package com.widiarifki.playground

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.runBlocking

class UploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

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
                        // Handle unsuccessful uploads
                        Log.v("uploadTask", it.message.toString())
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                        // ...
                        Log.v("uploadTask", "success ${taskSnapshot.metadata?.name} " +
                                "${taskSnapshot.metadata?.path}")
                    }
            }

            return Result.success()
        } ?: run {
            return Result.failure()
        }
    }

    companion object {
        const val PARAM_FILENAME = "filename"
    }
}