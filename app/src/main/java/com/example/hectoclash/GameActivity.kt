package com.example.hectoclash

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.model.Config
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import net.objecthunter.exp4j.ExpressionBuilder
import org.java_websocket.client.WebSocketClient
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide

class GameActivity : AppCompatActivity() {

    private lateinit var textViewTimer: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var textViewPuzzle: TextView
    private lateinit var textViewExpression: TextView
    private lateinit var buttonSubmit: Button
    private lateinit var textViewFeedback: TextView
    private lateinit var gridNumbers: GridLayout
    private lateinit var gridOperators: GridLayout

    private var originalPuzzle: String = ""
    private var countdownTimer: CountDownTimer? = null
    private var webSocketClient: WebSocketClient? = null
    private var currentExpression = ""
    private var nextNumberIndex = 0
    private var gameStartTime: Long = 0
    private var gameDurationSeconds: Long = 120
    private var roomId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(R.layout.activity_game)
        val firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid

        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        val profilePicture: ImageView = findViewById(R.id.imageViewPlayerProfile)
        val nameText: TextView = findViewById(R.id.textViewPlayerName)
        val ratingText: TextView = findViewById(R.id.textViewPlayerRating)
        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username") ?: "Unknown"
                    val url = document.getString("Profile Picture URL")
                    val rating = document.getDouble("Rating") ?: 0.0
//                    val avgTime = document.getDouble("Average Time") ?: 0.0
//                    val accuracy = document.getDouble("Accuracy") ?: 0.0
//                    val matchesPlayed = document.getLong("Matches Played") ?: 0
//                    val matchesLost = document.getLong("Matches Lost") ?: 0

