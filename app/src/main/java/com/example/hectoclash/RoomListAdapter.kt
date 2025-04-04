package com.example.hectoclash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        holder.roomNumberTextView.text = "Room: ${room.roomId}"
        holder.playersTextView.text = "${room.player1Uid} vs ${room.player2Uid}"

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