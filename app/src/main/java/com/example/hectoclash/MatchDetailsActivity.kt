package com.example.hectoclash

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.hectoclash.databinding.ActivityMatchDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore
import net.objecthunter.exp4j.ExpressionBuilder
import java.util.concurrent.TimeUnit

class MatchDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchDetailsBinding
    private lateinit var matchData: MatchHistoryEntry
    private lateinit var database: FirebaseFirestore
    var opponentImage :String = ""
    var playerImage :String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<ViewGroup>(android.R.id.content)) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
        binding = ActivityMatchDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchData = intent.getParcelableExtra("match_data")!!
        database = FirebaseFirestore.getInstance()

        loadUserData(matchData.selfUID, isPlayer = true)
        loadUserData(matchData.opponentUID, isPlayer = false)

        val timeAgo = getTimeAgo(matchData.timestamp)
        binding.textViewMatchTimestamp.text = "Played: $timeAgo ago"
        binding.textViewPuzzle.text = "Solve: ${matchData.puzzle}"
        binding.textViewFeedback.text = "${matchData.feedback}"
        val (sol1, sol2, sol3) = solveHectocTop3(matchData.puzzle)
        binding.textViewSolution1.text=sol1
        binding.textViewSolution2.text=sol2
        binding.textViewSolution3.text=sol3
        binding.imageViewPlayerProfile.setOnClickListener { it ->
            SfxManager.playSfx(this, R.raw.button_sound)
            val context = it.context
            if (context is AppCompatActivity) {
                val viewdp = viewdp(playerImage)
                viewdp.show(context.supportFragmentManager, "dp_popup")
            }
        }
        binding.imageViewOpponentProfile.setOnClickListener { it ->
            SfxManager.playSfx(this, R.raw.button_sound)
            val context = it.context
            if (context is AppCompatActivity) {
                val viewdp = viewdp(opponentImage)
                viewdp.show(context.supportFragmentManager, "dp_popup")
            }
        }


    }
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }

    private fun loadUserData(uid: String, isPlayer: Boolean) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("Username") ?: "User"
                    val rating = document.getLong("Rating")?.toString() ?: "0"
                    val accuracy = document.getDouble("Accuracy")?.toInt() ?: "0"
                    val avgTime = document.getDouble("Time")?.toInt() ?: "0"
                    val profileImage = document.getString("Profile Picture URL")?:""

                    if (isPlayer) {
                        binding.textViewPlayerName.text = name
                        binding.textViewPlayerRating.text = "Rating: $rating"
                        binding.textViewPlayerAccuracy.text = "🎯 : $accuracy%"
                        binding.textViewPlayerAvgTime.text = "⏱ : $avgTime s"
                        playerImage=profileImage
                        profileImage.let {
                            Glide.with(this).load(it).placeholder(R.drawable.defaultdp)
                                .into(binding.imageViewPlayerProfile)
                        }
                    } else {
                        binding.textViewOpponentName.text = name
                        binding.textViewOpponentRating.text = "Rating: $rating"
                        binding.textViewOpponentAccuracy.text = "🎯 : $accuracy%"
                        binding.textViewOpponentAvgTime.text = "⏱ : $avgTime s"
                        opponentImage=profileImage
                        profileImage.let {
                            Glide.with(this).load(it).placeholder(R.drawable.defaultdp)
                                .into(binding.imageViewOpponentProfile)
                        }
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
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

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes m"
            hours < 24 -> "$hours h"
            else -> "$days d"
        }
    }
}
