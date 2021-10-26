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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.annotation.KeepName
import com.thomasjbarrerasconsulting.faces.CameraSource
import com.thomasjbarrerasconsulting.faces.CameraSourcePreview
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import com.thomasjbarrerasconsulting.faces.preference.PreferenceUtils
import com.thomasjbarrerasconsulting.faces.preference.SettingsActivity
import com.thomasjbarrerasconsulting.faces.preference.SettingsActivity.LaunchSource
import java.io.IOException
import java.util.ArrayList
import android.media.ToneGenerator

import android.media.AudioManager
import android.os.PersistableBundle
import com.thomasjbarrerasconsulting.faces.databinding.ActivityVisionLivePreviewBinding


/** Live preview demo for ML Kit APIs.  */
@KeepName
class LivePreviewActivity :
  AppCompatActivity(),
  ActivityCompat.OnRequestPermissionsResultCallback,
  OnItemSelectedListener,
  CompoundButton.OnCheckedChangeListener {

  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var selectedModel = FACE_DETECTION
  private lateinit var binding: ActivityVisionLivePreviewBinding
  private lateinit var permissionsHandler: PermissionsHandler

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      Log.d(TAG, "onCreate")
      super.onCreate(savedInstanceState)

      binding = ActivityVisionLivePreviewBinding.inflate(layoutInflater)
      val view = binding.root
      graphicOverlay = binding.graphicOverlay
      preview = binding.previewLiveView
      setContentView(view)

      val launchStillImageAndUseCameraButton = binding.launchStillImageAndUseCamera
      launchStillImageAndUseCameraButton!!.setOnClickListener {
        val intent = Intent(this, StillImageActivity::class.java)
        intent.putExtra("fromCamera", true)
        startActivity(intent)
      }

      val launchStillImageAndSelectImageButton = binding.launchStillImageAndSelectImage
      launchStillImageAndSelectImageButton!!.setOnClickListener {
        val intent = Intent(this, StillImageActivity::class.java)
        intent.putExtra("fromCamera", false)
        startActivity(intent)
      }

      // TODO: handle permissions properly
      permissionsHandler = PermissionsHandler(this)
      if (!permissionsHandler.allPermissionsGranted()) {
        permissionsHandler.getRuntimePermissions()
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

      val settingsButton = binding.settingsImageView?.settingsImageView
      settingsButton?.setOnClickListener {
        val intent = Intent(applicationContext, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.LIVE_PREVIEW)
        startActivity(intent)
      }

      if (permissionsHandler.allPermissionsGranted()) {
        createCameraSource(selectedModel)
      } else {
        permissionsHandler.getRequiredPermissions()
      }

    } catch (e: Exception){
      Log.e(TAG, e.message.toString())
    }

  }

//  private fun Beep(){
//    val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
//    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
//  }

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

  private fun createCameraSource(model: String) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
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
        preview!!.start(cameraSource, graphicOverlay)
      } catch (e: IOException) {
        Log.e(TAG, "Unable to start camera source.", e)
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
    if (permissionsHandler.allPermissionsGranted()) {
      createCameraSource(selectedModel)
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

//  override fun onSaveInstanceState(outState: Bundle) {
//    outState.putInt(SETTING_CAMERA_FACING_FRONT, cameraFacing)
//    super.onSaveInstanceState(outState)
//  }

  companion object {
    private const val FACE_DETECTION = "Face Detection"
    private const val TAG = "LivePreviewActivity"
    private const val SETTING_CAMERA_FACING_FRONT = "cameraFacingFront"
  }
}
