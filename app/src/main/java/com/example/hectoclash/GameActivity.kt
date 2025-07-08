package com.example.hectoclash

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import net.objecthunter.exp4j.ExpressionBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import kotlin.math.abs

class GameActivity : AppCompatActivity() {

    private lateinit var textViewTimer: TextView
    private lateinit var textViewPuzzle: TextView
    private lateinit var textViewExpression: TextView
    private lateinit var buttonSubmit: Button
    private lateinit var Rematch: Button
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
    private var code: String = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(R.layout.activity_game)
        val firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val appLinkData: Uri? = intent?.data
        val appLinkAction: String? = intent?.action
        code = when {
            appLinkAction == Intent.ACTION_VIEW && appLinkData != null &&
                    appLinkData.scheme == "hectoclash" &&
                    appLinkData.host == "game" &&
                    appLinkData.pathSegments.size >= 2 &&
                    appLinkData.pathSegments[0] == "join" -> {
                val extractedCode = appLinkData.pathSegments[1]
                Log.d("GameActivity", "P1: Code from Deep Link: $extractedCode")
                extractedCode
            }
            intent?.hasExtra("code") == true -> {
                val extractedCode = intent.getStringExtra("code") ?: "default"
                Log.d("GameActivity", "P2: Code from Intent Extra: $extractedCode")
                extractedCode
            }
            else -> {
                Log.d("GameActivity", "P3: No code found, using default.")
                "default"
            }
        }
        if(code!="default") {
            checkGame(code)
        }
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        val profilePicture: ImageView = findViewById(R.id.imageViewPlayerProfile)
        val nameText: TextView = findViewById(R.id.textViewPlayerName)
        val ratingText: TextView = findViewById(R.id.textViewPlayerRating)
        val avgTimeText: TextView = findViewById(R.id.textViewPlayerAvgTime)
        val accuracyText: TextView = findViewById(R.id.textViewPlayerAccuracy)
        val gameCodeDisplay: TextView = findViewById(R.id.textViewDisplayedGameCode)
        val gameCodeCopy: ImageButton = findViewById(R.id.buttonCopyDisplayedCode)
        val gameCodeShare: ImageButton = findViewById(R.id.buttonShareDisplayedCode)
        val gameCodeLayout: LinearLayout = findViewById(R.id.gameCodeDisplaySection)
        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username") ?: "Unknown"
                    val url = document.getString("Profile Picture URL")
                    val rating = document.getDouble("Rating")?.toInt() ?:0
                    val avgTime = document.getDouble("Time")?.toInt() ?:0
                    val accuracy = document.getDouble("Accuracy")?.toInt() ?:0

