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

/*
 * Modified by Thomas J. Barreras
 * https://www.linkedin.com/in/tombarreras/
*/

package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.annotation.KeepName
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import com.thomasjbarrerasconsulting.faces.preference.PreferenceUtils
import com.thomasjbarrerasconsulting.faces.*
import com.thomasjbarrerasconsulting.faces.databinding.ActivityVisionLivePreviewBinding
import com.thomasjbarrerasconsulting.faces.preference.PreferencesActivity


@KeepName
class LivePreviewActivity :
  AppCompatActivity(),
  ActivityCompat.OnRequestPermissionsResultCallback,
  OnItemSelectedListener,
  CompoundButton.OnCheckedChangeListener {

  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var messageText: TextView? = null
  private var selectedModel = FACE_DETECTION
  private lateinit var binding: ActivityVisionLivePreviewBinding
  private lateinit var permissionsHandler: PermissionsHandler
  private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      Log.d(TAG, "onCreate")
      super.onCreate(savedInstanceState)

      requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
          createCameraSource(selectedModel)
          startCameraSource()
        } else {
          AlertDialog.Builder(this)
            .setTitle("Camera Permission Not Granted")
            .setMessage("Live video analysis is not available without Camera permission, but you can still use this amazing app to analyze images in your gallery.")
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
        }
      }

      // TODO: handle permissions properly
      permissionsHandler = PermissionsHandler(this)
      if (!permissionsHandler.allPermissionsGranted()) {
        permissionsHandler.getRuntimePermissions()
      }

      binding = ActivityVisionLivePreviewBinding.inflate(layoutInflater)
      val view = binding.root
      graphicOverlay = binding.graphicOverlay
      preview = binding.previewLiveView
      messageText = binding.messageTextView

      setContentView(view)

      val launchStillImageAndUseCameraButton = binding.launchStillImageAndUseCamera
      launchStillImageAndUseCameraButton.setOnClickListener {
        Settings.stillImageExists = false
        val intent = Intent(this, StillImageActivity::class.java)
        intent.putExtra(StillImageActivity.GET_IMAGE_FROM, StillImageActivity.GET_IMAGE_FROM_CAMERA)
        startActivity(intent)
      }

      val launchStillImageAndSelectImageButton = binding.launchStillImageAndSelectImage
      launchStillImageAndSelectImageButton.setOnClickListener {
        Settings.stillImageExists = false
        val intent = Intent(this, StillImageActivity::class.java)
        intent.putExtra(StillImageActivity.GET_IMAGE_FROM, StillImageActivity.GET_IMAGE_FROM_IMAGE_STORE)
        startActivity(intent)
      }

      val spinner = binding.featureSelector

      // Creating adapter for spinner
      val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, FaceClassifierProcessor.allClassifications)

      // Drop down layout style - list view with radio button
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      // attaching data adapter to spinner
      spinner.adapter = dataAdapter
      spinner.setSelection(Settings.selectedClassifier)
      spinner.onItemSelectedListener = this

      val facingSwitch = binding.facingSwitch
      facingSwitch.isChecked = Settings.cameraFacing == CameraSource.CAMERA_FACING_FRONT
      facingSwitch.setOnCheckedChangeListener(this)

      val settingsButton = binding.settingsImageView.settingsImageView
      settingsButton.setOnClickListener {
        val intent = Intent(applicationContext, PreferencesActivity::class.java)
        startActivity(intent)
      }

      binding.share.setOnClickListener {
        startShareIntent()
      }

      createCameraSource(selectedModel)

    } catch (e: Exception){
      Log.e(TAG, e.message.toString())
    }

  }

  private fun startShareIntent() {
    if (saveCurrentImageToCache(StillImageActivity.SHARED_IMAGE_NAME, Bitmap.CompressFormat.JPEG)){
      startActivity(Intent.createChooser(ShareUtils.createShareIntent(this), "Send to..."))
    }
  }

  private fun saveCurrentImageToCache(fileName:String, fileType:Bitmap.CompressFormat): Boolean {
    val imageBitmap = getBitmapOfDisplayedImage() ?: return false

    try {
      val canvas = Canvas(imageBitmap)
      graphicOverlay?.draw(canvas)

      ShareUtils.drawClassifierAndLogo(this, canvas, binding.featureSelector.selectedItem.toString())

      return ImageUtils.saveImageToCache(this, imageBitmap, fileName, fileType)
    }
    finally {
      imageBitmap.recycle()
    }
  }

  private fun getBitmapOfDisplayedImage(): Bitmap? {
    val imageBitmap = preview?.bitmap

    if (imageBitmap != null){
      return Bitmap.createBitmap(imageBitmap, 0, 0, preview!!.width, preview!!.height)
    }

    return null
  }

  @Synchronized
  override fun onItemSelected(
    parent: AdapterView<*>?,
    view: View?,
    pos: Int,
    id: Long
  ) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    val selectedClassifier = parent?.getItemAtPosition(pos).toString()
    Settings.selectedClassifier = pos
    FaceClassifierProcessor.classifier = selectedClassifier
    Log.d(TAG, "Selected classifier: $selectedClassifier")
