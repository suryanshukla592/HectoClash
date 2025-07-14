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

                adapter = RoomListAdapter(roomList) { room ->
                    val intent = Intent(this, SpectatorActivity::class.java)
                    intent.putExtra("roomId", room.roomId)
                    startActivity(intent)
                }

                recyclerView.adapter = adapter
                setupWebSocket()
                val onBackPressedCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        webSocketClient.close()
                        disconnectWebSocket()
                        finish()
                    }
                }
                onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
            }
    private fun setupWebSocket() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val serverUri = URI("ws://3.111.203.229:8080/ws?uid=$uid") // Replace with your WebSocket server URL

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // Request room list from server
                val json = JSONObject().apply {
                    put("type", "requestRoomList")
                    put("uid", uid)
                }
                send(json.toString())
            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    try {
                        val json = JSONObject(message)
                        if (json.getString("type") == "roomList") {
                            val roomListJsonObject = json.getJSONObject("content")
                            roomList.clear()
                            val roomIDs = roomListJsonObject.keys()
                            while (roomIDs.hasNext()) {
                                val roomID = roomIDs.next()
                                val players = roomListJsonObject.getJSONObject(roomID)
                                val player1 = players.getString("Player1")
                                val player2 = players.getString("Player2")
                                roomList.add(RoomInfo(roomID, player1, player2))
                            }
                            runOnUiThread {
                                adapter.updateRoomList(roomList)
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("RoomListActivity", "Error parsing room list: ${e.message}")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("RoomListActivity", "WebSocket closed")
            }

            override fun onError(ex: Exception?) {
                Log.e("RoomListActivity", "WebSocket error: ${ex?.message}")
            }
        }
        webSocketClient.connect()
    }
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }
    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }
    private fun disconnectWebSocket() {
        Log.d("GameActivity", "Disconnecting WebSocket...")
        if(webSocketClient.isOpen){
            webSocketClient.close()
        }
        Log.d("GameActivity", "WebSocket disconnected.")
    }

    }

