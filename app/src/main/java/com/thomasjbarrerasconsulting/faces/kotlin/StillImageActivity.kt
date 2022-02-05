/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You Uri?may obtain a copy of the License at
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

import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.annotation.KeepName
import com.thomasjbarrerasconsulting.faces.BitmapUtils
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.databinding.ActivityStillImageBinding
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceDetectorProcessor
import kotlin.math.max
import kotlin.math.min
import android.view.ScaleGestureDetector
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.BitmapScaler
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdView
import com.thomasjbarrerasconsulting.faces.ImageUtils
import com.thomasjbarrerasconsulting.faces.preference.PreferencesActivity
import java.lang.NullPointerException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.thomasjbarrerasconsulting.faces.kotlin.billing.BillingHandler
import com.thomasjbarrerasconsulting.faces.kotlin.billing.Premium
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@KeepName
class StillImageActivity : AppCompatActivity() {
  private var preview: ImageView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var isLandScape = false
  // Max width (portrait mode)
  private var imageMaxWidth = 0
  // Max height (portrait mode)
  private var imageMaxHeight = 0
  private var imageProcessor: FaceDetectorProcessor? = null
  private var localImageResultLauncher: ActivityResultLauncher<Intent>? = null
  private var imageFromPhotoResultLauncher: ActivityResultLauncher<Intent>? = null
  private var preferencesResultLauncher: ActivityResultLauncher<Intent>? = null
  private var shareResultLauncher: ActivityResultLauncher<Intent>? = null
  private var premiumStatusResultLauncher: ActivityResultLauncher<Intent>? = null
  private var scrolling: Boolean = false
  private var imageExists: Boolean = false
  private var cameraImageUri: Uri? = null
  private var localImage: Boolean = false
  private lateinit var scaleGestureDetector: ScaleGestureDetector
  private lateinit var panGestureDetector: GestureDetector
  private lateinit var binding: ActivityStillImageBinding
  private lateinit var adView : AdView
  private lateinit var firebaseAnalytics: FirebaseAnalytics

  private val purchasesListener = object: ObservableList.ListUpdatedListener<Purchase> {
    override fun listUpdated(list: List<Purchase>) {
      updatePremiumStatus()
      populateClassifierSelector()
    }
  }

  private var scaleFactor: Float = 1.0f
    set(value){
      field = value
      preview!!.scaleX = value
      preview!!.scaleY = value
    }

  private fun isImageLoaded():Boolean {
    val drawable = preview!!.drawable ?: return false
    if (drawable !is BitmapDrawable) return false
    return drawable.bitmap != null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initializeBillingAndPurchases()
    inflateUI()
    initializePermissions()
    initializeAnalytics()
    Ads.initialize(this, adView)
    initializeLocalImageBrowseButton()
    initializeImageFromPhotoButton()
    initializePreferencesButton()
    initializeShareButton()
    updatePremiumStatus()

    binding.launchLiveView.setOnClickListener { startLivePreviewActivity() }

    initializePremiumStatusButton()
    initializeGestureDetection()
    initializeClassifierSelector()

    isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    loadState()
    initializeDisplay()
  }

  private fun initializePremiumStatusButton() {
    premiumStatusResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePremiumStatus()
        populateClassifierSelector()
      }

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

  private fun initializeGestureDetection() {
    scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
    panGestureDetector = GestureDetector(this, PanListener())
  }

