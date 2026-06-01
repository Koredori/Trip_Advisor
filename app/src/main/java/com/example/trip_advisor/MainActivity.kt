package com.example.trip_advisor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trip_advisor.ui.addtrip.AddTripActivity
import com.example.trip_advisor.ui.home.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load home fragment on first launch
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.nav_home)
        }

        bottomNav.setOnItemSelectedListener { item ->
            // Clear back stack when switching tabs
            repeat(supportFragmentManager.backStackEntryCount) {
                supportFragmentManager.popBackStackImmediate()
            }
            when (item.itemId) {
                R.id.nav_home     -> {
                    loadFragment(HomeFragment(), item.itemId)
                    true
                }
                R.id.nav_add_trip -> {
                    val intent = Intent(this, AddTripActivity::class.java)
                    startActivity(intent)
                    false // Do not visually select the "Add" tab since it launches a separate Activity
                }
                else -> false
            }
        }

        // Sync bottom nav when back stack changes (e.g., returning from edit)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                bottomNav.selectedItemId = R.id.nav_home
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment, navItemId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}