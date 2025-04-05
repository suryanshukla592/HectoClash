package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class SpectatorActivity : AppCompatActivity() {
    private lateinit var player1ExpressionTextView: TextView
    private lateinit var player2ExpressionTextView: TextView
    private lateinit var player1FeedbackTextView: TextView
    private lateinit var player2FeedbackTextView: TextView
    private lateinit var player1NameTextView: TextView
    private lateinit var player2NameTextView: TextView
    private lateinit var puzzleTextView: TextView

    private lateinit var webSocketClient: WebSocketClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var roomId: String
    private var player1UID: String = ""
    private var player2UID: String = ""
    private val handler = Handler()
    private val pingInterval = 30000L // 30 seconds
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spectator_activity_real)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        firebaseAuth = FirebaseAuth.getInstance()

        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        // View Bindings bro
        puzzleTextView = findViewById(R.id.textViewFinalPuzzle)
        player1ExpressionTextView = findViewById(R.id.player1ExpressionTextView)
        player2ExpressionTextView = findViewById(R.id.player2ExpressionTextView)
        player1FeedbackTextView = findViewById(R.id.player1FeedbackTextView)
        player2FeedbackTextView = findViewById(R.id.player2FeedbackTextView)
        player1NameTextView = findViewById(R.id.player1NameTextView)
        player2NameTextView = findViewById(R.id.player2NameTextView)

        roomId = intent.getStringExtra("roomId") ?: ""

        setupWebSocket()
    }

    private fun setupWebSocket() {
        val serverUri = URI("ws://3.111.203.229:8080/ws")

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("SpectatorActivity", "WebSocket opened")
                val uid = firebaseAuth.currentUser?.uid ?: ""
                val json = JSONObject().apply {
                    put("type", "spectateRoom")
                    put("room_id", roomId)
                    put("uid", uid)
                }
                sendWebSocketMessage(json.toString())
            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    try {
                        val json = JSONObject(message)
                        when (json.getString("type")) {
                            "expressionUpdate" -> {
                                val expression = json.getString("expression")
                                val playerUID = json.getString("opponent")
                                runOnUiThread {
                                    if (playerUID == player1UID) {
                                        player1ExpressionTextView.text = expression
                                    } else if (playerUID == player2UID) {
                                        player2ExpressionTextView.text = expression
                                    }
                                }
                            }

                            "playerMeta" -> {
                                val role = json.getString("opponent")
                                val uid = json.getString("player")
                                runOnUiThread {
                                    if (role == "Player1") {
                                        player1UID = uid
                                        player1NameTextView.text = "Player 1: $uid"
                                    } else if (role == "Player2") {
                                        player2UID = uid
                                        player2NameTextView.text = "Player 2: $uid"
                                    }
                                }
                            }

                            "puzzle" -> {
                                val puzzle = json.getString("content")
                                runOnUiThread {
                                    puzzleTextView.text = "Puzzle: $puzzle"
                                }
                            }

                            "feedbackUpdate" -> {
                                val uid = json.getString("uid")
                                val feedback = json.getString("feedback")
                                runOnUiThread {
                                    if (uid == player1UID) {
                                        player1FeedbackTextView.text = feedback
                                    } else if (uid == player2UID) {
                                        player2FeedbackTextView.text = feedback
                                    }
                                }
                            }

                            else -> {
                                Log.d("SpectatorActivity", "Unknown type: ${json.getString("type")}")
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("SpectatorActivity", "JSON Error: ${e.message}")
                    }

                    isConnected = true
                    startPing()
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("SpectatorActivity", "WebSocket closed: $code, $reason")
                isConnected = false
                stopPing()
                reconnectWebSocket()
            }

            override fun onError(ex: Exception?) {
                Log.e("SpectatorActivity", "WebSocket error: ${ex?.message}")
                isConnected = false
                stopPing()
                reconnectWebSocket()
            }
        }

        webSocketClient.connect()
    }

    private fun reconnectWebSocket() {
        handler.postDelayed({
            setupWebSocket()
        }, 1000)
    }

    private fun stopPing() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun startPing() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected && webSocketClient.isOpen) {
                    webSocketClient.send("ping")
                }
                handler.postDelayed(this, pingInterval)
            }
        }, pingInterval)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPing()
        webSocketClient.close()
    }

    private fun sendWebSocketMessage(message: String) {
        if (webSocketClient.isOpen) {
            webSocketClient.send(message)
        } else {
            Log.w("SpectatorActivity", "WebSocket is not open!")
        }
    }
}
