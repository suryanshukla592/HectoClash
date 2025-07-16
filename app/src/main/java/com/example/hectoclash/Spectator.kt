package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class Spectator : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RoomListAdapter
    private lateinit var webSocketClient: WebSocketClient
    private val roomList = mutableListOf<RoomInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<ViewGroup>(android.R.id.content)) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
        setContentView(R.layout.activity_spectator)

        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        MusicManager.resumeMusic()
        MusicManager.setMusicVolume(this)

        recyclerView = findViewById(R.id.roomsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RoomListAdapter{ room ->
            val intent = Intent(this, SpectatorActivity::class.java)
            intent.putExtra("roomId", room.roomId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        setupWebSocket()

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                disconnectWebSocket()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupWebSocket() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val serverUri = URI("ws://3.111.203.229:8080/ws?uid=$uid")

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocket", "Connected to server.")

                // Send requestRoomList ONCE
                try {
                    val json = JSONObject().apply {
                        put("type", "requestRoomList")
                        put("uid", uid ?: "")
                    }
                    webSocketClient.send(json.toString())
                    Log.d("WebSocket", "Sent requestRoomList: $json")
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error sending requestRoomList: ${e.message}")
                }
            }

            // In Spectator.kt, inside webSocketClient.onMessage
            override fun onMessage(message: String?) {
                if (message != null) {
                    try {
                        val json = JSONObject(message)
                        if (json.getString("type") == "roomList") {
                            val roomListJsonObject = json.getJSONObject("content")
                            val newRoomList = mutableListOf<RoomInfo>()
                            val roomIDs = roomListJsonObject.keys()

                            Log.d("WebSocketData", "Received roomList update.")
                            if (!roomIDs.hasNext()) {
                                Log.d("WebSocketData", "Room list is empty.")
                            }

                            while (roomIDs.hasNext()) {
                                val roomID = roomIDs.next()
                                val players = roomListJsonObject.getJSONObject(roomID)
                                val player1 = players.getString("Player1")
                                val player2 = players.getString("Player2")
                                val roomInfo = RoomInfo(roomID, player1, player2)
                                newRoomList.add(roomInfo)
                            }

                            runOnUiThread {
                                adapter.submitList(newRoomList)
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("WebSocket", "Error parsing room list: ${e.message}")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "Connection closed: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "WebSocket error: ${ex?.message}")
            }
        }

        webSocketClient.connect()
    }


    private fun disconnectWebSocket() {
        if (this::webSocketClient.isInitialized && webSocketClient.isOpen) {
            webSocketClient.close()
        }
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
        MusicManager.setMusicVolume(this)
        if (!webSocketClient.isOpen) setupWebSocket()
    }

    override fun onPause() {
        super.onPause()
        disconnectWebSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
    }
}
