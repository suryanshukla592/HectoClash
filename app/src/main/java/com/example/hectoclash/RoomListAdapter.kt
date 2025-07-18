package com.example.hectoclash

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomListAdapter(
    private val onItemClick: (RoomInfo) -> Unit
) : ListAdapter<RoomInfo, RoomListAdapter.RoomViewHolder>(RoomDiffCallback()) {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val player1Name: TextView = itemView.findViewById(R.id.player1Name)
        val player2Name: TextView = itemView.findViewById(R.id.player2Name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_list_item, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = getItem(position)
        val db = Firebase.firestore

        val userRef = db.collection("Users")
        userRef.document(room.player1Uid).get().addOnSuccessListener { document1 ->
            if (document1.exists()) {
                holder.player1Name.text = document1.getString("Username").toString()
                holder.player1Name.isSelected = true
            }
            userRef.document(room.player2Uid).get().addOnSuccessListener { document2 ->
                if (document2.exists()) {
                    holder.player2Name.text = document2.getString("Username").toString()
                    holder.player2Name.isSelected = true
                }
            }
        }

        if (room.roomId.contains("private")) {
            holder.itemView.findViewById<LinearLayout>(R.id.privateTag).visibility = View.VISIBLE
        }else{
            holder.itemView.findViewById<LinearLayout>(R.id.privateTag).visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            SfxManager.playSfx(holder.itemView.context, R.raw.button_sound)
            if (room.roomId.contains("private")) {
                val dialogView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.room_privacy, null)
                val builder = AlertDialog.Builder(holder.itemView.context)
                    .setView(dialogView)
                    .setCancelable(true)
                val alertDialog = builder.create()
                alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                alertDialog.window?.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                alertDialog.window?.setGravity(Gravity.CENTER)
                val btnJoin = dialogView.findViewById<Button>(R.id.btn_submit)
                btnJoin.setOnClickListener {
                    SfxManager.playSfx(holder.itemView.context, R.raw.button_sound)
                    val roomID = dialogView.findViewById<EditText>(R.id.roomID).text.toString().trim()
                    if (roomID.isNotEmpty()) {
                        if (room.roomId.contains("-${roomID.uppercase()}-")) {
                            alertDialog.dismiss()
                            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
                            onItemClick(room)
                        } else {
                            Toast.makeText(holder.itemView.context, "Invalid Room ID", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            holder.itemView.context,
                            "Please enter the Room ID",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                alertDialog.show()
            }else{
                MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
                onItemClick(room)
            }
        }
    }
}