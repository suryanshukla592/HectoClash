package com.example.hectoclash

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.hectoclash.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore


class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
        setContentView(binding.root)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.google.setOnClickListener{
            SfxManager.playSfx(this, R.raw.button_sound)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.signuptext.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val intent = Intent(this, SignUp::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        binding.buttonLogin.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val email = binding.Email.text.toString()
            val pass = binding.Password.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {

                    if (it.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if(user?.isEmailVerified == true) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }
                        else{
                            binding.verify.visibility = android.view.View.VISIBLE
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Toast.makeText(
                                            this, "Verification email sent. Please verify your email.", Toast.LENGTH_LONG).show()
                                        val verificationCheckThread = Thread {
                                            while (true) {
                                                user.reload()
                                                if (user.isEmailVerified) {
                                                    runOnUiThread {
                                                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                                                        val intent = Intent(this, MainActivity::class.java)
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                    break
                                                }
                                                Thread.sleep(3000)
                                            }
                                        }
                                        verificationCheckThread.start()
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else if(it.exception is FirebaseAuthInvalidUserException){
                        Toast.makeText(this, "User does not exist!", Toast.LENGTH_SHORT).show()
                    } else if(it.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this, "Invalid Credentials!", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(this, "${it.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
        binding.forgotpassword.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            showResetEmailDialog()
        }


    }
    private fun showResetEmailDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.resetemaildialogue, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnYes = dialogView.findViewById<Button>(R.id.btn_submit)
        btnYes.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val email = dialogView.findViewById<EditText>(R.id.et_email).text.toString().trim()
            if (email.isNotEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Reset Link sent to your email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "No user found with this email", Toast.LENGTH_LONG).show()
                        }}} else {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {}
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                val userID = user?.uid
                if (userID != null) {
                    val usersRef = Firebase.firestore.collection("Users").document(userID)
                    usersRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            val userMap = hashMapOf(
                                "UserID" to userID,
                                "Username" to user.displayName,
                                "Email" to user.email,
                                "Rating" to 0,
                                "Time" to 0,
                                "Accuracy" to 0,
                                "Played" to 0,
                                "Won" to 0,
                                "Profile Picture URL" to user.photoUrl.toString()
                            )
                            usersRef.set(userMap).addOnSuccessListener {
                                val intent =
                                    Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            val intent =
                                Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
