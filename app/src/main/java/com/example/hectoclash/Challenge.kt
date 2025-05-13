package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class Challenge : AppCompatActivity() {

    private lateinit var createGameButton: Button
    private lateinit var joinGameButton: Button
    private lateinit var joinCodeEditText: EditText
    private lateinit var gameCodeTextView: TextView
    private lateinit var shareCodeButton: Button
    private var generatedRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        createGameButton = findViewById(R.id.buttonCreateGame)
        joinGameButton = findViewById(R.id.buttonJoinGame)
        joinCodeEditText = findViewById(R.id.editTextJoinCode)
        gameCodeTextView = findViewById(R.id.textViewGameCode)
        shareCodeButton = findViewById(R.id.buttonShareCode)
        shareCodeButton.visibility = View.GONE // Initially hide the share button

        createGameButton.setOnClickListener {
            generatedRoomId = generateRoomId()
            gameCodeTextView.text = "Game Code: $generatedRoomId"
            shareCodeButton.visibility = View.VISIBLE
            // TODO: Send "createPrivateRoom" message to the server with your UID and the generatedRoomId
            Toast.makeText(this, "Game code generated: $generatedRoomId", Toast.LENGTH_SHORT).show()
        }

        joinGameButton.setOnClickListener {
            val joinCode = joinCodeEditText.text.toString().trim()
            if (joinCode.isNotEmpty()) {
                // TODO: Send "joinPrivateRoom" message to the server with your UID and the joinCode
                val gameIntent = Intent(this, GameActivity::class.java)
                gameIntent.putExtra("room_id", joinCode)
                startActivity(gameIntent)
            } else {
                Toast.makeText(this, "Please enter a game code", Toast.LENGTH_SHORT).show()
            }
        }

        shareCodeButton.setOnClickListener {
            generatedRoomId?.let { roomId ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Join my HectoClash game!")
                    putExtra(Intent.EXTRA_TEXT, "Use this code to join my game: $roomId")
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            } ?: run {
                Toast.makeText(this, "No game code to share yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateRoomId(): String {
        // Simple unique ID generation - consider a more robust method on the server
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase()
    }
}