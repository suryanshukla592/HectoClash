package com.example.hectoclash

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Challenge : AppCompatActivity() {

    private lateinit var createGameButton: Button
    private lateinit var joinGameButton: Button
    private lateinit var gameCodeEditText: EditText
    private lateinit var shareCodeButton: ImageButton
    private lateinit var copyCodeButton: ImageButton
    private var generatedRoomId: String? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        createGameButton = findViewById(R.id.buttonCreateGame)
        joinGameButton = findViewById(R.id.buttonJoinGame)
        gameCodeEditText = findViewById(R.id.editTextGameCode)
        shareCodeButton = findViewById(R.id.buttonShareCode)
        copyCodeButton = findViewById(R.id.buttonCopyCode)
        val russoOneTypeface: Typeface? = ResourcesCompat.getFont(this, R.font.russo_one)
        russoOneTypeface?.let {
            createGameButton.typeface = it
        }
        createGameButton.textSize = 20f
        russoOneTypeface?.let {
            joinGameButton.typeface = it
        }
        joinGameButton.textSize = 20f
        shareCodeButton.visibility = View.GONE
        copyCodeButton.visibility = View.GONE
        gameCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                joinGameButton.isEnabled = s.isNullOrEmpty().not()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        createGameButton.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            generatedRoomId = generateRoomId()
            val roomId = generatedRoomId!!
            val roomData = hashMapOf("createdAt" to System.currentTimeMillis(), "status" to "created")

            db.collection("Private").document(roomId)
                .set(roomData)
                .addOnSuccessListener {
                    gameCodeEditText.setText(roomId)
                    gameCodeEditText.isEnabled = false
                    shareCodeButton.visibility = View.VISIBLE
                    copyCodeButton.visibility = View.VISIBLE
                    createGameButton.isEnabled = false
                    joinGameButton.isEnabled = true
                    Toast.makeText(this, "Game code generated: $roomId", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to create room", Toast.LENGTH_SHORT).show()
                }
        }

        joinGameButton.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val joinCode = gameCodeEditText.text.toString().trim().uppercase()

            if (joinCode.isNotEmpty()) {
                val roomRef = db.collection("Private").document(joinCode)
                roomRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            startGame(joinCode)
                        } else {
                            Toast.makeText(this, "Invalid Room Code", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to check room", Toast.LENGTH_SHORT).show()
                    }
            } else{
                Toast.makeText(this, "Please enter a game code", Toast.LENGTH_SHORT).show()
            }
        }

        shareCodeButton.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)

            generatedRoomId?.let { roomId ->
                val link = "https://itzshuvrodip.github.io/hectoclash-redirect/redirect.html?gameCode=$roomId"

                val shareText = """
            🎮 Join me on *HectoClash*!
            
            🔥 Game Code: *$roomId*
            📲 Tap to join: $link
            
            Let's clash it out! 💥
        """.trimIndent()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Join my HectoClash game!")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }

        copyCodeButton.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val code = generatedRoomId
            if (!code.isNullOrEmpty()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Game Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied code: $code", Toast.LENGTH_SHORT).show()
            }
        }
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                deleteCode()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun startGame(code: String) {
        MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
        val gameIntent = Intent(this, GameActivity::class.java)
        gameIntent.putExtra("code", code)
        startActivity(gameIntent)
    }

    private fun generateRoomId(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
    private fun deleteCode(){
        if (generatedRoomId != null) {
            val roomRef = db.collection("Private").document(generatedRoomId!!)
            roomRef.delete()
                .addOnSuccessListener {
                    Log.d("Challenge", "Room with code $generatedRoomId deleted successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Challenge", "Error deleting room with code $generatedRoomId", e)
                }
            generatedRoomId = null
        }
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }
    override fun onDestroy(){
        deleteCode()
        super.onDestroy()
    }
}
