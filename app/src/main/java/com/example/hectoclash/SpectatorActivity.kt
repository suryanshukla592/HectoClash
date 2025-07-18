package com.example.hectoclash

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    private lateinit var gameFeed: TextView
    private lateinit var player1Crown: ImageView
    private lateinit var player2Crown: ImageView
    private lateinit var player1Rating: TextView
    private lateinit var player2Rating: TextView
    private lateinit var player1AvgTime: TextView
    private lateinit var player2AvgTime: TextView
    private lateinit var player1Accuracy: TextView
    private lateinit var player2Accuracy: TextView
    private lateinit var player1Profile: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var player2Profile: de.hdodenhof.circleimageview.CircleImageView
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
    private var isConnected = false
    private var p1dp: String = ""
    private var p2dp: String = ""

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

        // View Bindings
        puzzleTextView = findViewById(R.id.textViewFinalPuzzle)
        player1ExpressionTextView = findViewById(R.id.player1ExpressionTextView)
        player2ExpressionTextView = findViewById(R.id.player2ExpressionTextView)
        textViewTimer = findViewById(R.id.textViewTimer)
        player1Crown = findViewById(R.id.P1Crown)
        player2Crown = findViewById(R.id.P2Crown)
        player1Rating = findViewById(R.id.textViewPlayer1Rating)
        player2Rating = findViewById(R.id.textViewPlayer2Rating)
        player1AvgTime = findViewById(R.id.textViewPlayer1AvgTime)
        player2AvgTime = findViewById(R.id.textViewPlayer2AvgTime)
        player1Accuracy = findViewById(R.id.textViewPlayer1Accuracy)
        player2Accuracy = findViewById(R.id.textViewPlayer2Accuracy)
        player1Profile = findViewById(R.id.imageViewPlayer1Profile)
        player2Profile = findViewById(R.id.imageViewPlayer2Profile)
        player1FeedbackTextView = findViewById(R.id.player1FeedbackTextView)
        player2FeedbackTextView = findViewById(R.id.player2FeedbackTextView)
        player1NameTextView = findViewById(R.id.textViewPlayer1Name)
        player2NameTextView = findViewById(R.id.textViewPlayer2Name)
        player2ResultTextView = findViewById(R.id.player2ResultTextView)
        player1ResultTextView = findViewById(R.id.player1ResultTextView)
        gameFeed = findViewById(R.id.RoomFeedbackTextView)
        MusicManager.stopMusic()

        roomId = intent.getStringExtra("roomId") ?: ""
        player1Profile.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            if (p1dp != "") {
                val viewdp = viewdp(p1dp)
                viewdp.show(supportFragmentManager, "dp_popup")
            }
        }
        player2Profile.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            if (p2dp != "") {
                val viewdp = viewdp(p1dp)
                viewdp.show(supportFragmentManager, "dp_popup")
            }
        }

        setupWebSocket()
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                countdownTimer?.cancel()
                MusicManager.stopMusic()
                MusicManager.startMusic(this@SpectatorActivity,R.raw.home_page_music,MusicState.lastSeekTime)
                MusicManager.setMusicVolume(this@SpectatorActivity)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
                endSpectating()
            }
        }.start()
    }

    private fun setupWebSocket() {
        val serverUri = URI("ws://3.111.203.229:8080/ws")
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("SpectatorActivity", "WebSocket opened")
                isConnected = true
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
                                        if (!e1.isNaN()) {
                                            player1ResultTextView.text = "Live Value = "+e1.toInt().toString()
                                        }
                                    } else if (playerUID == player2UID) {
                                        player2ExpressionTextView.text = expression
                                        val e2 = evaluateExpression(expression)
                                        if (!e2.isNaN()) {
                                            player2ResultTextView.text = "Live Value = "+e2.toInt().toString()
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
                                        if (!timerStarted) {
                                            startTimer()
                                        }

                                    } else {

                                        FirebaseFirestore.getInstance().collection("Users")
                                            .document(uid).get()
                                            .addOnSuccessListener { document ->
                                                val name =
                                                    document.getString("Username") ?: "Anonymous"
                                                val rating = document.getLong("Rating") ?: 0
                                                val avgTime = document.getLong("Time") ?: 0
                                                val accuracy = document.getLong("Accuracy") ?: 0
                                                val profile = document.getString("Profile Picture URL") ?: ""
                                                runOnUiThread {
                                                    if (role == "Player1") {
                                                        player1UID = uid
                                                        p1dp = profile
                                                        player1NameTextView.text = "$name"
                                                        player1Rating.text = "Rating: $rating"
                                                        player1AvgTime.text = "⏱: ${avgTime}s"
                                                        player1Accuracy.text = "🎯: ${accuracy}%"
                                                        Glide.with(this@SpectatorActivity)
                                                            .load(profile)
                                                            .placeholder(R.drawable.defaultdp)
                                                            .centerCrop()
                                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                            .into(player1Profile)
                                                    } else if (role == "Player2") {
                                                        player2UID = uid
                                                        p2dp = profile
                                                        player2NameTextView.text = "$name"
                                                        player2Rating.text = "Rating: $rating"
                                                        player2AvgTime.text = "⏱: ${avgTime}s"
                                                        player2Accuracy.text = "🎯: ${accuracy}%"
                                                        Glide.with(this@SpectatorActivity)
                                                            .load(profile)
                                                            .placeholder(R.drawable.defaultdp)
                                                            .centerCrop()
                                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                            .into(player2Profile)
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                runOnUiThread {
                                                    if (role == "Player1") {
                                                        player1NameTextView.text =
                                                            "(Error)"
                                                    } else if (role == "Player2") {
                                                        player2NameTextView.text =
                                                            "(Error)"
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

                                runOnUiThread {
                                    when (uid) {
                                        player1UID -> {
                                            if (feedback == "Win") {
                                                player1FeedbackTextView.text = "Winner !!"
                                                player1FeedbackTextView.setTextColor("#4CAF50".toColorInt())
                                                player1Crown.visibility = View.VISIBLE
                                                player2FeedbackTextView.text = "Loser :("
                                                player2FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                gameFeed.text = "Winner Declared"
                                                MusicManager.stopMusic()
                                                countdownTimer?.cancel()
                                                endSpectating()
                                            } else if (feedback == "Left"){
                                                player2FeedbackTextView.text = "Winner !!"
                                                player2FeedbackTextView.setTextColor("#4CAF50".toColorInt())
                                                player2Crown.visibility = View.VISIBLE
                                                player1FeedbackTextView.text = "Left The Game :("
                                                player1FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                gameFeed.text = "Opponent Left The Game"
                                                MusicManager.stopMusic()
                                                countdownTimer?.cancel()
                                                endSpectating()
                                            } else if (feedback == "Draw"){
                                                player1FeedbackTextView.text = "Draw"
                                                player1FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                player2FeedbackTextView.text = "Draw"
                                                player2FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                endSpectating()
                                            } else if (feedback.contains("Incorrect")){
                                                player1FeedbackTextView.text = "❌ Incorrect Answer (${evaluateExpression(player1ExpressionTextView.text.toString())})}) Submitted"
                                                player1FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player1FeedbackTextView.text = ""
                                                }, 2000)
                                            } else if (feedback.contains("Invalid")){
                                                player1FeedbackTextView.text = "❌ Blank Answer Submitted"
                                                player1FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player1FeedbackTextView.text = ""
                                                }, 2000)
                                            } else if (feedback.contains("Error")){
                                                player1FeedbackTextView.text = "❌ Illegal Expression Submitted"
                                                player1FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player1FeedbackTextView.text = ""
                                                }, 2000)
                                            }
                                        }
                                        player2UID -> {
                                            if (feedback == "Win") {
                                                player2FeedbackTextView.text = "Winner !!"
                                                player2FeedbackTextView.setTextColor("#4CAF50".toColorInt())
                                                player2Crown.visibility = View.VISIBLE
                                                player1FeedbackTextView.text = "Loser :("
                                                player1FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                gameFeed.text = "Winner Declared"
                                                MusicManager.stopMusic()
                                                countdownTimer?.cancel()
                                                endSpectating()
                                            } else if (feedback == "Left"){
                                                player1FeedbackTextView.text = "Winner !!"
                                                player1FeedbackTextView.setTextColor("#4CAF50".toColorInt())
                                                player1Crown.visibility = View.VISIBLE
                                                player2FeedbackTextView.text = "Left The Game :("
                                                player2FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                gameFeed.text = "Opponent Left The Game"
                                                MusicManager.stopMusic()
                                                countdownTimer?.cancel()
                                                endSpectating()
                                            } else if (feedback == "Draw"){
                                                player1FeedbackTextView.text = "Draw"
                                                player1FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                player2FeedbackTextView.text = "Draw"
                                                player2FeedbackTextView.setTextColor("#FFFFFF".toColorInt())
                                                endSpectating()
                                            } else if (feedback.contains("Incorrect")){
                                                player2FeedbackTextView.text = "❌ Incorrect Answer (${evaluateExpression(player2ExpressionTextView.text.toString())})}) Submitted"
                                                player2FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player2FeedbackTextView.text = ""
                                                }, 2000)
                                            } else if (feedback.contains("Invalid")){
                                                player2FeedbackTextView.text = "❌ Blank Answer Submitted"
                                                player2FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player2FeedbackTextView.text = ""
                                                }, 2000)
                                            } else if (feedback.contains("Error")){
                                                player2FeedbackTextView.text = "❌ Illegal Expression Submitted"
                                                player2FeedbackTextView.setTextColor("#F44336".toColorInt())
                                                SfxManager.playSfx(this@SpectatorActivity, R.raw.wrong)
                                                Handler().postDelayed({
                                                    player2FeedbackTextView.text = ""
                                                }, 2000)
                                            }
                                        }
                                        else -> {
                                            Log.w("Spectator", "Unknown UID in feedback update: $uid")
                                        }
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
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("SpectatorActivity", "WebSocket closed: $code, $reason")
                isConnected = false
            }

            override fun onError(ex: Exception?) {
                Log.e("SpectatorActivity", "WebSocket error: ${ex?.message}")
                isConnected = false
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

    override fun onPause() {
        super.onPause()
        webSocketClient.close()
        isConnected = false
    }
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
        if (!isConnected) {
            setupWebSocket()  // Only reconnect if not connected
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }
    private fun endSpectating(){
        textViewTimer.text = "Game Over"
        if (gameFeed.text=="    Game Ongoing...")
        {
            gameFeed.text = "Time's Up! Match Drawn."
            gameFeed.setTextColor("#FFFFFF".toColorInt())
        }
        textViewTimer.setTextColor("#FF5555".toColorInt()) // Final red text
        MusicManager.stopMusic()
        SfxManager.playSfx(this@SpectatorActivity, R.raw.draw)
        timerStarted = false
        countdownTimer?.cancel()
        Handler().postDelayed({
            var countdown = 3
            val countdownHandler = Handler()

            val countdownRunnable = object : Runnable {
                override fun run() {
                    if (countdown > 0) {
                        gameFeed.text = "Spectating ends in ${countdown}s…"
                        countdown--
                        countdownHandler.postDelayed(this, 1000)
                    } else {
                        if (!isFinishing) {
                            webSocketClient.close()
                            finish()
                        }
                    }
                }
            }
            countdownHandler.post(countdownRunnable)
        }, 3000)
    }

    private fun sendWebSocketMessage(message: String) {
        if (webSocketClient.isOpen) {
            webSocketClient.send(message)
        } else {
            Log.w("SpectatorActivity", "WebSocket is not open!")
        }
    }
}
