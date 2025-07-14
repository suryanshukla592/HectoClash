package com.example.hectoclash

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.PopupMenu
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.hectoclash.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var profileImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private val IMAGE_PICK_CODE = 104
    private val PERMISSION_CODE = 105
    private var nameint=""
    private var emailint=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
        val firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        setContentView(binding.root)
        MusicManager.startMusic(this,R.raw.home_page_music,0)
        MusicManager.setMusicVolume(this)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        profileImageUrl= intent.getStringExtra("imgu")
        nameint = intent.getStringExtra("name") ?: ""
        emailint = intent.getStringExtra("email") ?: ""
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        if (profileImageUrl != null||profileImageUrl!=""){
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.profile)
            binding.profile.visibility = View.VISIBLE
        }
        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username")
                    val url = document.getString("Profile Picture URL")
                    nameint=name.toString()
                    if(profileImageUrl==null||profileImageUrl=="") {
                        profileImageUrl = url
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.profile)
                    }
                    binding.profile.visibility = View.VISIBLE
                    binding.header.text = "Welcome $name to"
                }else {
                    binding.profile.setImageResource(R.drawable.defaultdp)
                    binding.header.text = "Welcome Unknown to"
                }
            }
        }
        updateSoundIcon(SoundManager.getSoundState(this))

        binding.soundToggle.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val newState = SoundManager.cycleSoundState(this)
            updateSoundIcon(newState)
        }
        binding.profile.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            binding.profile.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(70)
                .withEndAction {
                    binding.profile.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .withEndAction {
                            val dialog = Dialog(this)
                            dialog.setContentView(R.layout.dppopup)
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.window?.setLayout(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )

                            val viewProfilePicture =
                                dialog.findViewById<TextView>(R.id.view_profile_picture)
                            val changeProfilePicture =
                                dialog.findViewById<TextView>(R.id.change_profile_picture)
                            val yourStats = dialog.findViewById<TextView>(R.id.your_stats)
                            viewProfilePicture.setOnClickListener {
                                SfxManager.playSfx(this, R.raw.button_sound)
                                dialog.dismiss()
                                val viewdp = profileImageUrl?.let { it1 -> viewdp(it1) }
                                if (viewdp != null) {
                                    viewdp.show(supportFragmentManager, "dp_popup")
                                }
                            }
                            changeProfilePicture.setOnClickListener {
                                SfxManager.playSfx(this, R.raw.button_sound)
                                dialog.dismiss()
                                checkPermOpenDialog()
                            }
                            yourStats.setOnClickListener {
                                SfxManager.playSfx(this, R.raw.button_sound)
                                val intent = Intent(this, stats::class.java)
                                dialog.dismiss()
                                startActivity(intent)
                        }


                            dialog.show()
                        }
                        .start()
                }
                .start()
        }
        binding.options.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            showPopupMenu()
        }

        val buttonStartGame: CardView = findViewById(R.id.buttonStartGame)
        val buttonLeaderboard: CardView = findViewById(R.id.buttonLeaderboard)
        val buttonPractice: CardView = findViewById(R.id.buttonPractice)
        val buttonSpectate: CardView = findViewById(R.id.buttonSpectate)
        val buttonHowToPlay: CardView = findViewById(R.id.buttonHowToPlay)
        val buttonChallenge: CardView = findViewById(R.id.buttonChallenge)

        // Navigate to the Game Activity
        buttonStartGame.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
        buttonChallenge.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, Challenge::class.java)
            startActivity(intent)
        }
        buttonPractice.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, Practice::class.java)
            startActivity(intent)
        }

        // Navigate to the Leaderboard Activity
        buttonLeaderboard.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        buttonHowToPlay.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, HowToPlay::class.java)
            startActivity(intent)
        }

