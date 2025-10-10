package com.example.evcharger.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.evcharger.R

/**
 * Utility class for managing status bar appearance across the app.
 * Provides methods for transparent, colored, and edge-to-edge status bars.
 * Uses modern WindowCompat and WindowInsetsControllerCompat APIs.
 */
object StatusBarUtil {
    
    /**
     * Make status bar transparent (default modern design)
     * 
     * @param activity The activity to apply the status bar to
     * @param lightIcons True for dark icons (light background), false for light icons (dark background)
     */
    fun makeTransparent(activity: Activity, lightIcons: Boolean = true) {
        setStatusBarAppearance(activity, android.R.color.transparent, lightIcons)
    }
    
    /**
     * Set status bar to primary green color
     * Used for screens with green headers (Operator Dashboard, Booking List)
     * 
     * @param activity The activity to apply the status bar to
     */
    fun setGreen(activity: Activity) {
        setStatusBarAppearance(activity, R.color.primary, lightIcons = false)
    }
    
    /**
     * Set status bar to black color
     * Used for full-screen camera (QR Scanner)
     * 
     * @param activity The activity to apply the status bar to
     */
    fun setBlack(activity: Activity) {
        setStatusBarAppearance(activity, android.R.color.black, lightIcons = false)
    }
    
    /**
     * Set status bar to surface color (matches background)
     * 
     * @param activity The activity to apply the status bar to
     * @param isLightMode True for light mode, false for dark mode
     */
    fun setSurface(activity: Activity, isLightMode: Boolean = true) {
        val colorRes = if (isLightMode) R.color.surface_light else R.color.surface_dark
        setStatusBarAppearance(activity, colorRes, lightIcons = isLightMode)
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
     * Set status bar appearance with color and icon style
     * Uses modern WindowCompat APIs for setting status bar color and icon appearance
     * 
     * @param activity The activity to apply the status bar appearance to
     * @param colorRes The color resource ID for the status bar
     * @param lightIcons True for dark icons (on light backgrounds), false for light icons (on dark backgrounds)
     */
    private fun setStatusBarAppearance(activity: Activity, @ColorRes colorRes: Int, lightIcons: Boolean) {
        val window = activity.window
        val color = ContextCompat.getColor(activity, colorRes)
        
        // Set status bar color using modern Window API
        // Note: This is the recommended way as of Android SDK 35+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
        
        // Set icon color using WindowInsetsControllerCompat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = lightIcons
        }
    }
}
