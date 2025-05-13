package com.example.hectoclash

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth

class LeaderboardAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val rankText: TextView = view.findViewById(R.id.rankText)
        val playerName: TextView = view.findViewById(R.id.playerName)
        val scoreText: TextView = view.findViewById(R.id.ratingText)
        val image: ImageView = view.findViewById(R.id.image)
        val acc: TextView = view.findViewById(R.id.accuracyText)
        val time: TextView = view.findViewById(R.id.timeText)
        val medalImage: ImageView = view.findViewById(R.id.medalImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val player = players[position]

        // Load profile image
        Glide.with(holder.itemView.context)
            .load(player.profileURL)
            .placeholder(R.drawable.defaultdp)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)

        // Set player name & score
        holder.playerName.text = player.name
        holder.scoreText.text = "⭐ ${player.rating}"
        holder.acc.text = "🎯 ${player.accuracy}%"
        holder.time.text = "⏱ ${player.time}s"
        holder.cardView.setCardBackgroundColor("#1F1F1F".toColorInt()) // or your default bg
        holder.playerName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
        holder.rankText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))

        // Click to show full DP
        holder.image.setOnClickListener {
            SfxManager.playSfx(holder.itemView.context, R.raw.button_sound)
            val context = it.context
            if (context is AppCompatActivity) {
                val viewdp = viewdp(player.profileURL)
                viewdp.show(context.supportFragmentManager, "dp_popup")
            }
        }

        // Highlight current user
        if (userID == player.uid) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.playerName.setTextColor("#010101".toColorInt())
            holder.rankText.setTextColor("#010101".toColorInt())
        }

        // 🎖 Show medals for 2nd & 3rd rank
        when (position) {
            0 -> { // 2nd Rank (since 1st is shown somewhere else)
                holder.rankText.visibility = View.GONE
                holder.medalImage.visibility = View.VISIBLE
                holder.medalImage.setImageResource(R.drawable.silver_medal)
            }
            1 -> { // 3rd Rank
                holder.rankText.visibility = View.GONE
                holder.medalImage.visibility = View.VISIBLE
                holder.medalImage.setImageResource(R.drawable.bronze_medal)
            }
            else -> { // Normal Ranks (4+)
                holder.medalImage.visibility = View.GONE
                holder.rankText.visibility = View.VISIBLE
                holder.rankText.text = (position + 2).toString()
            }
        }
    }

    override fun getItemCount(): Int = players.size
}
