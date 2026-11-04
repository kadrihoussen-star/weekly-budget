package com.example.weeklybudget.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

private val AppIcons: Map<String, ImageVector> = linkedMapOf(
    // فئات المصاريف
    "bus" to Icons.Filled.DirectionsBus,
    "food" to Icons.Filled.Restaurant,
    "person" to Icons.Filled.Person,
    "cart" to Icons.Filled.ShoppingCart,
    "coffee" to Icons.Filled.Coffee,
    "clothes" to Icons.Filled.Checkroom,
    "fuel" to Icons.Filled.LocalGasStation,
    "health" to Icons.Filled.LocalHospital,
    "school" to Icons.Filled.School,
    "fun" to Icons.Filled.SportsEsports,
    "pets" to Icons.Filled.Pets,
    "kids" to Icons.Filled.ChildCare,
    "gift" to Icons.Filled.CardGiftcard,
    "other" to Icons.Filled.Category,
    // فواتير
    "electric" to Icons.Filled.Bolt,
    "water" to Icons.Filled.WaterDrop,
    "wifi" to Icons.Filled.Wifi,
    "phone" to Icons.Filled.PhoneAndroid,
    "home" to Icons.Filled.Home,
    "receipt" to Icons.Filled.ReceiptLong,
    "card" to Icons.Filled.CreditCard
)

fun iconFor(key: String): ImageVector = AppIcons[key] ?: Icons.Filled.Category

val CategoryIconKeys = listOf(
    "bus", "food", "person", "cart", "coffee", "clothes", "fuel",
    "health", "school", "fun", "pets", "kids", "gift", "other"
)

val BillIconKeys = listOf("electric", "water", "wifi", "phone", "home", "receipt", "card", "other")
