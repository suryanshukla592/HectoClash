package com.example.hectoclash

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class stats : AppCompatActivity() {
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
        val avgTimeText: TextView = findViewById(R.id.avg_time)
        val accuracyText: TextView = findViewById(R.id.accuracy)
        val matchesPlayedText: TextView = findViewById(R.id.matches_played)
        val matchesWonText: TextView = findViewById(R.id.matches_won)
        val button:TextView = findViewById(R.id.btn_match_history)
        button.setOnClickListener{
            SfxManager.playSfx(this, R.raw.button_sound)
            val intent = Intent(this, MatchHistoryList::class.java)
            startActivity(intent)
            finish()
        }
        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username") ?: "Unknown"
                    val url = document.getString("Profile Picture URL")
                    val rating = document.getDouble("Rating")?.toInt() ?:0
                    val avgTime = document.getDouble("Time")?.toInt() ?:0
                    val accuracy = document.getDouble("Accuracy")?.toInt() ?:0
                    val matchesPlayed = document.getLong("Played") ?: 0
                    val matchesWon = document.getLong("Won") ?: 0

                    nameText.text = name
                    ratingText.text = "⭐ Rating: $rating"
                    avgTimeText.text = "⏱ Average Time: ${avgTime}s"
                    accuracyText.text = "🎯 Accuracy: ${accuracy}%"
                    matchesPlayedText.text = "🎮 Matches Played: $matchesPlayed"
                    matchesWonText.text = "🏆 Matches Won: $matchesWon"

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp)
                            .centerCrop().into(profilePicture)
                    }
                }
            }


        }
    }
}