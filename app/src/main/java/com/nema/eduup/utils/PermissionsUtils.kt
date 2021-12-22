package com.nema.eduup.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.lang.Exception


object PermissionsUtils {

    fun checkStoragePermissions(context: Context, callback: PermissionsCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageStoragePermission(context, object : PermissionsCallback {
                override fun onPermissionRequest(granted: Boolean) {
                    if (granted) {
                        callback.onPermissionRequest(granted = true)
                    }
                    else {
                        callback.onPermissionRequest(granted = true)
                    }
                }

            })
        }else {
            requestReadStoragePermission(context, callback)
        }
    }

    fun requestWriteStoragePermission(context: Context, callback: PermissionsCallback) {
        requestSinglePermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, callback)
    }

    fun requestReadStoragePermission(context: Context, callback: PermissionsCallback) {
        requestSinglePermission(context, Manifest.permission.READ_EXTERNAL_STORAGE, callback)
    }

    fun requestManageStoragePermission(context: Context, callback: PermissionsCallback) {
        requestSinglePermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE, callback)
    }

    private fun requestSinglePermission(context: Context, permission: String, callback: PermissionsCallback) {
        Dexter.withActivity(context as Activity) // Cast the context to Activity
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    // User has granted the permission
                    callback.onPermissionRequest(granted = true)
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    // User previously denied the permission, request them again
                    token?.continuePermissionRequest()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    // User has denied the permission
                    callback.onPermissionRequest(granted = false)
                }
            })
    }
}

interface PermissionsCallback {

    // Pass request granted status i.e true or false
    fun onPermissionRequest(granted: Boolean)

}