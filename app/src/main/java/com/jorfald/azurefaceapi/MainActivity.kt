package com.jorfald.azurefaceapi

import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private val requestImageCapture = 123
    private val apiEndpoint = "https://northeurope.api.cognitive.microsoft.com/face/v1.0"
    private val subscriptionKey = "729b1716998045ce9af5d24722199146"
    private val faceServiceClient = FaceServiceRestClient(apiEndpoint, subscriptionKey)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scan_face_button.setOnClickListener {
            openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == requestImageCapture && resultCode == RESULT_OK && data != null) {
            val imageBitmap = data!!.extras.get("data") as Bitmap
            face_image_view.setImageBitmap(imageBitmap)
            detectAndFrame(imageBitmap)
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, requestImageCapture)
            }
        }
    }

    private fun detectAndFrame(imageBitmap: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val detectTask = FaceDetectionService(WeakReference(this), faceServiceClient)
        detectTask.execute(inputStream)
    }

    private class FaceDetectionService(
        val activity: WeakReference<MainActivity>,
        val faceServiceClient: FaceServiceClient
    ) : AsyncTask<InputStream, String, Array<Face>?>() {

        override fun doInBackground(vararg params: InputStream?): Array<Face>? {
            return try {
                faceServiceClient.detect(
                    params[0],
                    true,
                    true,
                    arrayOf(
                        FaceServiceClient.FaceAttributeType.Age,
                        FaceServiceClient.FaceAttributeType.Gender,
                        FaceServiceClient.FaceAttributeType.Emotion
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: Array<Face>?) {
            if (!result.isNullOrEmpty()) {
                var emotions = ""
                emotions += "\nAnger: " + result[0].faceAttributes.emotion.anger
                emotions += "\nContempt: " + result[0].faceAttributes.emotion.contempt
                emotions += "\nDisgust: " + result[0].faceAttributes.emotion.disgust
                emotions += "\nfear: " + result[0].faceAttributes.emotion.fear
                emotions += "\nhappiness: " + result[0].faceAttributes.emotion.happiness
                emotions += "\nneutral: " + result[0].faceAttributes.emotion.neutral
                emotions += "\nsadness: " + result[0].faceAttributes.emotion.sadness
                emotions += "\nsurprise: " + result[0].faceAttributes.emotion.surprise
                emotions += "\nAnger: " + result[0].faceAttributes.emotion.anger

                val text = ("Age: " + result[0].faceAttributes.age
                        + "\nGender: " + result[0].faceAttributes.gender
                        + emotions)

                activity.get()?.result_text_view?.text = text
            } else {
                activity.get()?.result_text_view?.text = "No face detected"
            }
        }
    }
}
