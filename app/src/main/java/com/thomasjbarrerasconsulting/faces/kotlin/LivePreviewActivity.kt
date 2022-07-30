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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdView
import com.google.android.gms.common.annotation.KeepName
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import com.thomasjbarrerasconsulting.faces.*
import com.thomasjbarrerasconsulting.faces.databinding.ActivityVisionLivePreviewBinding
import com.thomasjbarrerasconsulting.faces.kotlin.billing.BillingHandler
import com.thomasjbarrerasconsulting.faces.kotlin.billing.Premium
import com.thomasjbarrerasconsulting.faces.preference.PreferencesActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@KeepName
class LivePreviewActivity :
  AppCompatActivity(),
  ActivityCompat.OnRequestPermissionsResultCallback,
  CompoundButton.OnCheckedChangeListener {

  private var shareResultLauncher: ActivityResultLauncher<Intent>? = null
  private var preferencesResultLauncher: ActivityResultLauncher<Intent>? = null
  private var premiumStatusResultLauncher: ActivityResultLauncher<Intent>? = null
  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var messageText: TextView? = null
  private var selectedModel = FACE_DETECTION
  private lateinit var adView : AdView
  private lateinit var binding: ActivityVisionLivePreviewBinding
  private lateinit var permissionsHandler: PermissionsHandler
  private lateinit var firebaseAnalytics: FirebaseAnalytics

  private val purchasesListener = object: ObservableList.ListUpdatedListener<Purchase> {
    override fun listUpdated(list: List<Purchase>) {
      updatePremiumStatus()
      populateClassifierSelector()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      Log.d(TAG, "onCreate")
      super.onCreate(savedInstanceState)

      inflateUI()
      Privacy.obtainConsent(this) {
        Ads.loadAds(this, adView)
        initializeAnalytics()
      }
      initializeAnalytics()
      Ads.initialize(this, adView)

      binding.launchStillImageAndUseCamera.setOnClickListener { startStillImageFromCameraActivity() }
      binding.launchStillImageAndSelectImage.setOnClickListener { startLocalStillImageActivity()  }
      binding.featureSelector.onItemSelectedListener = ClassifierSelectedListener(this, firebaseAnalytics, { showPremiumStatus() }, { Review.checkLaunchInAppReview(this) })

      initializeBillingAndPurchases()
      initializeFacingSwitchButton()
      initializePremiumStatusButton()
      initializePreferencesButton()
      initializeShareButton()
      populateClassifierSelector()

      initializePermissions()
      createAndInitializeCameraSource(selectedModel)
      initializeReview()

    } catch (e: Exception){
      ExceptionHandler.alert(this, e.message.toString(), TAG, e)
    }
  }

  private fun initializeReview() {
    Review.initialize(this)
  }

  private fun initializePremiumStatusButton() {
    premiumStatusResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePremiumStatus()
        populateClassifierSelector()
      }

    updatePremiumStatus()
    binding.premiumStatusImageView.setOnClickListener { showPremiumStatus() }
  }

  private fun updatePremiumStatus() {
    runOnUiThread {
      when {
        Premium.premiumIsActive() -> binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_premium)
        Premium.premiumIsPending() -> binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_premium_pending)
        else -> binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_free)
      }
    }
  }

  private fun initializePreferencesButton() {
    preferencesResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Analytics.setAnalyticsEnabled(this, firebaseAnalytics)
        Ads.loadAds(this, adView)
      }

    binding.settingsImageView.settingsImageView.setOnClickListener { showPreferences() }
  }

  private fun initializeShareButton() {
    shareResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
          if (result.resultCode == RESULT_OK) {

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
              param(FirebaseAnalytics.Param.CONTENT_TYPE, "live_image")
              param(
                FirebaseAnalytics.Param.ITEM_ID,
                FaceClassifierProcessor.classifierDescriptionEnglish(FaceClassifierProcessor.classifier)
              )
            }
          }
      }

    binding.share.setOnClickListener { startShareIntent() }
  }

  private fun initializeFacingSwitchButton() {
    val facingSwitch = binding.facingSwitch
    facingSwitch.isChecked = Settings.cameraFacing == CameraSource.CAMERA_FACING_FRONT
    facingSwitch.setOnCheckedChangeListener(this)
  }

  private fun initializeAnalytics() {
    firebaseAnalytics = Firebase.analytics
    Analytics.setAnalyticsEnabled(this, firebaseAnalytics)
  }

  private fun inflateUI() {
    binding = ActivityVisionLivePreviewBinding.inflate(layoutInflater)
    adView = binding.adView
    graphicOverlay = binding.graphicOverlay
    preview = binding.previewLiveView
    messageText = binding.messageTextView
    setContentView(binding.root)
  }

  private fun initializePermissions() {
    permissionsHandler = PermissionsHandler(this)
    if (!permissionsHandler.allPermissionsGranted()) {
      permissionsHandler.getRuntimePermissions()
    }
  }

  private fun initializeBillingAndPurchases() {
    BillingHandler.addPurchasesListener(purchasesListener)
    BillingHandler.initialize()
  }

  private fun populateClassifierSelector(){

    try {
      runOnUiThread{
        val spinner = binding.featureSelector

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(
          this,
          R.layout.spinner_style,
          if (Premium.premiumIsActive()) FaceClassifierProcessor.allClassificationDescriptions(this) else FaceClassifierProcessor.allClassificationDescriptionsFree(this)
        )
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        spinner.adapter = dataAdapter

        spinner.setSelection(FaceClassifierProcessor.Classifier.values().indexOf(Settings.selectedClassifier))
      }

    } catch (e: Exception){
      ExceptionHandler.alert(this, e.message.toString(), TAG, e)
    }
  }

  private fun showPremiumStatus() {
    try {
      Review.resetLaunchInAppReviewCount()
      premiumStatusResultLauncher?.launch(Intent(applicationContext, PremiumStatusActivity::class.java))
    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_show_premium_status_dialog_exception), TAG, e)
    }
  }

  private fun showPreferences() {
    try {
      Review.resetLaunchInAppReviewCount()
      preferencesResultLauncher?.launch(Intent(applicationContext, PreferencesActivity::class.java))
    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_show_preferences_exception), TAG, e)
    }
  }

  private fun startLocalStillImageActivity() {
    try {
      Review.resetLaunchInAppReviewCount()
      Settings.stillImageExists = false
      val intent = Intent(this, StillImageActivity::class.java)
      intent.putExtra(StillImageActivity.GET_IMAGE_FROM, StillImageActivity.GET_IMAGE_FROM_IMAGE_STORE)
      startActivity(intent)
    }
    catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_launch_image_browser_exception), TAG, e)
    }
  }

  private fun startStillImageFromCameraActivity() {
      try {
        Review.resetLaunchInAppReviewCount()
        Settings.stillImageExists = false
        val intent = Intent(this, StillImageActivity::class.java)
        intent.putExtra(StillImageActivity.GET_IMAGE_FROM, StillImageActivity.GET_IMAGE_FROM_CAMERA)
        startActivity(intent)
      }
      catch (e: Exception){
        ExceptionHandler.alert(this, getString(R.string.failed_to_connect_to_camera_exception_message), TAG, e)
    }
  }

  private fun startShareIntent() {
    try {
      Review.resetLaunchInAppReviewCount()
      if (saveCurrentImageToCache(StillImageActivity.SHARED_IMAGE_NAME, Bitmap.CompressFormat.JPEG)){
        shareResultLauncher?.launch(Intent.createChooser(ShareUtils.createShareIntent(this), getString(R.string.send_to_title)))
      }
    } catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_share_image_exception), TAG, e)
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

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    Log.d(TAG, "Set facing")
    Review.resetLaunchInAppReviewCount()
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
    var result = true

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

  private fun createAndInitializeCameraSource(model: String) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      if (!checkCameraAndPermissionsExist()){
        return
      }

      if (!createCameraSourceObject()) {
        return
      }
    }
    try {
      when (model) {
        FACE_DETECTION -> {
          Log.i(TAG, "Using Face Detector Processor")
          cameraSource!!.setMachineLearningFrameProcessor(
            FaceDetectorProcessor(this, 100)
          )
        }
        else -> Log.e(TAG, "Unknown model: $model")
      }
    } catch (e: Exception) {
      ExceptionHandler.alert(this,  getString(R.string.failed_to_initialize_camera_source_exception) + " '$model'", TAG, e)
    }
  }

  private fun createCameraSourceObject(): Boolean {
    try {
      cameraSource = CameraSource(this, graphicOverlay)
      cameraSource?.setFacing(Settings.cameraFacing)
    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_connect_to_camera_exception_message), TAG, e)
      return false
    }
    return true
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
        ExceptionHandler.alert(this, getString(R.string.failed_to_start_camera_source_exception), TAG, e)
        cameraSource!!.release()
        cameraSource = null
      }
    }
  }

  public override fun onResume() {
    try {
      super.onResume()
      Log.d(TAG, "onResume")

      runBlocking {
        if (!Premium.premiumIsActive()) {
          BillingHandler.addPurchasesListener(purchasesListener)
          launch {
            BillingHandler.refreshInAppPurchases()
          }
        }
        createAndInitializeCameraSource(selectedModel)
        startCameraSource()
        binding.featureSelector.setSelection(FaceClassifierProcessor.Classifier.values().indexOf(Settings.selectedClassifier))
      }
    } catch (e: Exception){
      ExceptionHandler.alert(this, "Failed to resume LivePreviewActivity", TAG, e)
    }
  }

  /** Stops the camera.  */
  override fun onPause() {
    super.onPause()
    BillingHandler.removePurchasesListener(purchasesListener)
    preview?.stop()
  }

  public override fun onDestroy() {
    super.onDestroy()
    BillingHandler.removePurchasesListener(purchasesListener)
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
    createAndInitializeCameraSource(selectedModel)
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  companion object {
    private const val FACE_DETECTION = "Face Detection"
    private const val TAG = "LivePreviewActivity"
  }
}
