package com.example.evcharger.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.evcharger.R
import kotlinx.coroutines.*

/**
 * ErrorBarManager - Manages the display of error bars at the top of the screen
 * Handles network errors, API errors, and auto-dismissal with animations
 * 
 * @property errorBarView The root view of the error bar layout
 */
class ErrorBarManager(private val errorBarView: View) {

    private val errorText: TextView = errorBarView.findViewById(R.id.txtErrorMessage)
    private val errorIcon: View = errorBarView.findViewById(R.id.imgErrorIcon)
    
    private var dismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentAnimator: ObjectAnimator? = null

    /**
     * Enum representing different types of errors with specific behaviors
     */
    enum class ErrorType {
        /** Network completely disconnected - persistent display */
        NETWORK_DISCONNECTED,
        /** Network attempting to reconnect - persistent display */
        NETWORK_RECONNECTING,
        /** Network successfully connected - auto-dismiss after 2s */
        NETWORK_CONNECTED,
        /** API/Backend error - auto-dismiss after 4s */
        API_ERROR,
        /** General error - auto-dismiss after 3s */
        GENERAL_ERROR
    }

    /**
     * Show error bar with message and type
     * 
     * @param message Error message to display
     * @param type Type of error (affects color and auto-dismiss behavior)
     * @param autoDismiss Whether to automatically hide after delay
     * @param dismissAfterMs Delay before auto-dismiss (milliseconds)
     */
    fun showError(
        message: String, 
        type: ErrorType = ErrorType.GENERAL_ERROR, 
        autoDismiss: Boolean = false, 
        dismissAfterMs: Long = DEFAULT_DISMISS_DURATION
    ) {
        // Cancel any pending dismissal
        dismissJob?.cancel()
        
        // Cancel any running animation
        currentAnimator?.cancel()

        // Update UI based on error type
        updateErrorAppearance(message, type)

        // Show with animation if not already visible
        if (errorBarView.visibility != View.VISIBLE) {
            slideDown()
        }

        // Auto-dismiss if requested
        if (autoDismiss) {
            dismissJob = scope.launch {
                delay(dismissAfterMs)
                hide()
            }
        }
    }
    
    /**
     * Update error bar appearance based on type
     */
    private fun updateErrorAppearance(message: String, type: ErrorType) {
        val (backgroundColor, iconVisibility) = when (type) {
            ErrorType.NETWORK_DISCONNECTED -> R.color.error to View.VISIBLE
            ErrorType.NETWORK_RECONNECTING -> R.color.warning to View.VISIBLE
            ErrorType.NETWORK_CONNECTED -> R.color.success to View.VISIBLE
            ErrorType.API_ERROR -> R.color.error to View.VISIBLE
            ErrorType.GENERAL_ERROR -> R.color.error to View.VISIBLE
        }
        
        errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, backgroundColor))
        errorText.text = message
        errorIcon.visibility = iconVisibility
    }

    /**
     * Show network disconnected error (persistent display)
     */
    fun showNetworkDisconnected() {
        showError("No internet connection", ErrorType.NETWORK_DISCONNECTED, autoDismiss = false)
    }

    /**
     * Show network reconnecting message (persistent display)
     */
    fun showNetworkReconnecting() {
        showError("Trying to reconnect...", ErrorType.NETWORK_RECONNECTING, autoDismiss = false)
    }

    /**
     * Show network connected message (auto-dismisses after 2 seconds)
     */
    fun showNetworkConnected() {
        showError("Connected", ErrorType.NETWORK_CONNECTED, autoDismiss = true, dismissAfterMs = NETWORK_CONNECTED_DURATION)
    }

    /**
     * Show API error (auto-dismisses after 4 seconds for technical messages)
     * 
     * @param message The error message from the API
     */
    fun showApiError(message: String) {
        showError(message, ErrorType.API_ERROR, autoDismiss = true, dismissAfterMs = API_ERROR_DURATION)
    }

    /**
     * Show general error message
     * 
     * @param message The error message to display
     * @param autoDismiss Whether to automatically hide after default duration
     */
    fun showGeneralError(message: String, autoDismiss: Boolean = true) {
        showError(message, ErrorType.GENERAL_ERROR, autoDismiss, dismissAfterMs = DEFAULT_DISMISS_DURATION)
    }

    /**
     * Hide error bar with animation
     * Safe to call multiple times
     */
    fun hide() {
        if (errorBarView.visibility == View.VISIBLE) {
            slideUp()
        }
        dismissJob?.cancel()
    }

    /**
     * Slide down animation (show)
     */
    private fun slideDown() {
        errorBarView.visibility = View.VISIBLE
        errorBarView.translationY = -errorBarView.height.toFloat()
        
        currentAnimator = ObjectAnimator.ofFloat(
            errorBarView, 
            "translationY", 
            -errorBarView.height.toFloat(), 
            0f
        ).apply {
            duration = ANIMATION_DURATION
            start()
        }
    }

    /**
     * Slide up animation (hide)
     */
    private fun slideUp() {
        currentAnimator = ObjectAnimator.ofFloat(
            errorBarView, 
            "translationY", 
            0f, 
            -errorBarView.height.toFloat()
        ).apply {
            duration = ANIMATION_DURATION
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    errorBarView.visibility = View.GONE
                    currentAnimator = null
                }
            })
            start()
        }
    }

    /**
     * Check if error bar is currently visible
     * 
     * @return true if error bar is visible, false otherwise
     */
    fun isVisible(): Boolean = errorBarView.visibility == View.VISIBLE

    /**
     * Cleanup resources when no longer needed
     * Cancels all pending animations and coroutines
     * Call this in Activity's onDestroy()
     */
    fun cleanup() {
        dismissJob?.cancel()
        currentAnimator?.cancel()
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "ErrorBarManager"
        
        /** Default duration for error messages (3 seconds) */
        private const val DEFAULT_DISMISS_DURATION = 3000L
        
        /** Duration for network connected message (2 seconds - shorter since it's good news) */
        private const val NETWORK_CONNECTED_DURATION = 2000L
        
        /** Duration for API errors (4 seconds - longer for technical messages) */
        private const val API_ERROR_DURATION = 4000L
        
        /** Animation duration for slide in/out (300ms) */
        private const val ANIMATION_DURATION = 300L
    }
}
