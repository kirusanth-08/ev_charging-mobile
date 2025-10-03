package com.example.evcharger.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.evcharger.R
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shows Google Map with nearby charging stations.
 */
class MapsFragment : Fragment(R.layout.fragment_maps) {

    private var googleMap: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            enableMyLocation()
            loadNearby()
        }
    }

    private fun enableMyLocation() {
        val ctx = requireContext()
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            fused.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    val pos = LatLng(it.latitude, it.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13f))
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun loadNearby() {
        val ctx = requireContext()
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val res = RetrofitClient.api.getNearbyStations(loc.latitude, loc.longitude)
                    if (res.isSuccessful && res.body()?.data != null) {
                        val stations = res.body()!!.data!!
                        requireActivity().runOnUiThread {
                            stations.forEach { s ->
                                googleMap?.addMarker(
                                    MarkerOptions().position(LatLng(s.latitude, s.longitude)).title(s.name)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}