//    preview?.stop()
//    if (allPermissionsGranted()) {
//      createCameraSource(selectedModel)
//      startCameraSource()
//    } else {
//      runtimePermissions
//    }
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    // Do nothing.
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    Log.d(TAG, "Set facing")
    Settings.cameraFacing = if (isChecked) {
      CameraSource.CAMERA_FACING_FRONT
    } else {
      CameraSource.CAMERA_FACING_BACK
    }

    cameraSource?.setFacing(Settings.cameraFacing)
    preview?.stop()
    startCameraSource()
  }

  private fun checkCameraAndPermissionsExist():Boolean {
    var result: Boolean = true

    if (!CameraHandler.cameraExists(this)){
      messageText?.text = getString(R.string.no_camera_detected_message)
      messageText?.contentDescription = getString(R.string.no_camera_detected_message)
      result = false
    }

    if (!CameraHandler.cameraPermissionExists(this, permissionsHandler)){
      messageText?.text = getString(R.string.no_camera_permission_detected)
      messageText?.contentDescription = getString(R.string.no_camera_permission_detected)
      result = false
    }

    if (result) {
      messageText?.text = ""
      messageText?.contentDescription = ""
    }

    if (!result){
      binding.launchStillImageAndUseCamera.visibility = View.GONE
      binding.facingSwitch.visibility = View.GONE
      binding.share.visibility = View.GONE
    }
    else {
      binding.launchStillImageAndUseCamera.visibility = View.VISIBLE
      if (CameraHandler.frontFacingCameraExists(this)){
        binding.facingSwitch.visibility = View.VISIBLE
      } else {
        binding.facingSwitch.visibility = View.GONE
      }

      binding.share.visibility = View.VISIBLE
    }

    return result
  }

  private fun createCameraSource(model: String) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      if (!checkCameraAndPermissionsExist()){
        return
      }
      cameraSource = CameraSource(this, graphicOverlay)
      cameraSource?.setFacing(Settings.cameraFacing)
    }
    try {
      when (model) {
        FACE_DETECTION -> {
          Log.i(TAG, "Using Face Detector Processor")
          val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptionsForLivePreview(this)
          cameraSource!!.setMachineLearningFrameProcessor(
            FaceDetectorProcessor(this, faceDetectorOptions)
          )
        }
        else -> Log.e(TAG, "Unknown model: $model")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Can not create image processor: $model", e)
      Toast.makeText(
        applicationContext, "Can not create image processor: " + e.message,
        Toast.LENGTH_LONG
      ).show()
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private fun startCameraSource() {
    if (cameraSource != null) {
      try {
        if (CameraHandler.cameraAndPermissionExists(this, permissionsHandler)){
          preview!!.start(cameraSource, graphicOverlay)
        }
      } catch (e: Exception) {
        ExceptionHandler.Alert(this, "Unable to start camera source", TAG, e)
        cameraSource!!.release()
        cameraSource = null
      }
    }
  }

  public override fun onResume() {
    super.onResume()
    Log.d(TAG, "onResume")
    createCameraSource(selectedModel)
    startCameraSource()
    binding.featureSelector.setSelection(Settings.selectedClassifier)
  }

  /** Stops the camera.  */
  override fun onPause() {
    super.onPause()
    preview?.stop()
  }

  public override fun onDestroy() {
    super.onDestroy()
    if (cameraSource != null) {
      cameraSource?.release()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    Log.i(TAG, "Permission granted!")
    createCameraSource(selectedModel)
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  companion object {
    private const val FACE_DETECTION = "Face Detection"
    private const val TAG = "LivePreviewActivity"
    private const val SETTING_CAMERA_FACING_FRONT = "cameraFacingFront"
  }
}