                    nameText.text = name
                    ratingText.text = "Rating: $rating"
//                    avgTimeText.text = "⏱ Average Time: ${avgTime}s"
//                    accuracyText.text = "🎯 Accuracy: ${accuracy}%"
//                    matchesPlayedText.text = "🎮 Matches Played: $matchesPlayed"
//                    matchesLostText.text = "❌ Matches Lost: $matchesLost"

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp)
                            .centerCrop().into(profilePicture)
                    }
                }
            }
        }
        val dotLottieAnimationView = findViewById<DotLottieAnimation>(R.id.lottie_view)


        val config = Config.Builder()
            .autoplay(true)
            .speed(1f)
            .loop(true)
            .source(DotLottieSource.Asset("loading.json"))
            .useFrameInterpolation(true)
            .playMode(Mode.FORWARD)
            .build()
        dotLottieAnimationView.load(config)


        textViewTimer = findViewById(R.id.textViewTimer)
        textViewPuzzle = findViewById(R.id.textViewPuzzle)
        textViewExpression = findViewById(R.id.textViewExpression)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        textViewFeedback = findViewById(R.id.textViewFeedback)
        gridNumbers = findViewById(R.id.gridNumbers)
        gridOperators = findViewById(R.id.gridOperators)

        setupWebSocket()
        textViewExpression.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val expression = s.toString()
                sendExpressionUpdate(expression)
            }
        })

        buttonSubmit.setOnClickListener { sendSolutionToServer() }

        textViewTimer.text = "Time Left: 120s"
    }
    private fun sendExpressionUpdate(expression: String) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val uid = user?.uid

        if (uid == null) {
            Log.e("WebSocket", "UID is null. Cannot send expression update.")
            return
        }

        val json = JSONObject().apply {
            put("type", "expressionUpdate")
            put("expression", expression)
            put("room_id", roomId)
            put("opponent", uid)
        }
        webSocketClient?.send(json.toString())
    }
    private fun startTimer() {
        gameStartTime = System.currentTimeMillis()
        countdownTimer = object : CountDownTimer(gameDurationSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                textViewTimer.text = "Time Left: ${secondsRemaining}s"

                // Change color to red in last 30 seconds
                if (secondsRemaining <= 30) {
                    textViewTimer.setTextColor("#FF5555".toColorInt())
                } else {
                    textViewTimer.setTextColor("#D49337".toColorInt())
                }
            }

            override fun onFinish() {
                textViewTimer.text = "Time's Up!"
                textViewTimer.setTextColor("#FF5555".toColorInt()) // Ensure final message is red
                buttonSubmit.isEnabled = false
                sendResultToServer("Timeout") // Inform server about timeout
            }
        }.start()
    }


    private fun setupWebSocket() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val uid = user?.uid
        val db = Firebase.firestore
        Log.d("WebSocket", "Connecting with UID: $uid")

        if (uid == null) {
            Log.e("WebSocket", "UID is null. User is not authenticated.")
            return // Exit early if UID is null
        }

        val serverUri = "ws://3.111.203.229:8080/ws" // Replace with your WebSocket server URL

        webSocketClient = object : WebSocketClient(URI(serverUri)) {
            private val mainHandler = Handler(Looper.getMainLooper())

            override fun onOpen(handshakedata: ServerHandshake?) {
                mainHandler.post {
                    Log.d("WebSocket", "✅ Connected to duel server!")
                }

                // Creating matchmaking request
                val request = JSONObject()
                request.put("uid", uid)
                send(request.toString())
                mainHandler.post {
                    Log.d("WebSocket", "📤 Sent matchmaking request with UID: $uid")
                }
            }

            override fun onMessage(message: String?) {
                if (message == null) return

                Log.d("WebSocket", "📩 Message from server: $message")

                try {
                    val json = JSONObject(message)
                    when (json.getString("type")) {
                        "start" -> {
                            val puzzle = json.getString("content")
                            roomId = json.getString("room_id")
                            val opponentID1 = json.getString("opponent")
                            val opponentID2 = json.getString("player")
                            Log.d("WebSocket", "🚀 Match found! Puzzle: $puzzle, Room ID: $roomId")
                            var opponentID = opponentID1
                            if(opponentID1==uid)
                            {
                                opponentID = opponentID2
                            }else{
                                opponentID = opponentID1
                            }

                            // Handle game start logic
                            originalPuzzle = puzzle
                            runOnUiThread {
                                val profilePicture2: ImageView = findViewById(R.id.imageViewOpponentProfile)
                                val nameText2: TextView = findViewById(R.id.textViewOpponentName)
                                val ratingText2: TextView = findViewById(R.id.textViewOpponentRating)
                                if (opponentID != null) {
                                    db.collection("Users").document(opponentID).get().addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val name = document.getString("Username") ?: "Unknown"
                                            val url = document.getString("Profile Picture URL")
                                            val rating = document.getDouble("Rating") ?: 0.0
                        //                    val avgTime = document.getDouble("Average Time") ?: 0.0
                        //                    val accuracy = document.getDouble("Accuracy") ?: 0.0
                        //                    val matchesPlayed = document.getLong("Matches Played") ?: 0
                        //                    val matchesLost = document.getLong("Matches Lost") ?: 0

                                            nameText2.text = name
                                            ratingText2.text = "Rating: $rating"
                        //                    avgTimeText.text = "⏱ Average Time: ${avgTime}s"
                        //                    accuracyText.text = "🎯 Accuracy: ${accuracy}%"
                        //                    matchesPlayedText.text = "🎮 Matches Played: $matchesPlayed"
                        //                    matchesLostText.text = "❌ Matches Lost: $matchesLost"

                                            if (!url.isNullOrEmpty()) {
                                                Glide.with(profilePicture2.context).load(url).placeholder(R.drawable.defaultdp)
                                                    .centerCrop().into(profilePicture2)
                                            }
                                        }
                                    }
                                }
                                textViewPuzzle.text = "Solve: $originalPuzzle = 100"
                                setupButtons()  // Setup buttons based on the puzzle
                                val match: LinearLayout = findViewById(R.id.match)
                                val matchmaking: LinearLayout = findViewById(R.id.matchmaking)
                                match.visibility = View.VISIBLE
                                matchmaking.visibility = View.GONE
                                startTimer()
                            }
                        }

                        "feedback" -> {
                            val feedback = json.getString("content")
                            Log.d("WebSocket", "💬 Feedback from server: $feedback")
                            runOnUiThread {
                                textViewFeedback.text = feedback
                                buttonSubmit.isEnabled = true // Re-enable submit button after feedback
                            }
                        }
                        // When the game is over and the result is available
                        "result" -> {
                            val result = json.getString("content")
                            Log.d("WebSocket", "🏆 Game Over: $result")

                            // Handle the game result here
                            runOnUiThread {
                                textViewFeedback.text = "$result"
                                countdownTimer?.cancel() // Stop the timer when the game ends
                                buttonSubmit.isEnabled = false // Disable submit after game over
                            }
                        }

                        else -> {
                            Log.w("WebSocket", "⚠️ Unknown message type: ${json.getString("type")}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "⚠️ Error parsing server message: ${e.message}")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "❌ WebSocket closed. Code: $code, Reason: $reason, Remote: $remote")
                mainHandler.post {
                    Toast.makeText(this@GameActivity, "Disconnected from server.", Toast.LENGTH_SHORT).show()
                    finish() // Or start a new intent
                }
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "⚠️ WebSocket error: ${ex?.message}")
            }
        }

        webSocketClient?.connect()
    }

    private fun sendSolutionToServer() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val uid = user?.uid

        if (uid == null) {
            Log.e("WebSocket", "UID is null. Cannot send solution.")
            return
        }

        if (webSocketClient == null || !webSocketClient!!.isOpen) {
            Log.e("WebSocket", "WebSocket is not connected. Cannot send solution.")
            return
        }

        val expressionToSend = currentExpression
        Log.d("WebSocket", "Attempting to evaluate expression: '$expressionToSend'")

        if (expressionToSend.isNullOrBlank()) {
            Log.e("ERROR", "Expression is empty or null!")
            runOnUiThread {
                textViewFeedback.text = "Invalid expression!"
            }
            return
        }

        val result = evaluateExpression(expressionToSend)
        Log.d("WebSocket", "Evaluation Result: $result")

        if (result.isNaN()) {
            Log.e("ERROR", "Expression evaluation failed!")
            runOnUiThread {
                textViewFeedback.text = "Error in expression! Check operators."
            }
            return
        }

        try {
            val solutionJson = JSONObject().apply {
                put("type", "submit") // Use "submit" type
                put("answer", result.toString()) // Send the numerical result as a string
                put("uid", uid)
                put("expression",expressionToSend)
                put("room_id", roomId)
            }

            webSocketClient!!.send(solutionJson.toString())
            Log.d("WebSocket", "Sent answer '$result' to server with UID: $uid, Room ID: $roomId")
            runOnUiThread {
                textViewFeedback.text = "Submitting..." // Provide feedback
                buttonSubmit.isEnabled = false // Disable submit until feedback
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Failed to send solution: ${e.message}", e)
            runOnUiThread {
                textViewFeedback.text = "Error sending solution."
                buttonSubmit.isEnabled = true
            }
        }
    }

    private fun sendResultToServer(result: String) { // For sending timeout
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val uid = user?.uid

        if (uid == null) {
            Log.e("WebSocket", "UID is null. Cannot send result: $result")
            return
        }

        if (webSocketClient == null || !webSocketClient!!.isOpen) {
            Log.e("WebSocket", "WebSocket is not connected. Cannot send result: $result")
            return
        }

        try {
            val resultJson = JSONObject().apply {
                put("type", "result")
                put("content", result)
                put("uid", uid)
                put("room_id", roomId)
            }
            webSocketClient!!.send(resultJson.toString())
            Log.d("WebSocket", "Sent result '$result' to server with UID: $uid, Room ID: $roomId")
        } catch (e: Exception) {
            Log.e("WebSocket", "Failed to send result: ${e.message}", e)
        }
    }
    private fun evaluateExpression(expression: String): Double {
        return try {
            if (expression.isBlank()) {
                Log.e("ERROR", "Attempted to evaluate a blank expression.")
                return Double.NaN
            }

            // Check if the expression contains exactly 6 digits
            val digitCount = expression.count { it.isDigit() }
            if (digitCount != 6) {
                Log.w("WARNING", "Expression does not contain exactly 6 digits. Skipping evaluation.")
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
    private fun setupButtons() {
        gridNumbers.removeAllViews()
        gridOperators.removeAllViews()
        nextNumberIndex = 0
        currentExpression = ""
        textViewExpression.text = "Your Answer"
        addNumberButtons()
        addOperatorButtons()
    }
    private fun addNumberButtons() {
        gridNumbers.columnCount = 3
        for (i in originalPuzzle.indices) {
            val button = Button(this).apply {
                background = ContextCompat.getDrawable(context, R.drawable.custom_button_real)
                setTextColor(Color.BLACK)
                typeface= ResourcesCompat.getFont(context, R.font.russo_one)
                setTypeface(null, Typeface.BOLD)
                textSize = 18f
                text = originalPuzzle[i].toString()
                isEnabled = i == 0  // Enable only the first number initially
                setOnClickListener {
                    currentExpression += text
                    textViewExpression.text = currentExpression
                    isEnabled = false
                    nextNumberIndex++
                    if (nextNumberIndex < originalPuzzle.length) {
                        gridNumbers.getChildAt(nextNumberIndex)?.isEnabled = true
                    }
                    enableOperators()
                }
            }
            gridNumbers.addView(button)

        }
    }
    private fun addOperatorButtons() {
        val operators = listOf("+", "-", "*", "/", "(", ")", "^", "⬅\uFE0F","❌")
        gridOperators.columnCount = 3
        for (i in 0 until gridOperators.childCount) {
            val button = gridOperators.getChildAt(i) as Button
            if (button.text !in listOf("(", ")","-")) {
                button.background = ContextCompat.getDrawable(this, R.drawable.custom_button2)
                button.setTextColor(Color.WHITE)
                button.typeface= ResourcesCompat.getFont(this, R.font.russo_one)
                button.setTypeface(null, Typeface.BOLD)
                button.isEnabled = false
            }
        }
        for (op in operators) {
            val button = Button(this).apply {
                text = op
                if(text=="⬅\uFE0F")
                {
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button_blue)
                    setTextColor(Color.WHITE)
                    typeface= ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                }else if(text=="❌"){
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button2)
                    setTextColor(Color.WHITE)
                    typeface= ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                }else {
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button_real)
                    setTextColor(Color.BLACK)
                    typeface= ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                    }
                setOnClickListener {
                    if(text=="(")
                    {
                        enableMinus()
                    }
                    if(text=="⬅\uFE0F")
                    {
                        undoLastAction()
                    }else if(text=="❌"){
                        clearExpression()
                    }else {
                        currentExpression += text
                        textViewExpression.text = currentExpression
                        if (op !in listOf("(", ")")) {
                            disableOperatorsExceptBrackets()
                        }
                    }
                }
            }
            gridOperators.addView(button)
            disableOperatorsExceptBrackets()
            enableMinus()
        }
    }
    private fun enableMinus() {
        for (i in 0 until gridOperators.childCount) {
            val button = gridOperators.getChildAt(i) as Button
            if (button.text in listOf("-")) {
                button.isEnabled = true
            }
        }
    }
    private fun enableOperators() {
        for (i in 0 until gridOperators.childCount) {
            val button = gridOperators.getChildAt(i) as Button
            button.isEnabled = true
        }
    }
    private fun disableOperatorsExceptBrackets() {
        for (i in 0 until gridOperators.childCount) {
            val button = gridOperators.getChildAt(i) as Button
            if (button.text !in listOf("(", ")","⬅\uFE0F","❌")) {
                button.isEnabled = false
            }
        }
    }
    private fun undoLastAction() {
        if (currentExpression.isNotEmpty()) {
            val lastChar = currentExpression.last()
            currentExpression = currentExpression.dropLast(1)
            textViewExpression.text = currentExpression

            // If last character was a digit, re-enable the previous number
            if (lastChar.isDigit() && nextNumberIndex > 0) {
                nextNumberIndex--
                gridNumbers.getChildAt(nextNumberIndex)?.isEnabled = true

                // 🚀 Disable the next number to maintain sequence
                if (nextNumberIndex + 1 < gridNumbers.childCount) {
                    gridNumbers.getChildAt(nextNumberIndex + 1)?.isEnabled = false
                }
            }
            if ((lastChar.isDigit()||lastChar=='('||lastChar==')')) {
                disableOperatorsExceptBrackets()
            } else {
                enableOperators()
            }
        }
    }
    private fun clearExpression() {
        currentExpression = ""
        textViewExpression.text = "Expression: "
        nextNumberIndex = 0
        setupButtons()
    }
    override fun onBackPressed() {
        Log.d("GameActivity", "onBackPressed() called")
        webSocketClient?.close()
        disconnectWebSocket()
        countdownTimer?.cancel()
        finish()
        super.onBackPressed()
    }
    private fun disconnectWebSocket() {
        Log.d("GameActivity", "Disconnecting WebSocket...")
        if (webSocketClient != null) {
            if(webSocketClient!!.isOpen){
                webSocketClient!!.close()
            }
            webSocketClient = null //release the object
            Log.d("GameActivity", "WebSocket disconnected.")
        }else{
            Log.d("GameActivity", "WebSocket is already disconnected or null.")
        }
    }
}
