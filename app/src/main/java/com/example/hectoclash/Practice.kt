package com.example.hectoclash

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.dotlottie.dlplayer.Mode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.util.toColor
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation
import net.objecthunter.exp4j.ExpressionBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class Practice : AppCompatActivity() {

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
    private var currentExpression = ""
    private var nextNumberIndex = 0
    private var gameStartTime: Long = 0
    private var gameDurationSeconds: Long = 120

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(R.layout.activity_practice)
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

        textViewTimer = findViewById(R.id.textViewTimer)
        textViewPuzzle = findViewById(R.id.textViewPuzzle)
        textViewExpression = findViewById(R.id.textViewExpression)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        textViewFeedback = findViewById(R.id.textViewFeedback)
        gridNumbers = findViewById(R.id.gridNumbers)
        gridOperators = findViewById(R.id.gridOperators)

        setupProblem()
        setButtonAppearance(buttonSubmit,("#D4AF37".toColor()), "#FFFFFF".toColor(), 80f)
        buttonSubmit.setOnClickListener { validateSolution() }

        textViewTimer.text = "Time Left: 120s"
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
            }
        }.start()
    }


    private fun setupProblem() {
        val puzzle = (1..6).map { (1..9).random() }.joinToString("")
        originalPuzzle = puzzle
        textViewPuzzle.text = "Solve: $originalPuzzle = 100"
        setupButtons()
        startTimer()

    }

    private fun validateSolution() {
        val result = evaluateExpression(currentExpression)
        if (result == 100.0) {
            textViewFeedback.text = "🎉 Correct! You Won!"
            textViewFeedback.setTextColor("#4CAF50".toColorInt())
            buttonSubmit.isEnabled = false
            countdownTimer?.cancel()
        } else {
            textViewFeedback.text = "❌ Wrong! Try Again."
            textViewFeedback.setTextColor("#F44336".toColorInt()) // Red
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
                Log.w(
                    "WARNING",
                    "Expression does not contain exactly 6 digits. Skipping evaluation."
                )
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
                typeface = ResourcesCompat.getFont(context, R.font.russo_one)
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
        val operators = listOf("+", "-", "*", "/", "(", ")", "^", "⬅\uFE0F", "❌")
        gridOperators.columnCount = 3
        for (i in 0 until gridOperators.childCount) {
            val button = gridOperators.getChildAt(i) as Button
            if (button.text !in listOf("(", ")", "-")) {
                button.background = ContextCompat.getDrawable(this, R.drawable.custom_button2)
                button.setTextColor(Color.WHITE)
                button.typeface = ResourcesCompat.getFont(this, R.font.russo_one)
                button.setTypeface(null, Typeface.BOLD)
                button.isEnabled = false
            }
        }
        for (op in operators) {
            val button = Button(this).apply {
                text = op
                if (text == "⬅\uFE0F") {
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button_blue)
                    setTextColor(Color.WHITE)
                    typeface = ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                } else if (text == "❌") {
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button2)
                    setTextColor(Color.WHITE)
                    typeface = ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                } else {
                    background = ContextCompat.getDrawable(context, R.drawable.custom_button_real)
                    setTextColor(Color.BLACK)
                    typeface = ResourcesCompat.getFont(context, R.font.russo_one)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 18f
                }
                setOnClickListener {
                    if (text == "(") {
                        enableMinus()
                    }
                    if (text == "⬅\uFE0F") {
                        undoLastAction()
                    } else if (text == "❌") {
                        clearExpression()
                    } else {
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
    private fun setButtonAppearance(button: Button, normalColor: Int, disabledColor: Int, radius: Float) {
        // Create ColorStateList for background tint
        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf()
        )
        val colors = intArrayOf(
            normalColor,
            disabledColor,
            normalColor
        )
        val colorStateList = ColorStateList(states, colors)
        button.backgroundTintList = colorStateList

        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = radius
        button.background = gradientDrawable
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
        countdownTimer?.cancel()
        finish()
        super.onBackPressed()
    }
}