//         Navigate to the Spectator Mode Activity
        buttonSpectate.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            MusicState.lastSeekTime = MusicManager.getCurrentSeekTime()
            val intent = Intent(this, Spectator::class.java)
            startActivity(intent)
        }
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MusicManager.stopMusic()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
    private fun updateSoundIcon(state: SoundState) {
        when (state) {
            SoundState.ON -> binding.soundToggle.setImageResource(R.drawable.on)
            SoundState.HALF -> binding.soundToggle.setImageResource(R.drawable.down)
            SoundState.OFF -> binding.soundToggle.setImageResource(R.drawable.off)
        }

    }
    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.options)
        popupMenu.menuInflater.inflate(R.menu.homepage_menu, popupMenu.menu)
        try {
            val field = PopupMenu::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuHelper = field.get(popupMenu)
            val menu: Menu = popupMenu.menu
            val classPopupHelper = Class.forName(menuHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuHelper, true)
            val deleteAccountItem = menu.findItem(R.id.delete_account)
            val signoutitem = menu.findItem(R.id.sign_out)
            val aboutusitem = menu.findItem(R.id.aboutus)
            val deleteAccountTitle = SpannableString(deleteAccountItem.title)
            deleteAccountTitle.setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, deleteAccountTitle.length, 0)
            deleteAccountItem.title = deleteAccountTitle
            deleteAccountItem.icon?.let {
                DrawableCompat.setTint(it, Color.parseColor("#D32F2F"))
            }
            aboutusitem.icon?.let {
                DrawableCompat.setTint(it, Color.WHITE)
            }
           signoutitem.icon?.let {
                DrawableCompat.setTint(it, Color.WHITE)
            }
        } catch (e: Exception) {}

        val firebaseAuth = FirebaseAuth.getInstance()
        popupMenu.setOnMenuItemClickListener { menuItem ->
            SfxManager.playSfx(this, R.raw.button_sound)
            when (menuItem.itemId) {
                R.id.sign_out -> {
                    val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                    googleSignInClient.signOut()
                    firebaseAuth.signOut()
                    Toast.makeText(this, "Signed Out!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, opening::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.delete_account -> {
                    showDeleteConfirmationDialog(firebaseAuth)
                    true
                }
                R.id.aboutus -> {
                    val intent = Intent(this, AboutUs::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    private fun showDeleteConfirmationDialog(firebaseAuth: FirebaseAuth) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.deleteddialogue, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
        val alertDialog = builder.create()
        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
        val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
        btnYes.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            deleteUserAccount(firebaseAuth)
            alertDialog.dismiss()
        }

        btnNo.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun deleteUserAccount(firebaseAuth: FirebaseAuth) {
        val user: FirebaseUser? = firebaseAuth.currentUser
        val db = Firebase.firestore

        if (user != null) {
            val userID = user.uid
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    db.collection("Users").document(userID).delete()
                    Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                    val intent = Intent(this, opening::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is currently signed in.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkPermOpenDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_CODE)
            } else {
                pickImageFromChooser()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_CODE)
            } else {
                pickImageFromChooser()
            }
        }
    }
    private fun pickImageFromChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        startActivityForResult(Intent.createChooser(intent, "Select Image"), IMAGE_PICK_CODE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromChooser()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val db = Firebase.firestore
            val user = firebaseAuth.currentUser
            val userID = user?.uid
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                val contentResolver: ContentResolver = contentResolver
                selectedImageUri?.let { it1 ->
                    uploadImageToCloudinary(it1, contentResolver, userID) { url ->
                        if (url != null) {
                            if (userID != null) {
                                db.collection("Users").document(userID).update("Profile Picture URL", url)
                            }
                            Toast.makeText(this, "Profile Picture Updated !!", Toast.LENGTH_SHORT).show()
                            profileImageUrl=url
                            Glide.with(this).load(url).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(binding.profile)
                        }
                    }
                }
            }
        }
    }
    private fun uploadImageToCloudinary(uri: Uri, contentResolver: ContentResolver, UserID: String?=null, onSuccess: (String?) -> Unit) {
        contentResolver.openInputStream(uri)?.readBytes()?.let { byteArray ->
            val publicId = "ProfilePicture/${UserID ?: "default_filename"}"
            MediaManager.get().upload(byteArray)
                .option("resource_type", "image")
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        val imageUrl = resultData?.get("url")?.toString()
                        val secureImageUrl = if (imageUrl != null && imageUrl.startsWith("http://")) {
                            imageUrl.replace("http://", "https://")
                        } else {
                            imageUrl
                        }
                        onSuccess(secureImageUrl)
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {}
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.stopMusic()
    }
    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }
}
