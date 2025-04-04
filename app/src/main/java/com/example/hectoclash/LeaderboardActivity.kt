package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    var profileFirst:String ?=""
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("Users")
    private val playerList = mutableListOf<Player>()
    private var topPlayer: Player? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(R.layout.activity_leaderboard)
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardAdapter = LeaderboardAdapter(playerList)
        leaderboardRecyclerView.adapter = leaderboardAdapter
        val profilefirst: ImageView = findViewById(R.id.firstRankProfile)

        fetchLeaderboardData()
        profilefirst.setOnClickListener {
            profileFirst?.let { it1 -> viewdp(it1) }?.show(supportFragmentManager, "dp_popup")
        }
    }

    private fun fetchLeaderboardData() {
        val allPlayers = mutableListOf<Player>()
        usersCollection
            .orderBy("Rating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                playerList.clear()
                topPlayer = null

                for (document in documents) {
                    val uid = document.id
                    val rating = document.getLong("Rating")?.toInt() ?: 0
                    val playerName = document.getString("Username") ?: "Anonymous"
                    val profileURL = document.getString("Profile Picture URL") ?: ""
                    val player = Player(name = playerName, rating = rating, uid = uid, profileURL = profileURL)
                    allPlayers.add(player)
                }

                if (allPlayers.isNotEmpty()) {
                    topPlayer = allPlayers.firstOrNull()

                    val top30WithoutTopOne = allPlayers
                        .drop(1)
                        .take(29)

                    playerList.addAll(top30WithoutTopOne)
                }

                leaderboardAdapter.notifyDataSetChanged()

                topPlayer?.let {
                    val profilefirst: ImageView = findViewById(R.id.firstRankProfile)
                    val namefirst: TextView = findViewById(R.id.namefirst)
                    val ratingfirst: TextView = findViewById(R.id.ratingfirst)
                    namefirst.text= topPlayer!!.name
                    ratingfirst.text= "Rating : "+topPlayer!!.rating.toString()
                    Glide.with(this).load(topPlayer!!.profileURL).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(
                        DiskCacheStrategy.ALL)
                        .into(profilefirst)
                    profileFirst=topPlayer!!.profileURL
                    Log.d("LeaderboardActivity", "Top Player: Name=${it.name}, Rating=${it.rating}, UID=${it.uid}, Profile URL=${it.profileURL}")
                    // You can now use topPlayer.profileURL as well
                }
            }
            .addOnFailureListener { exception ->
                Log.w("LeaderboardActivity", "Error getting documents: ", exception)
                // Handle the error
            }
    }
}