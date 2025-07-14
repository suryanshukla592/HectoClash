package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import android.os.Handler
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import net.objecthunter.exp4j.ExpressionBuilder
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
    private lateinit var player1ResultTextView: TextView
    private lateinit var player2ResultTextView: TextView
    private lateinit var player1NameTextView: TextView
    private lateinit var player2NameTextView: TextView
    private lateinit var textViewTimer: TextView
    private var countdownTimer: CountDownTimer? = null
    private var gameStartTime: Long = 0
    private var gameDurationSeconds: Long = 120
    private lateinit var puzzleTextView: TextView

    private lateinit var webSocketClient: WebSocketClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var roomId: String
    private var player1UID: String = ""
    private var timerStarted = false
    private var player2UID: String = ""
    private val handler = Handler()
    private val pingInterval = 30000L
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spectator_activity_real)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<ViewGroup>(android.R.id.content)) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
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
        textViewTimer = findViewById(R.id.textViewTimer)

        player1FeedbackTextView = findViewById(R.id.player1FeedbackTextView)
        player2FeedbackTextView = findViewById(R.id.player2FeedbackTextView)
        player1NameTextView = findViewById(R.id.player1NameTextView)
        player2NameTextView = findViewById(R.id.player2NameTextView)
        player2ResultTextView = findViewById(R.id.player2ResultTextView)
        player1ResultTextView = findViewById(R.id.player1ResultTextView)

        roomId = intent.getStringExtra("roomId") ?: ""

        setupWebSocket()
    }
    private fun startTimer() {
        gameStartTime = System.currentTimeMillis()
        timerStarted=true
        countdownTimer = object : CountDownTimer(gameDurationSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                textViewTimer.text = "⏱\uFE0F ${secondsRemaining}s"

                // Change color to red in last 30 seconds
                if (secondsRemaining <= 30) {
                    textViewTimer.setTextColor("#FF5555".toColorInt())
                    MusicManager.startMusic(this@SpectatorActivity,R.raw.clock_ticking,0)
                    MusicManager.setMusicVolume(this@SpectatorActivity)
                } else {
                    textViewTimer.setTextColor("#D49337".toColorInt())
                }
            }

            override fun onFinish() {
                textViewTimer.text = "Time's Up!"
                MusicManager.stopMusic()
                timerStarted = false
                textViewTimer.setTextColor("#FF5555".toColorInt()) // Ensure final message is red
                countdownTimer?.cancel()
            }
        }.start()
    }

    private fun setupWebSocket() {
        val serverUri = URI("ws://3.111.203.229:8080/ws")
        startPing()

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
                                if (expression!="Your Answer"){
                                runOnUiThread {
                                    if (playerUID == player1UID) {
                                        player1ExpressionTextView.text = expression
                                        val e1 = evaluateExpression(expression)
                                        Log.d("DEBUG",e1.toString())
                                        if (!e1.isNaN() && e1 != 0.0) {
                                            player1ResultTextView.text = " = "+e1.toInt().toString()
                                        }
                                    } else if (playerUID == player2UID) {
                                        player2ExpressionTextView.text = expression
                                        val e2 = evaluateExpression(expression)
                                        Log.d("DEBUG",e2.toString())
                                        if (!e2.isNaN() && e2 != 0.0) {
                                            player2ResultTextView.text = " = "+e2.toInt().toString()
                                        }
                                    }
                                }
                                }
                            }

                            "playerMeta" -> {
                                val role = json.getString("opponent")
                                val uid = json.getString("player")
                                runOnUiThread {
                                    if (role == "Time") {
                                        gameDurationSeconds = uid.toLong()
                                        Log.d("Game", "Game Duration: $gameDurationSeconds")
                                        if (!timerStarted) {
                                            startTimer()
                                        }

                                    } else {

                                        FirebaseFirestore.getInstance().collection("Users")
                                            .document(uid).get()
                                            .addOnSuccessListener { document ->
                                                val name =
                                                    document.getString("Username") ?: "Anonymous"
                                                runOnUiThread {
                                                    if (role == "Player1") {
                                                        player1UID = uid
                                                        player1NameTextView.text = "Player 1: $name"
                                                    } else if (role == "Player2") {
                                                        player2UID = uid
                                                        player2NameTextView.text = "Player 2: $name"
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                runOnUiThread {
                                                    if (role == "Player1") {
                                                        player1NameTextView.text =
                                                            "Player 1: (Error)"
                                                    } else if (role == "Player2") {
                                                        player2NameTextView.text =
                                                            "Player 2: (Error)"
                                                    }
                                                }
                                            }
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
                                val uid = json.getString("opponent")
                                val feedback = json.getString("expression")
                                Log.d("Spectator", "Feedback update received for UID: $uid -> Feedback: $feedback")

                                runOnUiThread {
                                    if (uid == player1UID) {
                                        Log.d("Spectator", "Setting feedback for Player 1")
                                        player1FeedbackTextView.text = feedback
                                    } else if (uid == player2UID) {
                                        Log.d("Spectator", "Setting feedback for Player 2")
                                        player2FeedbackTextView.text = feedback
                                    } else {
                                        Log.w("Spectator", "Unknown UID in feedback update: $uid")
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
    private fun evaluateExpression(expression: String): Double {
        return try {
            if (expression.isBlank()) {
                Log.e("ERROR", "Attempted to evaluate a blank expression.")
                return Double.NaN
            }

            val result = ExpressionBuilder(expression).build().evaluate()
            Log.d("DEBUG", "Evaluation Result: $result")
            result
        } catch (e: Exception) {
            Log.e("ERROR", "Expression evaluation failed: ${e.message}")
            Double.NaN  // Return NaN if there's an error
        }
    }

    private fun reconnectWebSocket() {
        handler.postDelayed({
            setupWebSocket()
        }, 800)
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
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
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
