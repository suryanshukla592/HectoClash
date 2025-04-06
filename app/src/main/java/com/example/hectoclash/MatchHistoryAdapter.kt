package com.example.hectoclash

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hectoclash.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class MatchHistoryAdapter(private val matchHistoryList: List<MatchHistoryEntry>) :
    RecyclerView.Adapter<MatchHistoryAdapter.MatchHistoryViewHolder>() {

    private val usernameCache = mutableMapOf<String, String>()
    private val firestore = FirebaseFirestore.getInstance()

    class MatchHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val opponentName: TextView = itemView.findViewById(R.id.opponentName)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val puzzleText: TextView = itemView.findViewById(R.id.puzzleText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_list_item, parent, false)
        return MatchHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchHistoryViewHolder, position: Int) {
        val match = matchHistoryList[position]

        // Puzzle display
        holder.puzzleText.text = "Puzzle: ${match.puzzle}"

        // Time formatting
        holder.timestamp.text = getTimeAgo(match.timestamp)

        // Background color by result
        when (match.result.lowercase()) {
            "won" -> holder.itemView.setBackgroundColor(Color.parseColor("#2ECC71")) // Bright Green
            "lose" -> holder.itemView.setBackgroundColor(Color.parseColor("#FF3B3B")) // Bright Red
            "draw" -> holder.itemView.setBackgroundColor(Color.parseColor("#B0B0B0")) // Bright Grey
        }

        // Show opponent name using cache or fetch
        val opponentUID = match.opponentUID
        holder.itemView.setOnClickListener {
            val mediaPlayer = MediaPlayer.create(holder.itemView.context, R.raw.button_sound)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }
            val context = holder.itemView.context
            val intent = Intent(context, MatchDetailsActivity::class.java)
            intent.putExtra("match_data", match)
            context.startActivity(intent)
        }

        if (usernameCache.containsKey(opponentUID)) {
            holder.opponentName.text = "Opponent: ${usernameCache[opponentUID]}"
        } else {
            // Temporary placeholder
            holder.opponentName.text = "Opponent: ..."

            // Fetch username from Firestore
            firestore.collection("Users")
                .document(opponentUID)
                .get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("Username") ?: "Unknown"
                    usernameCache[opponentUID] = username
                    holder.opponentName.text = "Opponent: $username"
                }
                .addOnFailureListener { e ->
                    Log.e("MatchHistoryAdapter", "Failed to fetch username: ${e.message}")
                    holder.opponentName.text = "Opponent: Unknown"
                }
        }
    }

    override fun getItemCount(): Int = matchHistoryList.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val years = days / 365

        return when {
            seconds < 60 -> "${seconds}s ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 365 -> "${days}d ago"
            else -> "${years}y ago"
        }
    }
}
