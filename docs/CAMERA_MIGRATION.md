# Camera SDK Migration — Testing Strategy

The camera SDK currently used by the app is deprecated; upgrading will be a
major refactor of `CameraSource.java`, `CameraHandler.kt`, `CameraSourcePreview.java`,
and the frame-processing pipeline that feeds `FaceDetectorProcessor` and the
overlay graphics.

This document captures the testing plan to make that migration safe. The guiding
principle: **test at the seam, not inside the thing being replaced.** Tests
coupled to the old camera API become dead weight the day the rewrite starts.

## Recommended order

1. **Seam refactor (do this first, before touching the new SDK).**
   Introduce a camera-agnostic interface — e.g. `FrameSource` emitting
   `{bitmap, rotation, timestamp}` — and have the current `CameraSource`
   implement it. Rebind downstream code (face detection, classifiers, overlay
   rendering) to the seam, not the camera. The rewrite then swaps the
   implementation without disturbing the rest of the pipeline or its tests.

2. **Characterization tests against the seam.**
   Capture *observable* behavior of the current pipeline: given a sequence of
   known input frames, record the face detections, classifications, and overlay
   draw calls produced. Save outputs as goldens. Run the same tests against the
   new implementation during migration. This is the single highest-value
   artifact pre-rewrite.

3. **Fixture-image regression tests for historical crashes.**
   Bundle sample frames in `app/src/test/resources/` covering the failure modes
   this app has actually hit:
   - Nullable `Bitmap.config` (commit `c406abc`)
   - Face tracking box position for still images (commit `b800b84`)
   - Capture-from-live-view path (commit `281a89e`)
   - SDK 35 edge-to-edge layout, ANRs (commit `a206bcf`)

   Each past incident becomes one pinned test. These are camera-SDK-agnostic
   by construction and survive the rewrite.

4. **Instrumented smoke tests across a device matrix.**
   Camera bugs cluster by OEM and API level — no emulator will find them all.
   Use Firebase Test Lab (or the `reactivecircus/android-emulator-runner`
   GitHub Action for cheaper coverage) to run a short scripted flow across
   several devices/API levels:
   - Launch app → grant camera permission → detect a face in a still image →
     capture from live preview → verify no crash and overlay renders.

   Essential for camera rewrites specifically. Skip for most other work.

5. **Staged rollout with Crashlytics as the real test suite.**
   For a 100K-download app, the long tail of device-specific camera bugs will
   only surface in production. Ship the new pipeline behind a remote config
   flag, roll out 1% → 10% → 100%, and watch Crashlytics/ANR dashboards. If
   possible, run old and new implementations in shadow mode on real devices
   and compare outputs before flipping the flag.

## What NOT to do

- **Don't write Robolectric tests for the camera layer.** Robolectric can't
  meaningfully simulate camera surfaces, `SurfaceTexture`, or the preview
  lifecycle. Time spent mocking these is time that buys no confidence.
  Robolectric remains fine for the classifier / utility / preference tests
  already in the suite.

- **Don't pin tests to the old SDK's types or callback shapes.** Any test
  that imports from the deprecated camera API should be considered disposable
  and isolated from the seam-level tests.

- **Don't rely on the existing JVM unit test suite to catch camera bugs.**
  The current ~18 unit tests cover deterministic post-processing (classifier
  outputs, geometry, collections). The entire runtime pipeline from camera
  frames through bitmap conversion to overlay rendering has no coverage today,
  and none of the last four fix-commits would have been caught by these tests.

## Existing test coverage (context)

Unit tests today cover: TFLite classifier output processing (9 tests),
`ClassificationTracker`, `BitmapScaler`, `FaceBoundingBoxExpander`,
`ScopedExecutor`, `FrameMetadata`, `ObservableList`, `DrawingUtils`,
`ThreadSafeTaskMap`, `Premium` status derivation, `UserPreferences`.

Not covered: Activities, the camera pipeline, `BitmapUtils`/`ImageUtils`/
`BitmapCropper`, `FaceDetectorProcessor` orchestration, `BillingHandler`,
`FaceGraphic`/`GraphicOverlay` rendering, and all system integration
(permissions, ads, analytics, sharing).

The biggest ROI gaps to fill before the migration: fixture-based tests for
`BitmapUtils`/`ImageUtils`/`BitmapCropper` (these have crashed recently and
are pure pixel math — easy to test at the JVM layer).
