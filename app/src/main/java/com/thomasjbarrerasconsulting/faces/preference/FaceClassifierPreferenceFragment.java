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

package com.thomasjbarrerasconsulting.faces.preference;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.thomasjbarrerasconsulting.faces.R;

/** Configures live preview demo settings. */
public class FaceClassifierPreferenceFragment extends android.preference.PreferenceFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preference_live_preview_quickstart);
    setUpCameraPreferences();
    setUpFaceDetectionPreferences();
  }

  void setUpCameraPreferences() {
    PreferenceCategory cameraPreference =
        (PreferenceCategory) findPreference(getString(R.string.pref_category_key_camera));
    cameraPreference.removePreference(
        findPreference(getString(R.string.pref_key_camerax_rear_camera_target_resolution)));
    cameraPreference.removePreference(
        findPreference(getString(R.string.pref_key_camerax_front_camera_target_resolution)));
  }

  private void setUpFaceDetectionPreferences() {
    setUpListPreference(R.string.pref_key_live_preview_face_detection_landmark_mode);
    setUpListPreference(R.string.pref_key_live_preview_face_detection_contour_mode);
    setUpListPreference(R.string.pref_key_live_preview_face_detection_classification_mode);
    setUpListPreference(R.string.pref_key_live_preview_face_detection_performance_mode);

    EditTextPreference minFaceSizePreference =
        (EditTextPreference)
            findPreference(getString(R.string.pref_key_live_preview_face_detection_min_face_size));
    minFaceSizePreference.setSummary(minFaceSizePreference.getText());
    minFaceSizePreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          try {
            float minFaceSize = Float.parseFloat((String) newValue);
            if (minFaceSize >= 0.0f && minFaceSize <= 1.0f) {
              minFaceSizePreference.setSummary((String) newValue);
              return true;
            }
          } catch (NumberFormatException e) {
            // Fall through intentionally.
          }

          Toast.makeText(
                  getActivity(), R.string.pref_toast_invalid_min_face_size, Toast.LENGTH_LONG)
              .show();
          return false;
        });
  }

  private void setUpListPreference(@StringRes int listPreferenceKeyId) {
    ListPreference listPreference = (ListPreference) findPreference(getString(listPreferenceKeyId));
    listPreference.setSummary(listPreference.getEntry());
    listPreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          int index = listPreference.findIndexOfValue((String) newValue);
          listPreference.setSummary(listPreference.getEntries()[index]);
          return true;
        });
  }
}
