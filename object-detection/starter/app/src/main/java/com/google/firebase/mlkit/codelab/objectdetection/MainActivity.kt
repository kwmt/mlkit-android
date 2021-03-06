/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.mlkit.codelab.objectdetection

import android.Manifest
import android.os.Bundle
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.app.ActivityCompat
import android.provider.MediaStore
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions

class MainActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        const val ODT_PERMISSIONS_REQUEST: Int = 1
        const val ODT_REQUEST_IMAGE_CAPTURE = 1
    }

    private lateinit var outputFileUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImageFab.setOnClickListener {
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePhotoIntent.resolveActivity(packageManager) != null) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "MLKit_codelab")
                outputFileUri = contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                startActivityForResult(takePhotoIntent, ODT_REQUEST_IMAGE_CAPTURE)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            captureImageFab.isEnabled = false
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ODT_PERMISSIONS_REQUEST
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ODT_REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            val image = getCapturedImage()

            // display capture image
            imageView.setImageBitmap(image)

            // TODO: run through ODT and display result
            runObjectDetection(image)
        }
    }

    /**
     * MLKit Object Detection Function
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // TODO: implement along codelab
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        detector.processImage(image)
            .addOnSuccessListener {
                debugPrint(it)
            }
            .addOnFailureListener {
                Toast.makeText(baseContext, "Oops, something went wrong!",
                    Toast.LENGTH_SHORT).show()
            }

    }

    private fun debugPrint(visionObjects : List<FirebaseVisionObject>) {
        val LOG_MOD = "MLKit-ODT"
        for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox

            Log.d(LOG_MOD, "Detected object: ${idx} ")
            Log.d(LOG_MOD, "  Category: ${obj.classificationCategory}")
            Log.d(LOG_MOD, "  trackingId: ${obj.trackingId}")
            Log.d(LOG_MOD, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                Log.d(LOG_MOD, "  Confidence: ${confidence}%")
            }
        }
    }

    /**
     * getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {

        val srcImage = FirebaseVisionImage
            .fromFilePath(baseContext, outputFileUri).getBitmap()


        // crop image to match imageView's aspect ratio
        val scaleFactor = Math.min(
            srcImage.width / imageView.width.toFloat(),
            srcImage.height / imageView.height.toFloat()
        )

        val deltaWidth = (srcImage.width - imageView.width * scaleFactor).toInt()
        val deltaHeight = (srcImage.height - imageView.height * scaleFactor).toInt()

        val scaledImage = Bitmap.createBitmap(
            srcImage, deltaWidth / 2, deltaHeight / 2,
            srcImage.width - deltaWidth, srcImage.height - deltaHeight
        )
        srcImage.recycle()
        return scaledImage

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            ODT_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    captureImageFab.isEnabled = true
                }
            }
        }
    }
}

