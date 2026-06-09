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
        "restaurant" to "Makanan",
        "directions_car" to "Transportasi",
        "shopping_cart" to "Belanja",
        "movie" to "Hiburan",
        "receipt" to "Tagihan",
        "category" to "Lain-lain",
        "payments" to "Gaji",
        "trending_up" to "Investasi",
        "redeem" to "Bonus",
        "home" to "Rumah/Kos",
        "school" to "Pendidikan",
        "star" to "Favorit",
        "work" to "Pekerjaan"
    )
}
