package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MatchHistoryList : AppCompatActivity() {
    private lateinit var matchHistoryRecyclerView: RecyclerView
    private lateinit var matchHistoryAdapter: MatchHistoryAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private val matchHistoryList = mutableListOf<MatchHistoryEntry>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_list)

        matchHistoryRecyclerView = findViewById(R.id.historyRecyclerView)
        matchHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        matchHistoryAdapter = MatchHistoryAdapter(matchHistoryList)
        matchHistoryRecyclerView.adapter = matchHistoryAdapter

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, opening::class.java))
            finish()
        } else {
            fetchMatchHistory(currentUser.uid)
        }
    }

    private fun fetchMatchHistory(uid: String) {
        val matchHistoryRef = firestore.collection("Users").document(uid).collection("MatchHistory")

        matchHistoryRef.get()
            .addOnSuccessListener { documents ->
                matchHistoryList.clear()
                for (document in documents) {
                    val entry = document.toObject(MatchHistoryEntry::class.java)
                    matchHistoryList.add(entry)
                }

                matchHistoryList.sortByDescending { it.timestamp }

                matchHistoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to fetch match history", e)
            }
    }
}