package com.example.hectoclash

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomListAdapter(
    private var roomList: List<RoomInfo>,
    private val onItemClick: (RoomInfo) -> Unit
) : RecyclerView.Adapter<RoomListAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomIdText: TextView = itemView.findViewById(R.id.roomIdText)
        val player1Name: TextView = itemView.findViewById(R.id.player1Name)
        val player2Name: TextView = itemView.findViewById(R.id.player2Name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_list_item, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        val db = Firebase.firestore

        if (room.player1Uid != null||room.player2Uid != null) {
            val userRef = db.collection("Users")
            userRef.document(room.player1Uid).get().addOnSuccessListener { document1 ->
                if (document1.exists()) {
                    holder.player1Name.text = document1.getString("Username").toString()
                }
                userRef.document(room.player2Uid).get().addOnSuccessListener { document2 ->
                    if (document2.exists()) {
                        holder.player2Name.text = document2.getString("Username").toString()
                    }
                }
            }
        }
        val displayId = room.roomId.substringAfter("room-")
        holder.roomIdText.text = "Room ID: $displayId"

        holder.itemView.setOnClickListener {
            SfxManager.playSfx(holder.itemView.context, R.raw.button_sound)
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