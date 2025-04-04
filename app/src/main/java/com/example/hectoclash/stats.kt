package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class stats : AppCompatActivity() {private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        val db = Firebase.firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        val profilePicture: ImageView = findViewById(R.id.profile_picture)
        val nameText: TextView = findViewById(R.id.name)
        val ratingText: TextView = findViewById(R.id.rating)
//        val avgTimeText: TextView = findViewById(R.id.avg_time)
//        val accuracyText: TextView = findViewById(R.id.accuracy)
//        val matchesPlayedText: TextView = findViewById(R.id.matches_played)
//        val matchesLostText: TextView = findViewById(R.id.matches_lost)

        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username") ?: "Unknown"
                    val url = document.getString("Profile Picture URL")
                    val rating = document.getDouble("Rating") ?: 0.0
//                    val avgTime = document.getDouble("Average Time") ?: 0.0
//                    val accuracy = document.getDouble("Accuracy") ?: 0.0
//                    val matchesPlayed = document.getLong("Matches Played") ?: 0
//                    val matchesLost = document.getLong("Matches Lost") ?: 0

                    nameText.text = name
                    ratingText.text = "⭐ Rating: $rating"
//                    avgTimeText.text = "⏱ Average Time: ${avgTime}s"
//                    accuracyText.text = "🎯 Accuracy: ${accuracy}%"
//                    matchesPlayedText.text = "🎮 Matches Played: $matchesPlayed"
//                    matchesLostText.text = "❌ Matches Lost: $matchesLost"

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp)
                            .centerCrop().into(profilePicture)
                    }
                }
            }


        }
    }
}