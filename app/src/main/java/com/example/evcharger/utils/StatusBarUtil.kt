package com.example.evcharger.utils

import android.app.Activity
import android.os.Build
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.evcharger.R

/**
 * Utility class for managing status bar appearance across the app.
 * Provides methods for transparent, colored, and edge-to-edge status bars.
 */
object StatusBarUtil {
    
    /**
     * Make status bar transparent (default modern design)
     * 
     * @param activity The activity to apply the status bar to
     * @param lightIcons True for dark icons (light background), false for light icons (dark background)
     */
    fun makeTransparent(activity: Activity, lightIcons: Boolean = true) {
        activity.window.statusBarColor = ContextCompat.getColor(
            activity, 
            android.R.color.transparent
        )
        setIconColor(activity, lightIcons)
    }
    
    /**
     * Set status bar to primary green color
     * Used for screens with green headers (Operator Dashboard, Booking List)
     * 
     * @param activity The activity to apply the status bar to
     */
    fun setGreen(activity: Activity) {
        activity.window.statusBarColor = ContextCompat.getColor(
            activity, 
            R.color.primary
        )
        setIconColor(activity, lightIcons = false)
    }
    
    /**
     * Set status bar to black color
     * Used for full-screen camera (QR Scanner)
     * 
     * @param activity The activity to apply the status bar to
     */
    fun setBlack(activity: Activity) {
        activity.window.statusBarColor = ContextCompat.getColor(
            activity, 
            android.R.color.black
        )
        setIconColor(activity, lightIcons = false)
    }
    
    /**
     * Set status bar to surface color (matches background)
     * 
     * @param activity The activity to apply the status bar to
     * @param isLightMode True for light mode, false for dark mode
     */
    fun setSurface(activity: Activity, isLightMode: Boolean = true) {
        val colorRes = if (isLightMode) R.color.surface_light else R.color.surface_dark
        activity.window.statusBarColor = ContextCompat.getColor(activity, colorRes)
        setIconColor(activity, lightIcons = isLightMode)
    }
    
    /**
     * Enable edge-to-edge mode (content extends behind system bars)
     * Use this for immersive screens like maps or full-screen images
     * 
     * @param activity The activity to apply edge-to-edge mode to
     */
    fun enableEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
    
    /**
     * Disable edge-to-edge mode (content respects system bars)
     * This is the default behavior
     * 
     * @param activity The activity to disable edge-to-edge mode for
     */
    fun disableEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    }
    
    /**
     * Set status bar icon color
     * 
     * @param activity The activity to apply the icon color to
     * @param lightIcons True for dark icons (on light backgrounds), false for light icons (on dark backgrounds)
     */
    private fun setIconColor(activity: Activity, lightIcons: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars = lightIcons
        } else {
            // Fallback for API < 23: Use dark status bar since light icons not available
            if (lightIcons) {
                activity.window.statusBarColor = ContextCompat.getColor(
                    activity, 
                    R.color.primary_dark
                )
            }
        }
    }
}
