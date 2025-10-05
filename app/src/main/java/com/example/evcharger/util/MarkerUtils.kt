package com.example.evcharger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.example.evcharger.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

object MarkerUtils {

    /**
     * Creates a dynamic EV-station marker bitmap.
     *
     * @param context  Context used to inflate layout
     * @param label    Charging type or station name ("AC 22 kW", "DC 50 kW", etc.)
     * @param status   Station status ("available", "busy", "offline")
     * @param color    Optional override color (takes precedence over status color)
     */
    fun createEvMarkerBitmap(
        context: Context,
        label: String? = null,
        status: String? = null,
        @ColorInt color: Int? = null
    ): BitmapDescriptor {

    val inflater = LayoutInflater.from(context)
    // Use the marker layout that exists in the project
    val view = inflater.inflate(R.layout.marker_station, null)

    val bg = view.findViewById<ImageView?>(R.id.imgMarkerBg)
    // icon may not be present in this layout; keep it optional
    val icon = view.findViewById<ImageView?>(R.id.imgChargerIcon)
    val labelText = view.findViewById<TextView?>(R.id.txtStationLabel)

        // Label (e.g., "AC 22 kW")
        labelText?.text = label ?: ""

        // Background tint based on status or manual color. Use null-safe checks.
        val bgColor = when {
            color != null -> color
            status?.equals("available", ignoreCase = true) == true ->
                ContextCompat.getColor(context, R.color.ev_available)
            status?.equals("busy", ignoreCase = true) == true ->
                ContextCompat.getColor(context, R.color.ev_busy)
            status?.equals("offline", ignoreCase = true) == true ->
                ContextCompat.getColor(context, R.color.ev_offline)
            else -> ContextCompat.getColor(context, R.color.ev_available)
        }
        bg?.setColorFilter(bgColor)

        // Tint icon white for contrast if present
        icon?.setColorFilter(ContextCompat.getColor(context, android.R.color.white))

        // Properly measure and draw
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = createBitmap(view.measuredWidth, view.measuredHeight)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
