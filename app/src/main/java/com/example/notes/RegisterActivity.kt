package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            RegisterScreen(
                onRegister = { email, password ->
                    createAccount(email, password)
                },
                onNavigateToLogin = {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            )
        }
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToNotesActivity()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun navigateToNotesActivity() {
        val intent = Intent(this, AllNotesActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "",
            modifier = Modifier.clip(
                CircleShape
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = email,
            onValueChange = {
                // Убираем символы новой строки из вводимого текста
                if (!it.contains('\n')) {
                    email = it
                }
            },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            passwordFocusRequester.requestFocus()
                        }
                        true
                    } else {
                        false
                    }
                },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF246156),
                unfocusedIndicatorColor = Color.Gray,
                cursorColor = Color(0xFF246156),
                textColor = Color.Black,
                focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = {
                // Убираем символы новой строки из вводимого текста
                if (!it.contains('\n')) {
                    password = it
                }
            },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            keyboardController?.hide()
                        }
                        true
                    } else {
                        false
                    }
                },
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF246156),
                unfocusedIndicatorColor = Color.Gray,
                cursorColor = Color(0xFF246156),
                textColor = Color.Black,
                focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { onRegister(email, password) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF246156), contentColor = Color.White),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .width(240.dp) // Уменьшена ширина кнопки
                .height(56.dp) // Высота кнопки
        ) {
            Text("Register", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = Color.Transparent, // Убираем фиолетовый фон
                contentColor = Color(0xFF246156)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Login", color = Color(0xFF246156), fontSize = 16.sp)
        }
    }
}
