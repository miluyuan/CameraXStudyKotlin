package com.example.camerastudykotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 33)
        } else {
            cameraFragment()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
            cameraFragment()
        }
    }

    override fun onResume() {
        super.onResume()

        // 刘海屏需和 <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item> 一起用才能达到全屏效果
        findViewById<FrameLayout>(R.id.fragment_container).apply {
            postDelayed({
                systemUiVisibility = FLAGS_FULLSCREEN
            }, 300)
        }
    }

    private fun cameraFragment() {
        supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, CameraFragment())
                .replace(R.id.fragment_container, CameraViewFragment())
                .commit()
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val FLAGS_FULLSCREEN =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}
