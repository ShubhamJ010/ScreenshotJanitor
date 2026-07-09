package dev.sj010.ssjanitor.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralizes the storage-permission decision so the runtime *check* and the
 * *request* can never diverge (they previously both hardcoded
 * [Manifest.permission.READ_MEDIA_IMAGES], which does not exist below API 33
 * and left API 29-32 devices stuck in a permanent "Permissions Required" state).
 *
 * - API 33+ (TIRAMISU): [Manifest.permission.READ_MEDIA_IMAGES]
 * - API 29-32: [Manifest.permission.READ_EXTERNAL_STORAGE]
 */
object StoragePermissions {

    fun requiredStoragePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasStoragePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, requiredStoragePermission()) ==
            PackageManager.PERMISSION_GRANTED
}
