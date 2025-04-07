package com.example.hectoclash

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashScreenCustom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setTheme(R.style.Theme_App_Starting)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.splashscreen)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        proceedToDefault()
    }

    private fun proceedToDefault() {
        lifecycleScope.launch {
            delay(3000)
            val nextActivity = if (FirebaseAuth.getInstance().currentUser != null) {
                MainActivity::class.java
            } else {
                opening::class.java
            }
            val intent = Intent(this@SplashScreenCustom, nextActivity)
            val options = ActivityOptions.makeCustomAnimation(
                this@SplashScreenCustom,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent, options.toBundle())
            finish()
        }
    }
}
