package com.bookverse.notificationinbox

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookverse.notificationinbox.ui.theme.NotificationInboxTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private var fcmToken by mutableStateOf("Fetching token…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("FCM", "MainActivity started")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            fcmToken = if (task.isSuccessful) {
                task.result
            } else {
                "Token error: ${task.exception?.message}"
            }
            Log.e("FCM", "Token result: $fcmToken")
        }

        enableEdgeToEdge()
        setContent {
            NotificationInboxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Text("FCM token (debug only):")
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer { Text(fcmToken) }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { shareToken() }) {
                            Text("Share token")
                        }
                    }
                }
            }
        }
    }

    private fun shareToken() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fcmToken)
        }
        startActivity(Intent.createChooser(intent, "Share FCM token"))
    }
}