package com.example.uniflow

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.uniflow.ui.theme.UniFlowTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniFlowTheme {
                RegisterScreen { email, password ->
                    val auth = FirebaseAuth.getInstance()

                    // spremanje podataka u firestore uz Firebase Auth
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    val userMap = hashMapOf(
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )
                    userId?.let {
                        FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(it)
                            .set(userMap)
                    }


                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Registracija uspješna!", Toast.LENGTH_SHORT).show()
                                    finish() // zatvori aktivnost i vrati se na login
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Greška: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Unesite email i lozinku", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}



@Composable
fun RegisterScreen(onRegister: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Registracija", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Lozinka") }, visualTransformation = PasswordVisualTransformation())

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onRegister(email, password) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Registriraj se")
            }
        }
    }
}
