package com.cs407.fridgefinder

import android.os.Bundle
import android.text.Html
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
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RecipeDescriptionActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var isSaved = false
    private val firestore = FirebaseFirestore.getInstance()

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
        if (recipeId != -1) {
            checkIsSaved(recipeId) { saved ->
                isSaved = saved
                saveButton.setImageResource(
                    if (isSaved) R.drawable.bookmark_filled_icon
                    else R.drawable.bookmark_icon
                )
            }
        }

        saveButton.setOnClickListener {
            isSaved = !isSaved
            saveButton.setImageResource(
                if (isSaved) R.drawable.bookmark_filled_icon
                else R.drawable.bookmark_icon
            )

            if (isSaved) {
                saveRecipeToFirestore(recipeId, title, imageUrl, usedIngredients, missedIngredients)
            } else {
                removeRecipeFromFirestore(recipeId)
            }
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

    private fun fetchRecipeDetails(recipeId: Int, retries: Int = 3) {
        Thread {
            var attempts = 0
            while (attempts < retries) {
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
                            val instructionsHtml = recipeInfo.getString("instructions")
                            findViewById<TextView>(R.id.description).text = Html.fromHtml(instructionsHtml, Html.FROM_HTML_MODE_COMPACT)

                        }
                        return@Thread
                    } else {
                        attempts++
                        if (attempts == retries) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Failed to fetch recipe details: ${response.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    attempts++
                    if (attempts == retries) {
                        runOnUiThread {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun saveRecipeToFirestore(
        id: Int,
        title: String,
        image: String?,
        usedIngredients: List<String>,
        missedIngredients: List<String>
    ) {
        val recipe = hashMapOf(
            "id" to id,
            "title" to title,
            "image" to image,
            "usedIngredients" to usedIngredients,
            "missedIngredients" to missedIngredients
        )
        firestore.collection("recipe")
            .document(id.toString())
            .set(recipe)
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Toast.makeText(this, "Failed to save recipe", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeRecipeFromFirestore(id: Int) {
        firestore.collection("recipe")
            .document(id.toString())
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe removed from saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to remove recipe", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIsSaved(id: Int, callback: (Boolean) -> Unit) {
        firestore.collection("recipe")
            .document(id.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                callback(false)
            }
    }
}
