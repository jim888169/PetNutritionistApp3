package com.example.petnutritionistapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: run {
                AlertDialog.Builder(this)
                    .setTitle("版面配置錯誤")
                    .setMessage("找不到 R.id.nav_host_fragment。請確認 activity_main.xml 內含 FragmentContainerView，且 id 正確。")
                    .setPositiveButton("關閉") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
                return
            }

        navController = navHost.navController

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.bringToFront()

        // 不要用 setupWithNavController，避免和自訂 listener 打架
        // bottomNav.setupWithNavController(navController)  // ← 刪掉或確保註解掉

        // 目的地變化時同步 UI（只高亮首頁；上一頁/登出保持未選取）
        navController.addOnDestinationChangedListener { _, dest, _ ->
            bottomNav.menu.findItem(R.id.homeFragment).isChecked = (dest.id == R.id.homeFragment)
            bottomNav.menu.findItem(R.id.nav_back).isChecked = false
            bottomNav.menu.findItem(R.id.nav_logout).isChecked = false
        }

        // 連續點「首頁」也要能回到 Home
        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                goHome()
            }
        }

        // 手動處理三個按鈕
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // 不論身在何處，一律回首頁；可連點
                    goHome()
                    true
                }

                R.id.nav_back -> {
                    // 不讓「上一頁」進入選取狀態，避免下次點擊被當成 reselect
                    item.isChecked = false
                    if (navController.previousBackStackEntry != null) {
                        navController.navigateUp()
                    } else {
                        goHome()
                    }
                    true
                }

                R.id.nav_logout -> {
                    item.isChecked = false
                    showLogoutConfirmation()
                    true
                }

                else -> false
            }
        }

        // 系統返回鍵：首頁詢問是否離開，其它則 navigateUp()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navController.currentDestination?.id == R.id.homeFragment) {
                        showExitConfirmation()
                    } else {
                        navController.navigateUp()
                    }
                }
            }
        )
    }

    private fun goHome() {
        // 把 back stack 彈到 home（若已在 home，什麼都不做）
        navController.popBackStack(R.id.homeFragment, false)
        // 確保 UI 高亮在首頁
        bottomNav.menu.findItem(R.id.homeFragment).isChecked = true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("確認登出")
            .setMessage("你確定要登出嗎？")
            .setPositiveButton("是") { _, _ -> performLogout() }
            .setNegativeButton("否", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val prefs: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("離開應用程式")
            .setMessage("確定要離開嗎？")
            .setPositiveButton("是") { _, _ -> finish() }
            .setNegativeButton("取消", null)
            .show()
    }
}
