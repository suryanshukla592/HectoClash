package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import java.net.URI
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject

class SpectatorActivity : AppCompatActivity() {
    private lateinit var player1ExpressionTextView: TextView
    private lateinit var player2ExpressionTextView: TextView
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var roomId: String
    private var player1UID : String = ""
    private var player2UID : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spectator_activity_real)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        firebaseAuth = FirebaseAuth.getInstance()
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
                val user = firebaseAuth.currentUser // Trying to use firebaseAuth here BEFORE it's initialized at the class level
                val uid = user?.uid
                val serverUri = URI("ws://3.111.203.229:8080/ws") // Replace with your WebSocket server URL

                webSocketClient = object : WebSocketClient(serverUri) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        val json = JSONObject().apply {
                            put("type", "spectateRoom")
                            put("room_id", roomId)
                            put("uid",uid)
                        }
                        send(json.toString())
                    }

                    override fun onMessage(message: String?) {
                        if (message != null) {
                            try {
                                val json = JSONObject(message)
                                if (json.getString("type") == "expressionUpdate") {
                                    val expression = json.getString("content")
                                    val playerUID = json.getString("opponent")
                                    runOnUiThread {
                                        if(player1UID == ""){
                                            player1UID = playerUID;
                                        }
                                        if(player2UID == ""){
                                            player2UID = playerUID;
                                        }
                                        if (playerUID == player1UID) {
                                            player1ExpressionTextView.text = expression
                                        } else if (playerUID == player2UID) {
                                            player2ExpressionTextView.text = expression
                                        }
                                    }
                                }
                                if (json.getString("type") == "playerMeta") {
                                    val role = json.getString("role")
                                    val uid = json.getString("uid")
                                    runOnUiThread {
                                        if (role == "Player1") {
                                            player1UID = uid
                                        } else if (role == "Player2") {
                                            player2UID = uid
                                        }
                                    }
                                }
                            } catch (e: JSONException) {
                                Log.e("SpectatorActivity", "Error parsing message: ${e.message}")
                            }
                        }
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        Log.d("SpectatorActivity", "WebSocket closed")
                    }

                    override fun onError(ex: Exception?) {
                        Log.e("SpectatorActivity", "WebSocket error: ${ex?.message}")
                    }
                }
                webSocketClient.connect()
            }

    override fun onBackPressed() {
        webSocketClient.close()
        disconnectWebSocket()
        finish()
        super.onBackPressed()
    }
    private fun disconnectWebSocket() {
        Log.d("GameActivity", "Disconnecting WebSocket...")
        if (webSocketClient != null) {
            if(webSocketClient.isOpen){
                webSocketClient.close()
            }
            Log.d("GameActivity", "WebSocket disconnected.")
        }else{
            Log.d("GameActivity", "WebSocket is already disconnected or null.")
        }
    }
        }