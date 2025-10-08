package com.example.evcharger.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object LocationUtils {
    
    /**
     * Check if GPS/Location services are enabled on the device
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Show a dialog prompting user to enable location services
     * @param activity The activity context
     * @param onEnabled Callback when user agrees to enable location
     * @param onCancelled Callback when user cancels
     */
    fun showEnableLocationDialog(
        activity: Activity,
        onEnabled: (() -> Unit)? = null,
        onCancelled: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Location Services Disabled")
            .setMessage("To find nearby charging stations, please enable location services on your device.\n\nWould you like to enable it now?")
            .setIcon(android.R.drawable.ic_dialog_map)
            .setPositiveButton("Enable") { dialog, _ ->
                // Open location settings
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivity(intent)
                onEnabled?.invoke()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                onCancelled?.invoke()
                dialog.dismiss()
            }
            .setCancelable(true)
            .setOnCancelListener {
                onCancelled?.invoke()
            }
            .show()
    }
    
    /**
     * Check if location is enabled, if not show dialog
     * @return true if location is enabled, false otherwise
     */
    fun checkAndPromptLocationEnabled(activity: Activity, onProceed: () -> Unit): Boolean {
        return if (isLocationEnabled(activity)) {
            onProceed()
            true
        } else {
            showEnableLocationDialog(
                activity,
                onEnabled = {
                    // User will manually return to app after enabling
                },
                onCancelled = {
                    android.widget.Toast.makeText(
                        activity,
                        "Location is required to find charging stations",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
            false
        }
    }
}
