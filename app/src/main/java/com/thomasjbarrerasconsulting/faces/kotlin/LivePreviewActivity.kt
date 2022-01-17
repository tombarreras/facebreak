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
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.annotation.KeepName
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import com.thomasjbarrerasconsulting.faces.preference.PreferenceUtils
import com.thomasjbarrerasconsulting.faces.*
import com.thomasjbarrerasconsulting.faces.databinding.ActivityVisionLivePreviewBinding
import com.thomasjbarrerasconsulting.faces.kotlin.Toaster.Companion.toast
import com.thomasjbarrerasconsulting.faces.kotlin.billing.BillingHandler
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
  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var messageText: TextView? = null
  lateinit var adView : AdView
  private lateinit var consentInformation: ConsentInformation
  private lateinit var consentForm: ConsentForm
  private var selectedModel = FACE_DETECTION
  private lateinit var binding: ActivityVisionLivePreviewBinding
  private lateinit var permissionsHandler: PermissionsHandler
  private lateinit var firebaseAnalytics: FirebaseAnalytics

  private val purchasesListener = object: ObservableList.ListUpdatedListener<Purchase> {
    override fun listUpdated(list: List<Purchase>) {
      toast("Purchases: $list")
    }
  }

  private val skusListener = object: ObservableList.ListUpdatedListener<SkuDetails> {
    override fun listUpdated(list: List<SkuDetails>) {
      toast("Skus: $list")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      Log.d(TAG, "onCreate")
      super.onCreate(savedInstanceState)

      initializeBillingAndPurchases()
      initializePermissions()
      inflateUI()
      obtainConsent()
      initializeAnalytics()
      Ads.initialize(this, adView)
      initializeFacingSwitchButton()

      binding.launchStillImageAndUseCamera.setOnClickListener { startStillImageFromCameraActivity() }
      binding.launchStillImageAndSelectImage.setOnClickListener { startLocalStillImageActivity()  }
      initializePreferencesButton()

      initializeShareButton()

//      settingsButton.setOnClickListener { BillingHandler.startPurchaseFlow(BillingHandler.skus.items().first(), this) }

      populateClassifierSelector()
      createAndInitializeCameraSource(selectedModel)

    } catch (e: Exception){
      Log.e(TAG, e.message.toString())
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
    graphicOverlay = binding.graphicOverlay
    preview = binding.previewLiveView
    messageText = binding.messageTextView
    adView = binding.adView
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
    BillingHandler.addSkusListener(skusListener)
  }

  private fun populateClassifierSelector() {
    val spinner = binding.featureSelector

    // Creating adapter for spinner
    val dataAdapter = ArrayAdapter(
      this,
      R.layout.spinner_style,
      FaceClassifierProcessor.allClassificationDescriptions(this)
    )

    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    // attaching data adapter to spinner
    spinner.adapter = dataAdapter
    spinner.setSelection(FaceClassifierProcessor.Classifier.values().indexOf(Settings.selectedClassifier))
    spinner.onItemSelectedListener = ClassifierSelectedListener(this, firebaseAnalytics)
  }

  private fun obtainConsent() {
    // Set tag for underage of consent. false means users are not underage.
    val params = ConsentRequestParameters.Builder()
      .setTagForUnderAgeOfConsent(false)
      .build()

    consentInformation = UserMessagingPlatform.getConsentInformation(this)
    consentInformation.requestConsentInfoUpdate(
      this,
      params,
      {
        // The consent information state was updated.
        // You are now ready to check if a form is available.
        if (consentInformation.isConsentFormAvailable) {
          loadConsentForm()
        }
      },
      {
        ExceptionHandler.alert(this, getString(R.string.failed_to_obtain_consent) + " ${it.errorCode}", TAG, java.lang.Exception(it.message))
      })
  }

  private fun loadConsentForm() {
    UserMessagingPlatform.loadConsentForm(
      this,
      { consentForm ->
        this.consentForm = consentForm
        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
          consentForm.show(
            this
          ) { // Handle dismissal by reloading form.
            loadConsentForm()
          }
        }
      }
    ) {
      /// Handle Error.
      ExceptionHandler.alert(this, getString(R.string.failed_to_load_consent_form) + " ${it.errorCode}", TAG, java.lang.Exception(it.message))
    }
  }

  private fun initializeAds() {
    MobileAds.initialize(this) {}
    adView = findViewById(R.id.adView)
    val adRequest = AdRequest.Builder().build()
    adView.loadAd(adRequest)
  }

  private fun showPreferences() {
    try {
      preferencesResultLauncher?.launch(Intent(applicationContext, PreferencesActivity::class.java))
    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_show_preferences_exception), TAG, e)
    }
  }

  private fun startLocalStillImageActivity() {
    try {
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
          val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptionsForLivePreview(this)
          cameraSource!!.setMachineLearningFrameProcessor(
            FaceDetectorProcessor(this, faceDetectorOptions)
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
    super.onResume()
    Log.d(TAG, "onResume")

    runBlocking {
      launch{
        BillingHandler.refreshInAppPurchases()
      }
      createAndInitializeCameraSource(selectedModel)
      startCameraSource()
      binding.featureSelector.setSelection(FaceClassifierProcessor.Classifier.values().indexOf(Settings.selectedClassifier))
    }
  }

  /** Stops the camera.  */
  override fun onPause() {
    super.onPause()
    preview?.stop()
  }

  public override fun onDestroy() {
    toast("Destroy")
    super.onDestroy()
    BillingHandler.removePurchasesListener(purchasesListener)
    BillingHandler.removeSkusListener(skusListener)
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
