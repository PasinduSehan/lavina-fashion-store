package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val customerId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String = "CUSTOMER", // CUSTOMER or ADMIN
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val priceLkr: Double,
    val discountPriceLkr: Double? = null,
    val discountPercent: Int = 0,
    val sizesCsv: String = "S,M,L,XL", // comma separated
    val coloursCsv: String = "Black,White,Beige", // comma separated
    val imageUrlsCsv: String = "", // comma separated
    val stock: Int = 25,
    val brand: String = "Lavina Atelier",
    val rating: Double = 4.8,
    val reviewCount: Int = 32,
    val isFeatured: Boolean = false,
    val isNewArrival: Boolean = false,
    val isBestSeller: Boolean = false,
    val isDiscounted: Boolean = false,
    val isActive: Boolean = true
) {
    val sizes: List<String> get() = sizesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val colours: List<String> get() = coloursCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val imageUrls: List<String> get() = imageUrlsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val primaryImageUrl: String get() = imageUrls.firstOrNull() ?: ""
    val effectivePrice: Double get() = discountPriceLkr ?: priceLkr
}

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val imageUrl: String,
    val itemCount: Int = 0
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val productId: String,
    val productName: String,
    val productImage: String,
    val priceLkr: Double,
    val selectedSize: String,
    val selectedColour: String,
    val quantity: Int
)

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey val id: String, // composite "${userId}_${productId}"
    val userId: String,
    val productId: String,
    val productName: String,
    val productImage: String,
    val priceLkr: Double,
    val discountPriceLkr: Double? = null,
    val rating: Double = 4.8,
    val inStock: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderNumber: String,
    val userId: String,
    val customerName: String,
    val phone: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val deliveryInstructions: String = "",
    val deliveryMethod: String = "Standard Courier", // Standard or Express
    val paymentMethod: String = "Cash on Delivery", // Cash on Delivery or Online Card Payment
    val paymentStatus: String = "Pending", // Pending or Verified
    val orderStatus: String = "Pending", // Pending, Confirmed, Processing, Shipped, Out for Delivery, Delivered, Cancelled
    val subtotalLkr: Double,
    val deliveryFeeLkr: Double,
    val discountLkr: Double = 0.0,
    val totalLkr: Double,
    val itemsSummary: String, // formatted summary of items
    val productIdsCsv: String = "", // comma-separated product IDs
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val userId: String = "",
    val customerName: String,
    val rating: Int,
    val comment: String,
    val dateString: String,
    val isVerifiedPurchase: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // ORDER, PROMO, SYSTEM
    val dateFormatted: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "addresses")
data class SavedAddress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val label: String, // Home, Atelier, Office
    val recipientName: String,
    val phone: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean = false
)
