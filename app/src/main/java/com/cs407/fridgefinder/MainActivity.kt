package com.cs407.fridgefinder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigate to saved recipes when its button is clicked
        val savedRecipesButton = findViewById<Button>(R.id.savedRecipesButton)
        savedRecipesButton.setOnClickListener {
            val intent = Intent(this, SavedRecipesActivity::class.java)
            startActivity(intent)
        }

        // Navigate to scan camera activity when its button is clicked
        val scanCameraButton = findViewById<ImageButton>(R.id.cameraButton)
        scanCameraButton.setOnClickListener {
            val intent = Intent(this, ScanCameraActivity::class.java)
            startActivity(intent)
        }
    }
}