package com.example.petnutritionistapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit               // ✅ KTX
import androidx.core.net.toUri               // ✅ KTX
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var btnBCS: Button
    private lateinit var btnStart: Button
    private lateinit var btnAIAdvisor: Button
    private lateinit var btnLogout: Button
    private lateinit var btnNearbyVet: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private val locationPerms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.any { it.value }
        if (granted) openNearbyVets() else openNearbyVetsWithoutLocation()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        btnBCS = view.findViewById(R.id.btnBCS)
        btnStart = view.findViewById(R.id.btnStart)
        btnAIAdvisor = view.findViewById(R.id.btnAIAdvisor)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnNearbyVet = view.findViewById(R.id.btnNearbyVet)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", 0)

        btnBCS.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BCSIntroductionFragment())
                .addToBackStack(null)
                .commit()
        }

        btnStart.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DogInputFragment())
                .addToBackStack(null)
                .commit()
        }

        btnAIAdvisor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AIAdvisorFragment())
                .addToBackStack(null)
                .commit()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            // ✅ 使用 KTX，消掉 Lint
            sharedPreferences.edit { clear() }
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        btnNearbyVet.setOnClickListener { onNearbyVetClick() }

        return view
    }

    private fun onNearbyVetClick() {
        val hasPermission = locationPerms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) openNearbyVets() else requestPerms.launch(locationPerms)
    }

    // ✅ 再次檢查權限 + 捕捉 SecurityException
    @SuppressLint("MissingPermission")
    private fun openNearbyVets() {
        val ok = locationPerms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (!ok) {
            openNearbyVetsWithoutLocation()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val lat = loc.latitude
                        val lng = loc.longitude
                        val query = Uri.encode("寵物醫院")
                        val uri = "geo:$lat,$lng?q=$query&z=15".toUri()   // ✅ 使用 toUri()
                        openMaps(uri)
                    } else {
                        openNearbyVetsWithoutLocation()
                    }
                }
                .addOnFailureListener { openNearbyVetsWithoutLocation() }
        } catch (se: SecurityException) {
            openNearbyVetsWithoutLocation()
        }
    }

    private fun openNearbyVetsWithoutLocation() {
        val query = Uri.encode("寵物醫院")
        val uri = "geo:0,0?q=$query".toUri()                                // ✅ 使用 toUri()
        openMaps(uri)
    }

    private fun openMaps(uri: Uri) {
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "找不到可開啟地圖的應用程式", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
