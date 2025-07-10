package com.example.uniflow

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniflow.ui.theme.UniFlowTheme
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniFlowTheme {
                LoginScreen { username ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("EXTRA_USERNAME", username)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "UniFlow",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF31E981)
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Korisničko ime") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("usernameField"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Lozinka") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passwordField"),
                shape = RoundedCornerShape(12.dp)
            )

            // gumb za login

            Button(
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        auth.signInWithEmailAndPassword(username, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Prijava uspjela
                                    onLogin(username)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Pogrešno korisničko ime ili lozinka.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Unesite podatke", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF31E981)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Prijava", fontWeight = FontWeight.Bold)
            }

            // gumb za registraciju
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Nemate račun? Registrirajte se")
            }

        }
    }
}
