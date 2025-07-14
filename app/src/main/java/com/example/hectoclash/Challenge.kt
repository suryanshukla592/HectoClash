package com.example.hectoclash

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.core.graphics.toColorInt

class Challenge : AppCompatActivity() {

    private lateinit var createGameButton: CardView
    private lateinit var joinGameButton: CardView
    private lateinit var gameCodeEditText: EditText
    private lateinit var textViewCreateGame: TextView
    private lateinit var textViewJoinGame: TextView
    private lateinit var shareCodeButton: ImageButton
    private lateinit var copyCodeButton: ImageButton
    private var generatedRoomId: String? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }

        createGameButton = findViewById(R.id.buttonCreateGame)
        joinGameButton = findViewById(R.id.buttonJoinGame)
        textViewCreateGame = findViewById(R.id.textViewCreateGame)
        textViewJoinGame = findViewById(R.id.textViewJoinGame)
        gameCodeEditText = findViewById(R.id.editTextGameCode)
        shareCodeButton = findViewById(R.id.buttonShareCode)
        copyCodeButton = findViewById(R.id.buttonCopyCode)
        shareCodeButton.visibility = View.GONE
        copyCodeButton.visibility = View.GONE
        gameCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setJoinCardEnabled(s.isNullOrEmpty().not(), joinGameButton, textViewJoinGame)
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
                    setJoinCardEnabled(true, joinGameButton, textViewJoinGame)
                    setJoinCardEnabled(false, createGameButton, textViewCreateGame)
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
        setJoinCardEnabled(false, joinGameButton, textViewJoinGame)
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
    fun setJoinCardEnabled(enabled: Boolean, card: CardView, textView: TextView) {
        card.isClickable = enabled
        card.isFocusable = enabled
        card.foreground.alpha = if (enabled) 255 else 80

        textView.isEnabled = enabled
        textView.setTextColor(
            if (enabled) "#000000".toColorInt()
            else "#80000000".toColorInt()
        )

        card.setCardBackgroundColor(
            if (enabled) "#F6C318".toColorInt()
            else "#80F6C318".toColorInt()
        )
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
