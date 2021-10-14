/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thomasjbarrerasconsulting.faces.kotlin

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.annotation.KeepName
import com.thomasjbarrerasconsulting.faces.BitmapUtils
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.databinding.ActivityStillImageBinding
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import com.thomasjbarrerasconsulting.faces.preference.SettingsActivity
import com.thomasjbarrerasconsulting.faces.preference.SettingsActivity.LaunchSource
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import android.view.ScaleGestureDetector
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.BitmapScaler


/** Activity demonstrating different image detector features with a still image from camera.  */
@KeepName
class StillImageActivity : AppCompatActivity() {
  private var preview: ImageView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var isLandScape = false
  private var imageUri: Uri? = null
  // Max width (portrait mode)
  private var imageMaxWidth = 0
  // Max height (portrait mode)
  private var imageMaxHeight = 0
  private var imageProcessor: FaceDetectorProcessor? = null
  private lateinit var binding: ActivityStillImageBinding
  private var localImageResultLauncher: ActivityResultLauncher<Intent>? = null
  private var imageFromPhotoResultLauncher: ActivityResultLauncher<Intent>? = null
  private var scaleGestureDetector: ScaleGestureDetector? = null
  private var scaleFactor: Float = 1.0f

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityStillImageBinding.inflate(layoutInflater)
    val view = binding.root

    setContentView(view)
    localImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        // In this case, imageUri is returned by the chooser, save it.
        imageUri = data!!.data
        tryReloadAndDetectInImage()
      }
    }

    imageFromPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        tryReloadAndDetectInImage()
      }
    }

    binding.selectImageButton
      .setOnClickListener { v: View ->
        // Menu for selecting either: a) take new photo b) select from existing
        val popup = PopupMenu(this@StillImageActivity, v)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
          val itemId = menuItem.itemId
          if (itemId == R.id.select_images_from_local) {
            startChooseImageIntentForResult()
            return@setOnMenuItemClickListener true
          } else if (itemId == R.id.take_photo_using_camera) {
            startCameraIntentForResult()
            return@setOnMenuItemClickListener true
          }
          false
        }
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.camera_button_menu, popup.menu)
        popup.show()
      }
    preview = binding.preview
    graphicOverlay = binding.graphicOverlay

    scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

    populateFeatureSelector()
    isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (savedInstanceState != null) {
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI)
      imageMaxWidth = savedInstanceState.getInt(KEY_IMAGE_MAX_WIDTH)
      imageMaxHeight = savedInstanceState.getInt(KEY_IMAGE_MAX_HEIGHT)
    }

    val rootView = binding.root
    rootView.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
          imageMaxWidth = rootView.width
          imageMaxHeight = rootView.height - binding.control.height
          tryReloadAndDetectInImage()
        }
      })

    val settingsButton = binding.settingsImageView.settingsImageView

    settingsButton.setOnClickListener {
      val intent =
        Intent(
          applicationContext,
          SettingsActivity::class.java
        )
      intent.putExtra(
        SettingsActivity.EXTRA_LAUNCH_SOURCE,
        LaunchSource.STILL_IMAGE
      )
      startActivity(intent)
    }
  }

  public override fun onResume() {
    super.onResume()
    Log.d(TAG, "onResume")
    createImageProcessor()
    tryReloadAndDetectInImage()
  }

  public override fun onPause() {
    super.onPause()
    imageProcessor?.run {
      this.stop()
    }
  }

  public override fun onDestroy() {
    super.onDestroy()
    imageProcessor?.run {
      this.stop()
    }
  }

  private fun populateFeatureSelector() {
    val featureSpinner = binding.featureSelector
    // Creating adapter for featureSpinner
    val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, FaceClassifierProcessor.allClassifications)
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    // attaching data adapter to spinner
    featureSpinner.adapter = dataAdapter
    featureSpinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onItemSelected(
        parentView: AdapterView<*>,
        selectedItemView: View?,
        pos: Int,
        id: Long
      ) {
        if (pos >= 0) {
          val selectedClassifier = parentView.getItemAtPosition(pos).toString()
          FaceClassifierProcessor.classifier = selectedClassifier
          Log.d(TAG, "Selected classifier: $selectedClassifier")

          createImageProcessor()
          tryReloadAndDetectInImage()
        }
      }

      override fun onNothingSelected(arg0: AdapterView<*>?) {}
    }
  }

  public override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(
      KEY_IMAGE_URI,
      imageUri
    )
    outState.putInt(
      KEY_IMAGE_MAX_WIDTH,
      imageMaxWidth
    )
    outState.putInt(
      KEY_IMAGE_MAX_HEIGHT,
      imageMaxHeight
    )
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    scaleGestureDetector!!.onTouchEvent(event)
    return true
  }

  private fun startCameraIntentForResult() { // Clean up last time's image
    imageUri = null
    preview!!.setImageBitmap(null)
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (takePictureIntent.resolveActivity(packageManager) != null) {
      val values = ContentValues()
      values.put(MediaStore.Images.Media.TITLE, "New Picture")
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
      imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

      imageFromPhotoResultLauncher?.launch(takePictureIntent)
    }
  }

  private fun startChooseImageIntentForResult() {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    localImageResultLauncher?.launch(Intent.createChooser(intent, "Select Picture"))
  }

  private fun tryReloadAndDetectInImage() {
    Log.d(
      TAG,
      "Try reload and detect image"
    )
    try {
      if (imageUri == null) {
        return
      }

      if (imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.  TODO
        return
      }

      val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, imageUri)?: return

      // Clear the overlay first
      graphicOverlay!!.clear()

      val scaledBitmap = BitmapScaler.scaleBitmap(imageBitmap, scaleFactor, imageMaxWidth, imageMaxHeight)

      preview!!.setImageBitmap(scaledBitmap)
      if (imageProcessor != null) {
        graphicOverlay!!.setImageSourceInfo(
          scaledBitmap.width, scaledBitmap.height, /* isFlipped= */false
        )
        imageProcessor!!.scale = scaleFactor
        imageProcessor!!.processBitmap(scaledBitmap, graphicOverlay!!)
      } else {
        Log.e(
          TAG,
          "Null imageProcessor, please check adb logs for imageProcessor creation error"
        )
      }
    } catch (e: IOException) {
      Log.e(
        TAG,
        "Error retrieving saved image"
      )
      imageUri = null
    }
  }

  private fun createImageProcessor() {
    try {
      imageProcessor = FaceDetectorProcessor(this, null)
    }
    catch (e: Exception) {
      Log.e(
        TAG,
        "Can not create image processor: ${FaceClassifierProcessor.classifier}",
        e
      )
      Toast.makeText(
        applicationContext,
        "Can not create image processor: " + e.message,
        Toast.LENGTH_LONG
      )
        .show()
    }
  }

  private inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
      scaleFactor = scaleGestureDetector!!.scaleFactor
      scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
      tryReloadAndDetectInImage()
      return true
    }

  }

  companion object {
    private const val TAG = "StillImageActivity"

    private const val KEY_IMAGE_URI = "com.thomasjbarrerasconsulting.faces.KEY_IMAGE_URI"
    private const val KEY_IMAGE_MAX_WIDTH = "com.thomasjbarrerasconsulting.faces.KEY_IMAGE_MAX_WIDTH"
    private const val KEY_IMAGE_MAX_HEIGHT = "com.thomasjbarrerasconsulting.faces.KEY_IMAGE_MAX_HEIGHT"
  }
}
