package com.example.hectoclash

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import net.objecthunter.exp4j.ExpressionBuilder

class Practice : AppCompatActivity() {

    private lateinit var textViewTimer: TextView
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
        MusicManager.stopMusic()
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

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
        buttonSubmit.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            if (buttonSubmit.text == "Submit Solution") {
                validateSolution()
            }else{
                val intent = Intent(this, Practice::class.java)
                startActivity(intent)
                finish()
            }
        }

        textViewTimer.text = "Time Left: 120s"
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                countdownTimer?.cancel()
                MusicManager.stopMusic()
                MusicManager.startMusic(this@Practice,R.raw.home_page_music,MusicState.lastSeekTime)
                MusicManager.setMusicVolume(this@Practice)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
                    MusicManager.startMusic(this@Practice,R.raw.clock_ticking,0)
                    MusicManager.setMusicVolume(this@Practice)
                } else {
                    textViewTimer.setTextColor("#D49337".toColorInt())
                }
            }

            override fun onFinish() {
                textViewTimer.text = "Time's Up!"
                MusicManager.stopMusic()
                textViewTimer.setTextColor("#FF5555".toColorInt())
                SfxManager.playSfx(this@Practice, R.raw.defeat)
                val (sol1, sol2, sol3) = solveHectocTop3(originalPuzzle)
                showPossibleSolutionsPopup(this@Practice, sol1, sol2, sol3)
                if(textViewExpression.text=="Your Answer"||textViewExpression.text == ""){
                    textViewExpression.text = "No Answer Submitted"
                }
                buttonSubmit.isEnabled = false
                gridNumbers.visibility= View.GONE
                gridOperators.visibility= View.GONE
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
    fun showPossibleSolutionsPopup(context: Context, solution1: String? = null, solution2: String? = null, solution3: String? = null    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.possible_solutions)

        val buttonClose = dialog.findViewById<ImageView>(R.id.buttonClosePopup)
        val textSolution1 = dialog.findViewById<TextView>(R.id.textViewSolution1)
        val textSolution2 = dialog.findViewById<TextView>(R.id.textViewSolution2)
        val textSolution3 = dialog.findViewById<TextView>(R.id.textViewSolution3)

        buttonClose.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            buttonSubmit.text = "Play Again"
            buttonSubmit.isEnabled = true
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


    private fun validateSolution() {
        val result = evaluateExpression(currentExpression)
        if (result == 100.0) {
            textViewFeedback.text = "🎉 Correct! You Won!"
            textViewFeedback.setTextColor("#4CAF50".toColorInt())
            MusicManager.stopMusic()
            SfxManager.playSfx(this, R.raw.victory)
            countdownTimer?.cancel()
            if(textViewExpression.text=="Your Answer"||textViewExpression.text == ""){
                textViewExpression.text = "No Answer Submitted"
            }
            buttonSubmit.isEnabled = false
            gridNumbers.visibility= View.GONE
            gridOperators.visibility= View.GONE
            val (sol1, sol2, sol3) = solveHectocTop3(originalPuzzle)
            showPossibleSolutionsPopup(this@Practice, sol1, sol2, sol3)
        } else {
            textViewFeedback.text = "❌ Wrong! Try Again."
            textViewFeedback.setTextColor("#F44336".toColorInt()) // Red
            SfxManager.playSfx(this, R.raw.wrong)
        }
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
            if (result != null && Math.abs(result - 100.0) < 1e-9) {
                solutions.add("$expr = 100")
                if (solutions.size == 3) break
            }
        }

        val string1 = solutions.getOrNull(0)
        val string2 = solutions.getOrNull(1)
        val string3 = solutions.getOrNull(2)

        return Triple(string1, string2,string3)
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
                    SfxManager.playSfx(context, R.raw.button_sound)
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

    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()
    }
}