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

package com.thomasjbarrerasconsulting.faces

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.thomasjbarrerasconsulting.faces.databinding.ActivityVisionEntryChoiceBinding
import com.thomasjbarrerasconsulting.faces.java.ChooserActivity

class EntryChoiceActivity : AppCompatActivity() {

  private lateinit var binding: ActivityVisionEntryChoiceBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityVisionEntryChoiceBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    binding.javaEntryPoint.setOnClickListener {
      val intent = Intent(this@EntryChoiceActivity, ChooserActivity::class.java)
      startActivity(intent)
    }

    binding.kotlinEntryPoint.setOnClickListener {
      val intent =
        Intent(
          this@EntryChoiceActivity,
          com.thomasjbarrerasconsulting.faces.kotlin.ChooserActivity::class.java
        )
      startActivity(intent)
    }
  }
}
