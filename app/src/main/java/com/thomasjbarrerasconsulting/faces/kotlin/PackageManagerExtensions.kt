/*
 * Copyright 2023 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

class PackageManagerExtensions {
    companion object {
        fun PackageManager.getPackageInfoCompat(
            packageName: String,
            getPermissions: Int = 0
        ): PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(getPermissions.toLong())
                )
            } else {
                @Suppress("DEPRECATION") getPackageInfo(packageName, getPermissions)
            }
    }
}