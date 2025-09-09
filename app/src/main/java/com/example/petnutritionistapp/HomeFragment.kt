package com.example.petnutritionistapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var btnBCS: Button
    private lateinit var btnStart: Button
    private lateinit var btnAIAdvisor: Button
    private lateinit var btnLogout: FloatingActionButton
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        btnBCS       = view.findViewById(R.id.btnBCS)
        btnStart     = view.findViewById(R.id.btnStart)
        btnAIAdvisor = view.findViewById(R.id.btnAIAdvisor)
        btnLogout    = view.findViewById(R.id.btnLogout)
        btnNearbyVet = view.findViewById(R.id.btnNearbyVet)

        // 🔒 保險：覆蓋物不攔截點擊
        view.findViewById<View>(R.id.topScrim)?.apply {
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            isSoundEffectsEnabled = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // ───────── 導頁（view-based Navigation）─────────
        btnBCS.setOnClickListener { v ->
            Navigation.findNavController(v).navigate(R.id.bcsIntroTopFragment)
        }
        btnStart.setOnClickListener { v ->
            Navigation.findNavController(v).navigate(R.id.dogInputFragment)
        }
        btnAIAdvisor.setOnClickListener { v ->
            Navigation.findNavController(v).navigate(R.id.aiAdvisorFragment)
        }

        // 登出 → 回到 LoginActivity
        btnLogout.setOnClickListener {
            auth.signOut()
            sharedPreferences.edit { clear() }
            val intent = Intent().apply {
                setClassName(
                    requireContext().packageName,
                    "com.example.petnutritionistapp.api.LoginActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        // 查看附近寵物醫院（有權限用座標，無權限用關鍵字）
        btnNearbyVet.setOnClickListener { onNearbyVetClick() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 依裝置的系統手勢/導覽列自動調整底部間距與漂浮按鈕高度
        val content = view.findViewById<View>(R.id.content)
        val aiBtn = view.findViewById<View>(R.id.btnAIAdvisor)
        val fab = view.findViewById<View>(R.id.btnLogout)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            // 內容底部：在原有 padding 的基礎上加上系統 bottom inset
            if (bottomInset > 0) {
                content.updatePadding(bottom = content.paddingBottom + bottomInset)
                // 浮動按鈕上移，避免被底部系統列／BottomNavigation 遮擋
                aiBtn.translationY = -bottomInset.toFloat()
                fab.translationY = -bottomInset.toFloat()
            }
            insets
        }
    }

    private fun onNearbyVetClick() {
        val hasPermission = locationPerms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) openNearbyVets() else requestPerms.launch(locationPerms)
    }

    @SuppressLint("MissingPermission")
    private fun openNearbyVets() {
        val ok = locationPerms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
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
                        val uri = "geo:$lat,$lng?q=$query&z=15".toUri()
                        openMaps(uri)
                    } else {
                        openNearbyVetsWithoutLocation()
                    }
                }
                .addOnFailureListener { openNearbyVetsWithoutLocation() }
        } catch (_: SecurityException) {
            openNearbyVetsWithoutLocation()
        }
    }

    private fun openNearbyVetsWithoutLocation() {
        val query = Uri.encode("寵物醫院")
        val uri = "geo:0,0?q=$query".toUri()
        openMaps(uri)
    }

    private fun openMaps(uri: Uri) {
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapsIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "找不到可開啟地圖的應用程式",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
