package com.example.hectoclash

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.hectoclash.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore


class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, sysBars.top, 0, sysBars.bottom)
            insets
        }
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.logintext.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val intent = Intent(this, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        binding.buttonSignup.setOnClickListener{
            SfxManager.playSfx(this, R.raw.button_sound)
            val email = binding.Email1.text.toString()
            val pass = binding.Password1.text.toString()
            val confirmPass = binding.Password2.text.toString()
            val username = binding.Username.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            val userID = user?.uid
                            val userMap = hashMapOf(
                                "UserID" to userID,
                                "Username" to username,
                                "Email" to email,
                                "Rating" to 0,
                                "Played" to 0,
                                "Won" to 0,
                                "Time" to 0,
                                "Accuracy" to 0
                            )
                            if (userID != null)
                            {
                                db.collection("Users").document(userID).set(userMap)
                            }
                            binding.verify.visibility = android.view.View.VISIBLE
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Verification email sent. Please verify your email.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        val verificationCheckThread = Thread {
                                            while (true) {
                                                user.reload()
                                                if (user.isEmailVerified) {
                                                    runOnUiThread {
                                                        Toast.makeText(
                                                            this,
                                                            "Email verified successfully!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        val intent =
                                                            Intent(this, MainActivity::class.java)
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
                                    }
                                }
                        } else {
                            try {
                                throw task.exception ?: Exception("Weak Password!")
                            } catch (e: FirebaseAuthWeakPasswordException) {
                                Toast.makeText(this, "Weak Password!", Toast.LENGTH_SHORT).show()
                            } catch (e: FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show()
                            } catch (e: FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "Email already in use!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this, "Weak Password!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
        binding.google.setOnClickListener {
            SfxManager.playSfx(this, R.raw.button_sound)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

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
            } catch (_: ApiException) {}
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