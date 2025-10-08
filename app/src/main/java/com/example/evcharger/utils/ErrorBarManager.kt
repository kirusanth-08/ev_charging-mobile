package com.example.evcharger.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.evcharger.R
import kotlinx.coroutines.*

/**
 * ErrorBarManager - Manages the display of error bars at the top of the screen
 * Handles network errors, API errors, and auto-dismissal with animations
 */
class ErrorBarManager(private val errorBarView: View) {

    private val errorText: TextView = errorBarView.findViewById(R.id.txtErrorMessage)
    private val errorIcon: View = errorBarView.findViewById(R.id.imgErrorIcon)
    
    private var dismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    enum class ErrorType {
        NETWORK_DISCONNECTED,
        NETWORK_RECONNECTING,
        NETWORK_CONNECTED,
        API_ERROR,
        GENERAL_ERROR
    }

    /**
     * Show error bar with message and type
     */
    fun showError(message: String, type: ErrorType = ErrorType.GENERAL_ERROR, autoDismiss: Boolean = false, dismissAfterMs: Long = 3000) {
        // Cancel any pending dismissal
        dismissJob?.cancel()

        // Update UI based on error type
        when (type) {
            ErrorType.NETWORK_DISCONNECTED -> {
                errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, R.color.error))
                errorText.text = message
                errorIcon.visibility = View.VISIBLE
            }
            ErrorType.NETWORK_RECONNECTING -> {
                errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, R.color.warning))
                errorText.text = message
                errorIcon.visibility = View.VISIBLE
            }
            ErrorType.NETWORK_CONNECTED -> {
                errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, R.color.success))
                errorText.text = message
                errorIcon.visibility = View.VISIBLE
            }
            ErrorType.API_ERROR -> {
                errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, R.color.error))
                errorText.text = message
                errorIcon.visibility = View.VISIBLE
            }
            ErrorType.GENERAL_ERROR -> {
                errorBarView.setBackgroundColor(ContextCompat.getColor(errorBarView.context, R.color.error))
                errorText.text = message
                errorIcon.visibility = View.VISIBLE
            }
        }

        // Show with animation
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
     * Show network disconnected error
     */
    fun showNetworkDisconnected() {
        showError("No internet connection", ErrorType.NETWORK_DISCONNECTED, autoDismiss = false)
    }

    /**
     * Show network reconnecting message
     */
    fun showNetworkReconnecting() {
        showError("Trying to reconnect...", ErrorType.NETWORK_RECONNECTING, autoDismiss = false)
    }

    /**
     * Show network connected and auto-dismiss
     */
    fun showNetworkConnected() {
        showError("Connected", ErrorType.NETWORK_CONNECTED, autoDismiss = true, dismissAfterMs = 2000)
    }

    /**
     * Show API error
     */
    fun showApiError(message: String) {
        showError(message, ErrorType.API_ERROR, autoDismiss = true, dismissAfterMs = 4000)
    }

    /**
     * Show general error
     */
    fun showGeneralError(message: String, autoDismiss: Boolean = true) {
        showError(message, ErrorType.GENERAL_ERROR, autoDismiss, dismissAfterMs = 3000)
    }

    /**
     * Hide error bar with animation
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
        
        ObjectAnimator.ofFloat(errorBarView, "translationY", -errorBarView.height.toFloat(), 0f).apply {
            duration = 300
            start()
        }
    }

    /**
     * Slide up animation (hide)
     */
    private fun slideUp() {
        ObjectAnimator.ofFloat(errorBarView, "translationY", 0f, -errorBarView.height.toFloat()).apply {
            duration = 300
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    errorBarView.visibility = View.GONE
                }
            })
            start()
        }
    }

    /**
     * Check if error bar is currently visible
     */
    fun isVisible(): Boolean = errorBarView.visibility == View.VISIBLE

    /**
     * Cleanup resources
     */
    fun cleanup() {
        dismissJob?.cancel()
        scope.cancel()
    }
}
