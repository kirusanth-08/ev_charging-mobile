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
 * Supports two view modes:
 * 1. Nearby Mode: Shows stations within radius with distance calculation
 * 2. All Stations Mode: Shows all available stations (no distance)
 */
class MapsFragment : Fragment(R.layout.fragment_maps) {

    private var googleMap: GoogleMap? = null
    private val currentMarkers = mutableListOf<Marker>()
    private var isShowingAllStations = false  // Toggle between nearby and all stations

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
            // Load stations based on current mode
            if (isShowingAllStations) {
                loadAllStations()
            } else {
                loadNearby()
            }
        }
    }
    
    /**
     * Toggle between nearby and all stations view
     */
    fun toggleStationView() {
        isShowingAllStations = !isShowingAllStations
        
        if (isShowingAllStations) {
            loadAllStations()
        } else {
            loadNearby()
        }
        
        // Notify activity to update toggle button text
        (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.updateToggleButtonText(isShowingAllStations)
    }
    
    /**
     * Refresh stations data from the server
     */
    fun refreshStations() {
        if (isShowingAllStations) {
            loadAllStations()
        } else {
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
                // Show loading indicator
                (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.showLoadingIndicator()
                
                CoroutineScope(Dispatchers.IO).launch {
                    // Use repository with offline caching support
                    val repo = com.example.evcharger.repository.ReservationRepository(requireContext())
                    val res = repo.getNearby(loc.latitude, loc.longitude)
                    
                    if (res.isSuccessful && res.body()?.data != null) {
                        val nearbyItems = res.body()!!.data!!
                        val isFromCache = res.body()?.message?.contains("offline cache", ignoreCase = true) == true

                        requireActivity().runOnUiThread {
                            // Show offline indicator if data is from cache
                            if (isFromCache) {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "📡 Offline Mode: Showing cached nearby stations",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            
                            displayStations(nearbyItems, showDistance = true)
                            
                            // Hide loading indicator
                            (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.hideLoadingIndicator()
                        }
                    } else {
                        // Hide loading indicator on error
                        requireActivity().runOnUiThread {
                            (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.hideLoadingIndicator()
                        }
                    }
                }
            } else {
                // Hide loading indicator if no location
                (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.hideLoadingIndicator()
            }
        }
    }
    
    /**
     * Load all stations from the backend (no distance calculation)
     */
    private fun loadAllStations() {
        val ctx = requireContext()
        
        // Show loading indicator
        (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.showLoadingIndicator()
        
        CoroutineScope(Dispatchers.IO).launch {
            // Use repository with offline caching support
            val repo = com.example.evcharger.repository.ReservationRepository(requireContext())
            val res = repo.getAllStations()
            
            if (res.isSuccessful && res.body()?.data != null) {
                val allStations = res.body()!!.data!!
                val isFromCache = res.body()?.message?.contains("offline cache", ignoreCase = true) == true

                requireActivity().runOnUiThread {
                    // Show offline indicator if data is from cache
                    if (isFromCache) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "📡 Offline Mode: Showing cached stations",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "✓ Showing ${allStations.size} stations",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    // Convert to NearbyItem format (without distance)
                    val nearbyItems = allStations.map { station ->
                        com.example.evcharger.model.BackendNearbyItem(
                            station = station,
                            distanceKm = null  // No distance in all stations mode
                        )
                    }
                    
                    displayStations(nearbyItems, showDistance = false)
                    
                    // Adjust camera to show all stations
                    adjustCameraToShowAllStations(allStations)
                    
                    // Hide loading indicator
                    (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.hideLoadingIndicator()
                }
            } else {
                // Hide loading indicator on error
                requireActivity().runOnUiThread {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to load all stations",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    (activity as? com.example.evcharger.ui.activities.DashboardActivity)?.hideLoadingIndicator()
                }
            }
        }
    }
    
    /**
     * Display stations on the map
     * @param nearbyItems List of stations with optional distance
     * @param showDistance Whether to show distance in the marker info
     */
    private fun displayStations(nearbyItems: List<com.example.evcharger.model.BackendNearbyItem>, showDistance: Boolean) {
        // Clear old markers
        currentMarkers.forEach { it.remove() }
        currentMarkers.clear()

        // Add new station markers
        nearbyItems.forEach { bs ->
            val backend = bs.station
            val appStation = com.example.evcharger.model.Station(
                id = backend.stationId,
                name = backend.name,
                latitude = backend.location.latitude,
                longitude = backend.location.longitude,
                address = backend.location.address ?: backend.location.city ?: "",
                connectorTypes = backend.slots.map { it.connectorType },
                chargingPowerKw = backend.slots.firstOrNull()?.powerRating,
                status = backend.type,  // e.g., "AC", "DC"
                lastUpdated = backend.updatedAt,
                distanceMeters = bs.distanceKm?.let { (it * 1000).toInt() },
                isActive = backend.isActive  // Include station active status
            )

            // 💡 Label shows power type (e.g., "DC 50kW")
            val powerLabel = appStation.chargingPowerKw?.let { "${it.toInt()}kW" }
                ?: appStation.connectorTypes.firstOrNull()
                ?: "EV"

            // Marker color depends on station active status
            val markerStatus = if (backend.isActive) "available" else "offline"
            val markerIcon = MarkerUtils.createEvMarkerBitmap(
                context = requireContext(),
                label = powerLabel,
                status = markerStatus
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
    
    /**
     * Adjust camera to show all stations on the map
     */
    private fun adjustCameraToShowAllStations(stations: List<com.example.evcharger.model.BackendStationV2>) {
        if (stations.isEmpty()) return
        
        val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
        stations.forEach { station ->
            builder.include(LatLng(station.location.latitude, station.location.longitude))
        }
        
        val bounds = builder.build()
        val padding = 100 // pixels
        
        try {
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding)
            )
        } catch (e: Exception) {
            // Fallback to first station if bounds calculation fails
            stations.firstOrNull()?.let { station ->
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(station.location.latitude, station.location.longitude),
                        10f
                    )
                )
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
