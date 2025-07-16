package com.example.hectoclash

import androidx.recyclerview.widget.DiffUtil

class RoomDiffCallback : DiffUtil.ItemCallback<RoomInfo>() {
    override fun areItemsTheSame(oldItem: RoomInfo, newItem: RoomInfo): Boolean {
        return oldItem.roomId == newItem.roomId
    }

    override fun areContentsTheSame(oldItem: RoomInfo, newItem: RoomInfo): Boolean {
        return oldItem.player1Uid == newItem.player1Uid &&
                oldItem.player2Uid == newItem.player2Uid
    }
}