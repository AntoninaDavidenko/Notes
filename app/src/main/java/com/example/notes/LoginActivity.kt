package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.ui.text.input.PasswordVisualTransformation

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LoginScreen(
                onLogin = { email, password ->
                    signInWithEmail(email, password)
                },
                onGoogleLogin = {
                    signInWithGoogle()
                },
                onNavigateToRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                }
            )
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToNotesActivity()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    Toast.makeText(this, "Google login successful!", Toast.LENGTH_SHORT).show()
                    navigateToNotesActivity()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToNotesActivity() {
        val intent = Intent(this, AllNotesActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onGoogleLogin: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF246156),
                unfocusedIndicatorColor = Color.Gray,
                cursorColor = Color(0xFF246156),
                textColor = Color.Black,
                focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF246156),
                unfocusedIndicatorColor = Color.Gray,
                cursorColor = Color(0xFF246156),
                textColor = Color.Black,
                focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onLogin(email, password) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF246156), contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onGoogleLogin,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF246156), contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()) {
            Text("Login with Google")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onNavigateToRegister,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = Color.Transparent, // Убираем фиолетовый фон
                contentColor = Color(0xFF246156)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Register")
        }
    }
}
