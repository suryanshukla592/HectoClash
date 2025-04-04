package com.example.hectoclash

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import androidx.core.graphics.toColorInt

class LeaderboardAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val crownIcon: ImageView = view.findViewById(R.id.crownIcon)
        val cardView: CardView = view.findViewById(R.id.cardView)
        val rankText: TextView = view.findViewById(R.id.rankText)
        val playerName: TextView = view.findViewById(R.id.playerName)
        val scoreText: TextView = view.findViewById(R.id.scoreText)
        val image: ImageView = view.findViewById(R.id.image)
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
        val cardView = holder.cardView
        holder.rankText.text = (position + 2).toString()
        if (userID == player.uid) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.playerName.setTextColor("#010101".toColorInt())
            holder.rankText.setTextColor("#010101".toColorInt())
        }
            holder.playerName.text = player.name
        holder.scoreText.text = "Rating : "+ player.rating.toString()
        Glide.with(holder.itemView.context).load(player.profileURL).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)
        holder.image.setOnClickListener {
            val context = it.context
            if (context is AppCompatActivity) {
                val viewdp = viewdp(player.profileURL)
                viewdp.show(context.supportFragmentManager, "dp_popup")
            }
        }

        // Assign crowns for the top 3 ranks
//        when (position) {
//            0 -> { // 1st place (Gold Crown)
//                holder.crownIcon.visibility = View.VISIBLE
//                holder.crownIcon.setImageResource(R.drawable.ic_crown_gold)
//            }
//            1 -> { // 2nd place (Silver Crown)
//                holder.crownIcon.visibility = View.VISIBLE
//                holder.crownIcon.setImageResource(R.drawable.ic_crown_gold)
//            }
//            2 -> { // 3rd place (Bronze Crown)
//                holder.crownIcon.visibility = View.VISIBLE
//                holder.crownIcon.setImageResource(R.drawable.ic_crown_gold)
//            }
//            else -> {
//                holder.crownIcon.visibility = View.GONE // Hide ic_crown_gold.xml for ranks 4+
//            }
//        }
    }

    override fun getItemCount(): Int = players.size
}
