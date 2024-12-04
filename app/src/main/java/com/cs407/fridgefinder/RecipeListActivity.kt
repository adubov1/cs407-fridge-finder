package com.cs407.fridgefinder

import Recipe
import RecipeAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class RecipeListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private lateinit var seeAddIngredientsButton: Button
    private lateinit var allFiltersButton: Button
    private lateinit var menuButton: ImageButton
    private lateinit var backButton: ImageButton
    private val client = OkHttpClient()
    private var currentIngredients: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recipeList)
        seeAddIngredientsButton = findViewById(R.id.seeAddIngredientsButton)
        allFiltersButton = findViewById(R.id.allFiltersButton)
        menuButton = findViewById(R.id.menuButton)
        backButton = findViewById(R.id.backButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList())
        recyclerView.adapter = adapter

        currentIngredients = intent.getStringArrayListExtra("ingredients") ?: ArrayList()

        if (currentIngredients.isNotEmpty()) {
            fetchRecipes(currentIngredients)
        } else {
            Toast.makeText(this, "No ingredients provided", Toast.LENGTH_SHORT).show()
        }

        seeAddIngredientsButton.setOnClickListener {
            navigateToIngredients()
        }

        allFiltersButton.setOnClickListener {
            Toast.makeText(this, "Filters coming soon", Toast.LENGTH_SHORT).show()
        }

        menuButton.setOnClickListener {
            Toast.makeText(this, "Menu coming soon", Toast.LENGTH_SHORT).show()
        }

        backButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun fetchRecipes(ingredients: List<String>) {
        val apiKey = "37ae16adc4fa442fb2663292d5b710d0"
        val ingredientQuery = ingredients.joinToString(",")
        val url = "https://api.spoonacular.com/recipes/findByIngredients?ingredients=$ingredientQuery&number=25&ranking=1&ignorePantry=true&apiKey=$apiKey"

        Thread {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val recipes = parseRecipes(responseBody)
                    runOnUiThread {
                        if (recipes.isNotEmpty()) {
                            adapter.updateRecipes(recipes)
                        } else {
                            Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to fetch recipes: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun parseRecipes(responseBody: String?): List<Recipe> {
        if (responseBody.isNullOrEmpty()) return emptyList()

        val recipes = mutableListOf<Recipe>()
        try {
            val jsonArray = JSONArray(responseBody)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val image = jsonObject.getString("image")
                val id = jsonObject.getInt("id")
                val missedIngredients = jsonObject.getJSONArray("missedIngredients").let { missedArray ->
                    (0 until missedArray.length()).map { index ->
                        missedArray.getJSONObject(index).getString("name")
                    }
                }
                val usedIngredients = jsonObject.getJSONArray("usedIngredients").let { usedArray ->
                    (0 until usedArray.length()).map { index ->
                        usedArray.getJSONObject(index).getString("name")
                    }
                }
                recipes.add(Recipe(id, title, image, missedIngredients, usedIngredients))
            }

            recipes.sortBy { it.missedIngredients.size }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return recipes
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun navigateToIngredients() {
        val intent = Intent(this, IdentifyIngredientsActivity::class.java).apply {
            putStringArrayListExtra("ingredients", currentIngredients)
            putExtra("fromRecipeList", true)
        }

        startActivity(intent)
        finish()
    }
}

