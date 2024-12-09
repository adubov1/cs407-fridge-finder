data class Recipe(
    val id: Int = 0,
    val title: String = "",
    val image: String = "",
    val missedIngredients: List<String> = emptyList(),
    val usedIngredients: List<String> = emptyList()
)