                    nameText.text = name
                    ratingText.text = "Rating: $rating"
                    avgTimeText.text = "⏱ : ${avgTime}s"
                    accuracyText.text = "🎯 : ${accuracy}%"

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp)
                            .centerCrop().into(profilePicture)
                    }
                }
            }
        }
        if (code!="default" && code!="")
        {
            gameCodeLayout.visibility=View.VISIBLE
            gameCodeDisplay.text = code
        }
        gameCodeShare.setOnClickListener {
                SfxManager.playSfx(this, R.raw.button_sound)

                val roomId = code
                val link = "https://itzshuvrodip.github.io/hectoclash-redirect/redirect.html?gameCode=$roomId"

                val shareText = """
                🎮 Join me on *HectoClash*!
                
                🔥 Game Code: *$roomId*
                📲 Tap to join: $link
                
                Let's clash it out! 💥
            """.trimIndent()

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Join my HectoClash game!")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                startActivity(Intent.createChooser(intent, "Share via"))
        }

        gameCodeCopy.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val code = code
            if (code.isNotEmpty()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Game Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied code: $code", Toast.LENGTH_SHORT).show()
            }
        }


        textViewTimer = findViewById(R.id.textViewTimer)
        textViewPuzzle = findViewById(R.id.textViewPuzzle)
        textViewExpression = findViewById(R.id.textViewExpression)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        Rematch = findViewById(R.id.Rematch)
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
        buttonSubmit.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            sendSolutionToServer()
        }
        Rematch.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicManager.stopMusic()
            countdownTimer?.cancel()
            if (code.isNotEmpty() && code != "default") {
                val roomRef = db.collection("Private").document(code)
                roomRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists() && document.getString("status") == "waiting") {
                                    val gameIntent = Intent(this, GameActivity::class.java)
                                    gameIntent.putExtra("code", code)
                                    startActivity(gameIntent)
                                    finish()
                        } else {
                            val roomData = hashMapOf("createdAt" to System.currentTimeMillis(), "status" to "created")
                            db.collection("Private").document(code)
                                .set(roomData)
                                .addOnSuccessListener {
                                    val gameIntent = Intent(this, GameActivity::class.java)
                                    gameIntent.putExtra("code", code)
                                    startActivity(gameIntent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to create new room", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to access room info", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val gameIntent = Intent(this, GameActivity::class.java)
                startActivity(gameIntent)
                finish()
            }
        }
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("GameActivity", "onBackPressed() called")
                webSocketClient?.close()
                disconnectWebSocket()
                deleteCode()
                MusicManager.stopMusic()
                countdownTimer?.cancel()
                MusicManager.startMusic(this@GameActivity,R.raw.home_page_music)
                MusicManager.setMusicVolume(this@GameActivity)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
                    MusicManager.startMusic(this@GameActivity,R.raw.clock_ticking)
                    MusicManager.setMusicVolume(this@GameActivity)
                } else {
                    textViewTimer.setTextColor("#D49337".toColorInt())
                }
            }

            override fun onFinish() {
                textViewTimer.text = "Time's Up!"
                MusicManager.stopMusic()
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
                request.put("code", code)
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
                            var opponentID: String
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
                                val avgTimeText2: TextView = findViewById(R.id.textViewOpponentAvgTime)
                                val accuracyText2: TextView = findViewById(R.id.textViewOpponentAccuracy)
                                db.collection("Users").document(opponentID).get().addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val name = document.getString("Username") ?: "Unknown"
                                        val url = document.getString("Profile Picture URL")
                                        val rating = document.getDouble("Rating")?.toInt() ?:0
                                        val avgTime = document.getDouble("Time")?.toInt() ?:0
                                        val accuracy = document.getDouble("Accuracy")?.toInt() ?:0
                                        nameText2.text = name
                                        ratingText2.text = "Rating: $rating"
                                        ratingText2.text = "Rating: $rating"
                                        avgTimeText2.text = "⏱ : ${avgTime}s"
                                        accuracyText2.text = "🎯 : ${accuracy}%"

                                        if (!url.isNullOrEmpty()) {
                                            Glide.with(profilePicture2.context).load(url).placeholder(R.drawable.defaultdp)
                                                .centerCrop().into(profilePicture2)
                                        }
                                    }
                                }
                                textViewPuzzle.text = "Solve: $originalPuzzle = 100"
                                setupButtons()  // Setup buttons based on the puzzle
                                val match: LinearLayout = findViewById(R.id.match)
                                val matchmaking: RelativeLayout = findViewById(R.id.matchmakingLayout)
                                match.visibility=View.VISIBLE
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
                                val currentExpressionText = textViewExpression.text?.toString()
                                if (currentExpressionText == "Your Answer" || currentExpressionText == "" || currentExpressionText == null) {
                                    textViewExpression.text = "No Answer Submitted"
                                }
                                countdownTimer?.cancel() // Stop the timer when the game ends
                                MusicManager.stopMusic()
                                buttonSubmit.isEnabled = false // Disable submit after game over
                                val (sol1, sol2, sol3) = solveHectocTop3(originalPuzzle)
                                showPossibleSolutionsPopup(this@GameActivity, sol1, sol2, sol3)
                                Rematch.visibility=View.VISIBLE
                                gridNumbers.visibility=View.GONE
                                buttonSubmit.visibility = View.GONE
                                gridOperators.visibility=View.GONE
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

            override fun onClose(code2: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "❌ WebSocket closed. Code: $code2, Reason: $reason, Remote: $remote")
                if (code != "default" && code != "") {
                    val roomId = intent.getStringExtra("code") ?: return
                    val db = Firebase.firestore
                    val roomRef = db.collection("Private").document(roomId)
                    roomRef.get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val status = document.getString("status")
                                if (status == "waiting") {
                                    roomRef.delete()
                                        .addOnSuccessListener {
                                            Log.d("WebSocket", "🧹 Deleted waiting room: $roomId")
                                            Toast.makeText(this@GameActivity, "Matchmaking Cancelled", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Log.e(
                                                "WebSocket",
                                                "⚠️ Failed to delete room: $roomId",
                                                it
                                            )
                                            Toast.makeText(this@GameActivity, "Matchmaking Cancelled", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                }
                            }
                        }
                        .addOnFailureListener {
                            Log.e("WebSocket", "⚠️ Failed to check room status on disconnect", it)
                            Toast.makeText(this@GameActivity, "Matchmaking Cancelled", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }else{
                    mainHandler.post {
                        Toast.makeText(this@GameActivity, "Matchmaking Cancelled", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "⚠️ WebSocket error: ${ex?.message}")
            }
        }

        webSocketClient?.connect()
    }
    fun showPossibleSolutionsPopup(context: Context,solution1: String? = null,solution2: String? = null,solution3: String? = null    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.possible_solutions)

        val buttonClose = dialog.findViewById<ImageView>(R.id.buttonClosePopup)
        val textSolution1 = dialog.findViewById<TextView>(R.id.textViewSolution1)
        val textSolution2 = dialog.findViewById<TextView>(R.id.textViewSolution2)
        val textSolution3 = dialog.findViewById<TextView>(R.id.textViewSolution3)

        buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        textSolution1.text = "Solution 1: ${solution1 ?: "N/A"}"
        textSolution2.text = "Solution 2: ${solution2 ?: "N/A"}"
        textSolution3.text = "Solution 3: ${solution3 ?: "N/A"}"

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val layoutParams = dialog.window?.attributes
        layoutParams?.gravity = Gravity.CENTER
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        dialog.show()
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
                    SfxManager.playSfx(context, R.raw.button_sound)
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
                    SfxManager.playSfx(context, R.raw.button_sound)
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
    private fun evaluate(expression: String): Double? {
        return try {
            val exp = ExpressionBuilder(expression).build()
            exp.evaluate()
        } catch (e: Exception) {
            null
        }
    }
    private fun getDigitSplits(s: String): List<List<String>> {
        val results = mutableListOf<List<String>>()
        val n = s.length

        fun backtrack(index: Int, path: MutableList<String>) {
            if (index == n) {
                results.add(ArrayList(path))
                return
            }
            for (i in index + 1..n) {
                val part = s.substring(index, i)
                path.add(part)
                backtrack(i, path)
                path.removeAt(path.size - 1)
            }
        }

        backtrack(0, mutableListOf())
        return results
    }
    private fun generateExpressions(digits: String, operators: List<String>): Sequence<String> = sequence {
        val splits = getDigitSplits(digits)
        for (group in splits) {
            val opsCombos = cartesianProduct(operators, group.size - 1)
            for (ops in opsCombos) {
                yieldAll(applyOperators(group, ops))
            }
        }
    }
    private fun applyOperators(nums: List<String>, opsSeq: List<String>): Sequence<String> = sequence {
        if (nums.size == 1) {
            yield(nums[0])
            return@sequence
        }
        for (i in opsSeq.indices) {
            val leftNums = nums.subList(0, i + 1)
            val rightNums = nums.subList(i + 1, nums.size)
            val leftOps = opsSeq.subList(0, i)
            val rightOps = opsSeq.subList(i + 1, opsSeq.size)

            for (left in applyOperators(leftNums, leftOps)) {
                for (right in applyOperators(rightNums, rightOps)) {
                    yield("($left${opsSeq[i]}$right)")
                }
            }
        }
    }
    private fun cartesianProduct(list: List<String>, length: Int): Sequence<List<String>> = sequence {
        if (length == 0) {
            yield(emptyList())
        } else {
            for (item in list) {
                for (rest in cartesianProduct(list, length - 1)) {
                    yield(listOf(item) + rest)
                }
            }
        }
    }
    private fun solveHectocTop3(digitStr: String): Triple<String?, String?, String?> {
        val operators = listOf("+", "-", "*", "/")
        val seen = mutableSetOf<String>()
        val solutions = mutableListOf<String>()

        for (expr in generateExpressions(digitStr, operators)) {
            if (expr in seen) continue
            seen.add(expr)
            val result = evaluate(expr)
            if (result != null && abs(result - 100.0) < 1e-9) {
                solutions.add("$expr = 100")
                if (solutions.size == 3) break
            }
        }

        val string1 = solutions.getOrNull(0)
        val string2 = solutions.getOrNull(1)
        val string3 = solutions.getOrNull(2)

        return Triple(string1, string2,string3)
    }
    private fun checkGame(joinCode: String){
        val db = Firebase.firestore
        if (joinCode.isNotEmpty()) {
            val roomRef = db.collection("Private").document(joinCode)
            roomRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentStatus = document.getString("status")

                        when (currentStatus) {
                            "created" -> {
                                roomRef.update("status", "waiting")
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Waiting for opponent to join with code: $joinCode", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            "waiting" -> {
                                roomRef.delete()
                                    .addOnSuccessListener {
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to delete invalid room.", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            else
                                -> {
                                roomRef.delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Room Code Invalid !!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(this, "Failed to delete invalid room.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                    } else {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this, "Room does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    Toast.makeText(this, "Failed to check room", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun deleteCode(){
        val db = Firebase.firestore
        if (code != "default" && code != "") {
            val roomRef = db.collection("Private").document(code)
            roomRef.delete()
                .addOnSuccessListener {
                    Log.d("Challenge", "Room with code $code deleted successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Challenge", "Error deleting room with code $code", e)
                }
            code = "default"
        }
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
