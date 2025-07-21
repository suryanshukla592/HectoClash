package com.example.hectoclash

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt

class MatchHistoryAdapter(private val matchHistoryList: List<MatchHistoryEntry>) :
    RecyclerView.Adapter<MatchHistoryAdapter.MatchHistoryViewHolder>() {

    private val usernameCache = mutableMapOf<String, String>()
    private val firestore = FirebaseFirestore.getInstance()

    class MatchHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val opponentName: TextView = itemView.findViewById(R.id.opponentName)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val puzzleText: TextView = itemView.findViewById(R.id.puzzleText)
        val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        val resultIcon: ImageView = itemView.findViewById(R.id.result_icon)
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

        if (match.feedback.contains("Won", ignoreCase = true) || match.feedback.contains("Opponent Left", ignoreCase = true)) {
            holder.statusIndicator.setBackgroundColor("#2ECC71".toColorInt())
            holder.resultIcon.setImageResource(R.drawable.win)
        } else if (match.feedback.contains("Lose", ignoreCase = true) || match.feedback.contains("You Left", ignoreCase = true)) {
            holder.statusIndicator.setBackgroundColor("#FF3B3B".toColorInt())
            holder.resultIcon.setImageResource(R.drawable.lose)
        } else {
            holder.statusIndicator.setBackgroundColor("#B0B0B0".toColorInt())
            holder.resultIcon.setImageResource(R.drawable.tied)
        }

        // Show opponent name using cache or fetch
        val opponentUID = match.opponentUID
        holder.itemView.setOnClickListener {
            SfxManager.playSfx(holder.itemView.context, R.raw.button_sound)
            val context = holder.itemView.context
            val intent = Intent(context, MatchDetailsActivity::class.java)
            intent.putExtra("match_data", match)
            context.startActivity(intent)
        }

        if (usernameCache.containsKey(opponentUID)) {
            holder.opponentName.text = "vs. ${usernameCache[opponentUID]}"
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
                    holder.opponentName.text = "vs. $username"
                }
                .addOnFailureListener { e ->
                    Log.e("MatchHistoryAdapter", "Failed to fetch username: ${e.message}")
                    holder.opponentName.text = "vs. Unknown"
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
