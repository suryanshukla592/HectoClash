package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(R.layout.activity_spectator)
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

                recyclerView = findViewById(R.id.roomsRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this)

                adapter = RoomListAdapter(roomList) { room ->
                    val intent = Intent(this, SpectatorActivity::class.java)
                    intent.putExtra("roomId", room.roomId)
                    startActivity(intent)
                }

                recyclerView.adapter = adapter
                setupWebSocket()
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

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        webSocketClient.close()
        disconnectWebSocket()
        finish()
        super.onBackPressed()
    }
    private fun disconnectWebSocket() {
        Log.d("GameActivity", "Disconnecting WebSocket...")
        if(webSocketClient.isOpen){
            webSocketClient.close()
        }
        Log.d("GameActivity", "WebSocket disconnected.")
    }

    }

