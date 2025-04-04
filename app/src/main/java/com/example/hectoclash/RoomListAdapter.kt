package com.example.hectoclash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomListAdapter(
    private var roomList: List<RoomInfo>,
    private val onItemClick: (RoomInfo) -> Unit
) : RecyclerView.Adapter<RoomListAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomNumberTextView: TextView = itemView.findViewById(R.id.roomNumberTextView)
        val playersTextView: TextView = itemView.findViewById(R.id.playersTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_list_item, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        val db = Firebase.firestore
        var name1 =""
        var name2 =""
        if (room.player1Uid != null||room.player2Uid != null) {
            val userRef = db.collection("Users")
            userRef.document(room.player1Uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    name1 = document.getString("Username").toString()
                }
                userRef.document(room.player2Uid).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        name2 = document.getString("Username").toString()
                    }
                }
            }
        }
        holder.roomNumberTextView.text = "Room: ${room.roomId}"
        holder.playersTextView.text = "${name1}\nVS\n${name2}"

        holder.itemView.setOnClickListener {
            onItemClick(room)
        }
    }

    override fun getItemCount(): Int {
        return roomList.size
    }

    fun updateRoomList(newList: List<RoomInfo>) {
        roomList = newList
        notifyDataSetChanged()
    }
}