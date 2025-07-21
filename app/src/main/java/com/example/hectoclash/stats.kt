package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class stats : AppCompatActivity() {
    private var profilePic: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats)
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        val realRoot = contentView.getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(realRoot) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
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
        val button:TextView = findViewById(R.id.btn_match_history)
        button.setOnClickListener{
            SfxManager.playSfx(this, R.raw.button_sound)
            val intent = Intent(this, MatchHistoryList::class.java)
            startActivity(intent)
        }
        profilePicture.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            profilePic?.let { it1 -> viewdp(it1) }?.show(supportFragmentManager, "dp_popup")
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
                    setupStatCard(R.id.avg_time_card, "⏱️", "Average Time", "${avgTime}s")
                    setupStatCard(R.id.accuracy_card, "🎯", "Accuracy", "${accuracy}%")
                    setupStatCard(R.id.matches_played_card, "🎮", "Matches Played", "$matchesPlayed")
                    setupStatCard(R.id.matches_won_card, "🏆", "Matches Won", "$matchesWon")

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp)
                            .centerCrop().into(profilePicture)
                        profilePic = url
                    }
                }
            }


        }
    }
    private fun setupStatCard(cardId: Int, icon: String, label: String, value: String) {
        val cardView = findViewById<View>(cardId)
        cardView.findViewById<TextView>(R.id.stat_icon).text = icon
        cardView.findViewById<TextView>(R.id.stat_label).text = label
        cardView.findViewById<TextView>(R.id.stat_value).text = value
    }
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }
}