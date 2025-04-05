package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import java.net.URI
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SpectatorActivity : AppCompatActivity() {
    private lateinit var player1ExpressionTextView: TextView
    private lateinit var player2ExpressionTextView: TextView
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var roomId: String
    private var player1UID: String = ""
    private var player2UID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spectator_activity_real)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        firebaseAuth = FirebaseAuth.getInstance() // Initialize firebaseAuth here

        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val uid = user?.uid
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        player1ExpressionTextView = findViewById(R.id.player1ExpressionTextView)
        player2ExpressionTextView = findViewById(R.id.player2ExpressionTextView)

        roomId = intent.getStringExtra("roomId") ?: ""
        setupWebSocket()
    }

    private fun setupWebSocket() {
        Log.d("SpectatorActivity", "Attempting to connect to WebSocket: ws://3.111.203.229:8080/ws")
        val serverUri = URI("ws://3.111.203.229:8080/ws") // Replace with your WebSocket server URL

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("SpectatorActivity", "WebSocket opened")
                if (handshakedata != null) {
                    Log.v("SpectatorActivity", "Handshake Status: ${handshakedata.httpStatus}")
                    Log.v("SpectatorActivity", "Handshake Message: ${handshakedata.httpStatusMessage}")

                    Log.v("SpectatorActivity", "Server Handshake Headers:")
                    val headerIterator = handshakedata.iterateHttpFields()
                    while (headerIterator.hasNext()) {
                        val headerName = headerIterator.next()
                    }
                }
                val uid = firebaseAuth.currentUser?.uid ?: ""
                val json = JSONObject().apply {
                    put("type", "spectateRoom")
                    put("room_id", roomId)
                    put("uid", uid)
                }
                sendWebSocketMessage(json.toString())
            }

            override fun onMessage(message: String?) {
                Log.v("SpectatorActivity", "Received WebSocket message: $message")
                if (message != null) {
                    try {
                        val json = JSONObject(message)
                        when (json.getString("type")) {
                            "expressionUpdate" -> {
                                try {
                                    val expression = json.getString("expression")
                                    val playerUID = json.getString("opponent")
                                    runOnUiThread {
                                        when (playerUID) {
                                            player1UID -> player1ExpressionTextView.text = expression
                                            player2UID -> player2ExpressionTextView.text = expression
                                        }
                                    }
                                } catch (e: JSONException) {
                                    Log.e("SpectatorActivity", "Error parsing expressionUpdate: ${e.message}, Raw message: $message")
                                }
                            }
                            "playerMeta" -> {
                                try {
                                    val role = json.getString("opponent") // Changed to "opponent" as per your Go server
                                    val uid = json.getString("player")   // Changed to "player" as per your Go server
                                    runOnUiThread {
                                        if (role == "Player1") {
                                            player1UID = uid
                                        } else if (role == "Player2") {
                                            player2UID = uid
                                        }
                                    }
                                } catch (e: JSONException) {
                                    Log.e("SpectatorActivity", "Error parsing playerMeta: ${e.message}, Raw message: $message")
                                }
                            }
                            "puzzle" -> {
                                try {
                                    val puzzle = json.getString("content")
                                    runOnUiThread {
                                        // You might want to display the puzzle somewhere in your UI
                                        Log.d("SpectatorActivity", "Received puzzle: $puzzle")
                                    }
                                } catch (e: JSONException) {
                                    Log.e("SpectatorActivity", "Error parsing puzzle: ${e.message}, Raw message: $message")
                                }
                            }
                            else -> {
                                Log.d("SpectatorActivity", "Received unknown message type: ${json.getString("type")}")
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("SpectatorActivity", "Error parsing base JSON: ${e.message}, Raw message: $message")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("SpectatorActivity", "WebSocket closed. Code: $code, Reason: ${reason ?: "No reason provided"}, Remote: $remote")
            }

            override fun onError(ex: Exception?) {
                Log.e("SpectatorActivity", "WebSocket error: ${ex?.message}")
                ex?.printStackTrace()
            }
        }
        webSocketClient.connect()
    }

    override fun onBackPressed() {
        disconnectWebSocket()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
    }

    private fun disconnectWebSocket() {
        Log.d("SpectatorActivity", "Disconnecting WebSocket...")
        if (webSocketClient != null) {
            if (webSocketClient.isOpen) {
                webSocketClient.close()
            }
            Log.d("SpectatorActivity", "WebSocket disconnected.")
        } else {
            Log.d("SpectatorActivity", "WebSocket is already disconnected or null.")
        }
    }

    private fun sendWebSocketMessage(message: String) {
        if (webSocketClient.isOpen) {
            Log.d("SpectatorActivity", "Sending WebSocket message: $message")
            webSocketClient.send(message)
        } else {
            Log.w("SpectatorActivity", "WebSocket is not open, cannot send message: $message")
        }
    }
}