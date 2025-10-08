package com.example.evcharger.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.evcharger.R
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.util.MarkerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shows Google Map with nearby EV charging stations.
 */
class MapsFragment : Fragment(R.layout.fragment_maps) {

    private var googleMap: GoogleMap? = null
    private val currentMarkers = mutableListOf<Marker>()

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                enableMyLocationAndLoad()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            ensureLocationPermissionThenLoad()
        }
    }

    private fun ensureLocationPermissionThenLoad() {
        val ctx = requireContext()
        val fineGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            enableMyLocationAndLoad()
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    private fun enableMyLocationAndLoad() {
        val ctx = requireContext()
        if (
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            fused.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    val pos = LatLng(it.latitude, it.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13f))
                }
            }
            loadNearby()
        }
    }

    private fun loadNearby() {
        val ctx = requireContext()
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        if (
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val res = RetrofitClient.api.getNearbyStations(loc.latitude, loc.longitude, 10)
                    if (res.isSuccessful && res.body()?.data != null) {
                        val stations = res.body()!!.data!!

                        requireActivity().runOnUiThread {
                            // Clear old markers
                            currentMarkers.forEach { it.remove() }
                            currentMarkers.clear()

                            // Add new station markers
                            stations.forEach { bs ->
                                val backend = bs.station
                                val appStation = com.example.evcharger.model.Station(
                                    id = backend.stationId,
                                    name = backend.name,
                                    latitude = backend.location.latitude,
                                    longitude = backend.location.longitude,
                                    address = backend.location.address ?: backend.location.city ?: "",
                                    connectorTypes = backend.slots.map { it.connectorType },
                                    chargingPowerKw = backend.slots.firstOrNull()?.powerRating,
                                    status = backend.type,  // e.g., "available", "busy", "offline"
                                    lastUpdated = backend.updatedAt,
                                    distanceMeters = bs.distanceKm?.let { (it * 1000).toInt() }
                                )

                                // 💡 Label shows power type (e.g., "DC 50kW")
                                val powerLabel = appStation.chargingPowerKw?.let { "${it.toInt()}kW" }
                                    ?: appStation.connectorTypes.firstOrNull()
                                    ?: "EV"

                                // ⚡ Marker color depends on station status
                                val markerIcon = MarkerUtils.createEvMarkerBitmap(
                                    context = requireContext(),
                                    label = powerLabel,
                                    status = appStation.status
                                )

                                val marker = googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(appStation.latitude, appStation.longitude))
                                        .title(appStation.name)
                                        .icon(markerIcon)
                                )

                                marker?.let {
                                    it.tag = appStation
                                    currentMarkers.add(it)
                                }
                            }

                            // Handle marker click → open bottom sheet
                            googleMap?.setOnMarkerClickListener { marker ->
                                val st = marker.tag as? com.example.evcharger.model.Station
                                st?.let { station ->
                                    val sheet = StationDetailsBottomSheet.newInstance(station)
                                    sheet.show(childFragmentManager, "station_details")
                                    true
                                } ?: false
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Public helper to center the map on the device's last known location.
     * If permissions are not granted this will trigger the permission flow.
     */
    fun centerOnCurrentLocation() {
        val ctx = requireContext()
        val fineGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            // ask for permission; when granted the permissionLauncher will call enableMyLocationAndLoad()
            permissionLauncher.launch(locationPermissions)
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                val pos = LatLng(it.latitude, it.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            }
        }
    }
    
    /**
     * Refresh map if location is now enabled (called when returning from settings)
     */
    fun refreshIfLocationEnabled() {
        val ctx = context ?: return
        val locationManager = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        
        if (isLocationEnabled) {
            // Check if we have permission
            val fineGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (fineGranted || coarseGranted) {
                enableMyLocationAndLoad()
            }
        }
    }
}
