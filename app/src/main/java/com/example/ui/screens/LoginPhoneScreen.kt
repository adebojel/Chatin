package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

@Composable
fun LoginPhoneScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isCodeSent by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        if (!isCodeSent) {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP +62...") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(context as Activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            Firebase.auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                if (task.isSuccessful) onLoginSuccess()
                            }
                        }
                        override fun onVerificationFailed(e: FirebaseException) {}
                        override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            verificationId = verId
                            isCodeSent = true
                        }
                    })
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }, modifier = Modifier.fillMaxWidth()) { Text("Kirim OTP") }
        } else {
            OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Kode OTP") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                Firebase.auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) onLoginSuccess()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verifikasi") }
        }
    }
}
