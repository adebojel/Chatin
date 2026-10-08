package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun LoginPhoneScreen(onLoginSuccess: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Login Chatin", style = MaterialTheme.typography.headlineMedium)
        Text("Kayak WhatsApp - SMS beneran", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        
        if (!isOtpSent) {
            OutlinedTextField(
                value = phone, 
                onValueChange = { phone = it }, 
                label = { Text("No HP +62822...") }, 
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+62 822...") }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(context as FragmentActivity)
                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                verificationId = id
                                isOtpSent = true
                            }
                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                            }
                        })
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kirim SMS OTP Beneran")
            }
        } else {
            Text("Kode SMS udah dikirim ke $phone")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = otp, 
                onValueChange = { otp = it }, 
                label = { Text("Kode SMS dari HP") }, 
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { onLoginSuccess() }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verifikasi")
            }
        }
    }
}
