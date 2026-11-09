package com.hitunguang.feature.category.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.hitunguang.R

object CategoryIconHelper {
    fun getIconByName(name: String?): ImageVector {
        return when (name?.lowercase()) {
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "shopping_cart" -> Icons.Default.ShoppingCart
            "movie" -> Icons.Default.Movie
            "receipt" -> Icons.Default.Receipt
            "category" -> Icons.Default.Category
            "payments" -> Icons.Default.Payments
            "trending_up" -> Icons.Default.TrendingUp
            "redeem" -> Icons.Default.Redeem
            "home" -> Icons.Default.Home
            "school" -> Icons.Default.School
            "star" -> Icons.Default.Star
            "work" -> Icons.Default.Work
            else -> Icons.Default.Category
        }
    }

    val availableIcons = listOf(
        "restaurant" to R.string.category_icon_restaurant,
        "directions_car" to R.string.category_icon_directions_car,
        "shopping_cart" to R.string.category_icon_shopping_cart,
        "movie" to R.string.category_icon_movie,
        "receipt" to R.string.category_icon_receipt,
        "category" to R.string.category_icon_category,
        "payments" to R.string.category_icon_payments,
        "trending_up" to R.string.category_icon_trending_up,
        "redeem" to R.string.category_icon_redeem,
        "home" to R.string.category_icon_home,
        "school" to R.string.category_icon_school,
        "star" to R.string.category_icon_star,
        "work" to R.string.category_icon_work
    )
}
