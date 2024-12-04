package com.cs407.fridgefinder

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RecipeDescriptionActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var isSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_description)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recipeId = intent.getIntExtra("recipeId", -1)
        val title = intent.getStringExtra("title") ?: "Recipe"
        val imageUrl = intent.getStringExtra("image")
        val usedIngredients = intent.getStringArrayListExtra("usedIngredients") ?: ArrayList()
        val missedIngredients = intent.getStringArrayListExtra("missedIngredients") ?: ArrayList()

        findViewById<TextView>(R.id.titleTextView).text = title
        findViewById<TextView>(R.id.recipeTitle).text = title

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .into(findViewById(R.id.recipeImage))
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val saveButton = findViewById<ImageButton>(R.id.saveButton)
        saveButton.setOnClickListener {
            isSaved = !isSaved
            saveButton.setImageResource(
                if (isSaved) R.drawable.bookmark_filled_icon
                else R.drawable.bookmark_icon
            )
            Toast.makeText(this,
                if (isSaved) "Recipe saved" else "Recipe removed from saved",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<TextView>(R.id.Text1).text = "Ingredients:"
        val ingredientsText = StringBuilder().apply {
            append("Available: ")
            append(usedIngredients.joinToString())
            append("\n\nNeeded: ")
            append(missedIngredients.joinToString())
        }
        findViewById<TextView>(R.id.supportingText).text = ingredientsText

        if (recipeId != -1) {
            fetchRecipeDetails(recipeId)
        }
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        Thread {
            try {
                val apiKey = "37ae16adc4fa442fb2663292d5b710d0"
                val request = Request.Builder()
                    .url("https://api.spoonacular.com/recipes/$recipeId/information?apiKey=$apiKey")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val recipeInfo = JSONObject(responseBody)

                    runOnUiThread {
                        findViewById<TextView>(R.id.description).text = recipeInfo.getString("instructions")
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to fetch recipe details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
