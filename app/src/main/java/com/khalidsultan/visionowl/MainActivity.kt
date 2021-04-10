package com.khalidsultan.visionowl
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            var fragment: Fragment
            when (item.itemId) {
                R.id.navigationMyProfile -> return@OnNavigationItemSelectedListener true
                R.id.navigationMyCourses -> return@OnNavigationItemSelectedListener true
                R.id.navigationHome -> return@OnNavigationItemSelectedListener true
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDarkMode(window)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        //
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior = BottomNavigationBehavior()
        bottomNavigationView.selectedItemId = R.id.navigationHome

        //handling floating action menu
        findViewById<View>(R.id.floatingActionButton).setOnClickListener {
            (findViewById<View>(R.id.drawer_layout) as DrawerLayout).openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_share -> {
            }
            R.id.nav_dark_mode -> {
                val darkModePrefManager = DarkModePrefManager(this)
                darkModePrefManager.setDarkMode(!darkModePrefManager.isNightMode)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                recreate()
            }
        }
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setDarkMode(window: Window) {
        if (DarkModePrefManager(this).isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            changeStatusBar(MODE_DARK, window)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            changeStatusBar(MODE_LIGHT, window)
        }
    }

    private fun changeStatusBar(mode: Int, window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = this.resources.getColor(R.color.contentStatusBar)
            //Light mode
            if (mode == MODE_LIGHT) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    companion object {
        private const val MODE_DARK = 0
        private const val MODE_LIGHT = 1
    }
}