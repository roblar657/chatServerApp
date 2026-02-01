package com.example.oving6server

import ServerCompose
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.example.oving6server.ui.theme.Oving6ServerTheme


class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.BLACK),
			navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.BLACK)
		)
		setContent {
			Oving6ServerTheme {
				ServerCompose()
			}
		}
	}
}
