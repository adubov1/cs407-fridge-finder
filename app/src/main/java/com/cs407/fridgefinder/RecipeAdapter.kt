import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cs407.fridgefinder.R

class RecipeAdapter(private var recipes: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recipe_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.title.text = recipe.title
        Glide.with(holder.image.context).load(recipe.image).into(holder.image)
        holder.ingredients.text = "Available Ingredients: ${recipe.usedIngredients.joinToString()}\nMissing Ingredients: ${recipe.missedIngredients.joinToString()}"
    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.recipeTitle)
        val image: ImageView = view.findViewById(R.id.recipeImage)
        val ingredients: TextView = view.findViewById(R.id.recipeIngredients)
    }
}
