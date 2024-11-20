data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val missedIngredients: List<String>,
    val usedIngredients: List<String>
)
