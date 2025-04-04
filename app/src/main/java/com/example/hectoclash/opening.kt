package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class opening : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.opening)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val signInButton = findViewById<Button>(R.id.button_sign_in)
        signInButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
        val signUpButton = findViewById<TextView>(R.id.button_signup)
        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }
}