  private fun initializeDisplay() {
    val rootView = binding.root
    rootView.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
          imageMaxWidth = rootView.width
          imageMaxHeight = rootView.height - binding.toolbar.height
          loadOrGetImage()
        }
      })
  }

  private fun initializePermissions() {
    if (!CameraHandler.cameraAndPermissionExists(this, PermissionsHandler(this))) {
      binding.takePicture.visibility = View.GONE
      }
  }

  private fun initializeShareButton() {
    shareResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {

          val type = if (localImage) "local_image" else "photograph"
          firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, type)
            param(
              FirebaseAnalytics.Param.ITEM_ID,
              FaceClassifierProcessor.classifierDescriptionEnglish(FaceClassifierProcessor.classifier)
            )
          }
        }
      }
    binding.share.setOnClickListener { startShareIntent() }
  }

  private fun initializePreferencesButton() {
    preferencesResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Analytics.setAnalyticsEnabled(this, firebaseAnalytics)
        Ads.loadAds(this, adView)
        tryLoadAndClassifyImage()
      }

    binding.settingsImageView.settingsImageView.setOnClickListener { showPreferences() }
  }

  private fun initializeImageFromPhotoButton() {
    imageFromPhotoResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
          localImage = false

          firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "camera_image")
          }

          resetImage(cameraImageUri)
        } else if (result.resultCode == RESULT_CANCELED) {
          if (!imageExists) {
            finish()
          }
        }
      }

    binding.takePicture.setOnClickListener {
      takePicture()
    }
  }

  private fun initializeLocalImageBrowseButton() {
    localImageResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
          val data: Intent? = result.data
          localImage = true

          firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "local_image")
          }

          resetImage(data!!.data)
        } else if (result.resultCode == RESULT_CANCELED) {
          if (!imageExists) {
            finish()
          }
        }
      }

    binding.selectImage.setOnClickListener {
      browseForImage()
    }
  }

  private fun initializeAnalytics() {
    firebaseAnalytics = Firebase.analytics
  }

  private fun inflateUI() {
    binding = ActivityStillImageBinding.inflate(layoutInflater)
    adView = binding.adView
    preview = binding.preview
    graphicOverlay = binding.graphicOverlay

    setContentView(binding.root)
  }

  private fun initializeBillingAndPurchases() {
    BillingHandler.addPurchasesListener(purchasesListener)
  }

  private fun startLivePreviewActivity() {
    try {
      startActivity(Intent(this, LivePreviewActivity::class.java))
    }
    catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_start_live_preview_exception), TAG, e)
    }
  }

  private fun resetImage(imageUri: Uri?) {
    try {
      val imageBitmap = if (imageUri == null) null else BitmapUtils.getBitmapFromContentUri(contentResolver, imageUri)

      if (imageBitmap == null) {
        ImageUtils.deleteImageFromCache(this, CLASSIFIED_IMAGE_NAME, Bitmap.CompressFormat.PNG)
      } else {
        ImageUtils.saveImageToCache(this, imageBitmap, CLASSIFIED_IMAGE_NAME, Bitmap.CompressFormat.PNG)
      }

      loadImage(null)

      imageExists = imageBitmap != null
      scaleFactor = 1.0f
      preview!!.x = preview!!.left.toFloat()
      preview!!.y = preview!!.top.toFloat()

      saveState()
    }
    catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_reset_image_exception), TAG, e)
    }
  }

  private fun saveState(){
    Settings.stillImageExists = imageExists
    Settings.stillImageScaleFactor = scaleFactor
    Settings.stillImageX = preview!!.x
    Settings.stillImageY = preview!!.y
  }

  private fun loadState() {
    imageExists = Settings.stillImageExists
    scaleFactor = Settings.stillImageScaleFactor
    preview!!.x = Settings.stillImageX
    preview!!.y = Settings.stillImageY
  }

  private fun startShareIntent() {
    try {
      if (saveCurrentImageToCache(SHARED_IMAGE_NAME, Bitmap.CompressFormat.JPEG)){
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

  private fun loadOrGetImage(){
    val getImageFrom = intent.getStringExtra(GET_IMAGE_FROM)
    if (!imageExists) {
      if (getImageFrom == GET_IMAGE_FROM_CAMERA) {
        takePicture()
      } else if (getImageFrom == GET_IMAGE_FROM_IMAGE_STORE) {
        browseForImage()
      }
    } else if (!isImageLoaded()) {
      tryLoadAndClassifyImage()
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
        createImageProcessor()
        if (!isImageLoaded()) {
          tryLoadAndClassifyImage()
        }
      }
    } catch (e: Exception){
      ExceptionHandler.alert(this, "Failed to resume StillImageActivity", TAG, e)
    }
  }

  public override fun onPause() {
    super.onPause()
    BillingHandler.removePurchasesListener(purchasesListener)
    imageProcessor?.run {
      this.stop()
    }
    saveState()
  }

  public override fun onDestroy() {
    super.onDestroy()
    BillingHandler.removePurchasesListener(purchasesListener)
    imageProcessor?.run {
      this.stop()
    }
  }

  private fun initializeClassifierSelector() {
    populateClassifierSelector()
    binding.featureSelector.onItemSelectedListener = StillImageActivityClassifierSelectedListener(this, this, firebaseAnalytics) { showPremiumStatus() }
  }

  private fun populateClassifierSelector() {
    runOnUiThread{
      val featureSpinner = binding.featureSelector
      val featureSpinnerDataAdapter = ArrayAdapter(
        this,
        R.layout.spinner_style,
        if (Premium.premiumIsActive()) FaceClassifierProcessor.allClassificationDescriptions(this) else FaceClassifierProcessor.allClassificationDescriptionsFree(this)
      )
      featureSpinnerDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      featureSpinner.adapter = featureSpinnerDataAdapter
      featureSpinner.setSelection(FaceClassifierProcessor.Classifier.values().indexOf(Settings.selectedClassifier))
    }
  }

  public override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    saveState()
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    scaleGestureDetector.onTouchEvent(event)
    panGestureDetector.onTouchEvent(event)

    if (scrolling && (event!!.action == MotionEvent.ACTION_UP)) {
      scrolling = false
      classifyDisplayedImage()
    }
    return true
  }

  private fun getBitmapOfDisplayedImage(): Bitmap? {
    // Still loading the view
    if (preview!!.drawable == null){
      return null
    }
    val drawable = preview!!.drawable as BitmapDrawable
    val scaledWidth = (scaleFactor * drawable.intrinsicWidth).toInt()
    val scaledHeight = (scaleFactor * drawable.intrinsicHeight).toInt()

    // Still loading the view
    if (scaledHeight == -1 || scaledWidth == -1){
      return null
    }

    val bitmap = Bitmap.createScaledBitmap(drawable.bitmap, scaledWidth, scaledHeight, true)
    val scaledAndPositioned = Bitmap.createBitmap(
      drawable.intrinsicWidth + 2 * preview!!.left,
      drawable.intrinsicHeight + 2 * preview!!.top,
      bitmap.config
    )
    val canvas = Canvas(scaledAndPositioned)
    canvas.drawColor(Color.DKGRAY)
    canvas.drawBitmap(
      bitmap,
      preview!!.x + (drawable.intrinsicWidth - scaledWidth) / 2,
      preview!!.y + (drawable.intrinsicHeight - scaledHeight) / 2,
      null
    )
    return scaledAndPositioned
  }

  private fun takePicture() { // Clean up last time's image
    try {
      loadImage(null)

      val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      if (takePictureIntent.resolveActivity(packageManager) != null) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.new_picture_title))
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.from_camera_description))
        cameraImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

        imageFromPhotoResultLauncher?.launch(takePictureIntent)
      }
    }
    catch (e: java.lang.Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_connect_to_camera_exception_message), TAG, e)
    }
  }

  private fun browseForImage() {
    try {
      val intent = Intent()
      intent.type = "image/*"
      intent.action = Intent.ACTION_GET_CONTENT

      localImageResultLauncher?.launch(Intent.createChooser(intent, getString(R.string.select_picture_title)))
    }
    catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_launch_image_browser_exception), TAG, e)
    }
  }

  private fun showPremiumStatus() {
    try {
      premiumStatusResultLauncher?.launch(Intent(applicationContext, PremiumStatusActivity::class.java))
    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_show_premium_status_dialog_exception),
        TAG, e)
    }
  }

  private fun showPreferences(){
    try {
      val intent = Intent(applicationContext, PreferencesActivity::class.java)
      preferencesResultLauncher?.launch(Intent.createChooser(intent, getString(R.string.preferences_title)))
    } catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_show_preferences_exception), TAG, e)
    }
  }

  private fun tryLoadAndClassifyImage() {
    Log.d(
      TAG,
      "Try load and classify image"
    )
    try {
      if (!imageExists) {
        return
      }

      if (imageMaxWidth == 0 || preview == null) {
        // UI layout has not finished yet, will reload once it's ready.
        return
      }

      val imageBitmap = ImageUtils.getImageFromCache(this, CLASSIFIED_IMAGE_NAME, Bitmap.CompressFormat.PNG)?: return

      loadImage(BitmapScaler.scaleBitmap(imageBitmap, 1.0f, imageMaxWidth, imageMaxHeight))

      // Post to give the UI time to position itself
      Handler(Looper.getMainLooper()).post{ classifyDisplayedImage() }

    } catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_load_and_classify_image_exception), TAG, e)
    }
  }

  private fun loadImage(scaledBitmap: Bitmap?) {
    if (preview == null) return

    try {
      preview!!.setImageBitmap(scaledBitmap)
    }
    catch (e: Exception){
      ExceptionHandler.alert(this, getString(R.string.failed_to_update_image_preview_exception), TAG, e)
    }
  }


  internal fun classifyDisplayedImage(){
    try {
      classifyImage(getBitmapOfDisplayedImage())
    }
    catch (e: Exception) {
      ExceptionHandler.alert(this, getString(R.string.failed_to_classify_displayed_image_exception), TAG, e)
    }
  }

  private fun classifyImage(scaledBitmap: Bitmap?) {
    if (scaledBitmap == null){
      return
    }

    if (imageProcessor != null) {
        graphicOverlay!!.setImageSourceInfo(
          scaledBitmap.width, scaledBitmap.height, /* isFlipped= */false
      )
      graphicOverlay!!.clear()
      imageProcessor!!.resetClassificationTracker()
      imageProcessor!!.processBitmap(scaledBitmap, graphicOverlay!!)
    } else {
      ExceptionHandler.alert(this, getString(R.string.image_process_is_null_exception), TAG, NullPointerException())
    }
  }

  internal fun createImageProcessor() {
    try {
      imageProcessor = FaceDetectorProcessor(this, 0)
    }
    catch (e: Exception) {
      val message = getString(R.string.failed_to_create_image_processor_exception)
      ExceptionHandler.alert(applicationContext, message + " '${FaceClassifierProcessor.classifier}'", TAG, e)
    }
  }

  private inner class PanListener: GestureDetector.SimpleOnGestureListener(){

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
      preview!!.x -= distanceX
      preview!!.y -= distanceY

      scrolling = true

      return false
    }
  }

  private inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
      val newScaleFactor = if (scaleGestureDetector.scaleFactor < 1){
        scaleFactor * (1.0f - 0.1f * (1.0f - scaleGestureDetector.scaleFactor))
      } else {
        scaleFactor * (1.0f + 0.1f * (scaleGestureDetector.scaleFactor - 1.0f))
      }
      scaleFactor = max(0.1f, min(newScaleFactor, 10.0f))
      return false
    }
  }

  companion object {
    private const val TAG = "StillImageActivity"
    const val GET_IMAGE_FROM = "getImageFrom"
    const val GET_IMAGE_FROM_CAMERA = "camera"
    const val GET_IMAGE_FROM_IMAGE_STORE = "imageStore"
    const val SHARED_IMAGE_NAME = "shared"
    const val CLASSIFIED_IMAGE_NAME = "image"
  }
}
