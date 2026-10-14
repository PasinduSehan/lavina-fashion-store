package com.example.data.firebase

import android.util.Log
import com.example.data.firebase.FirebaseInitializer.awaitTask
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Service layer for Cloud Firestore in the Lumina application.
 * Manages full CRUD operations for Products, User Profiles, Cart Data, and Reviews,
 * ensuring all operations use the initialized Firestore SDK.
 */
class FirestoreService(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseInitializer.getFirestore() }
) {

    private val firestore: FirebaseFirestore
        get() = firestoreProvider()

    companion object {
        private const val TAG = "FirestoreService"

        const val COLLECTION_PRODUCTS = "products"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_CARTS = "carts"
        const val COLLECTION_REVIEWS = "reviews"

        @Volatile
        private var instance: FirestoreService? = null

        fun getInstance(): FirestoreService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreService().also { instance = it }
            }
        }
    }

    // =========================================================================
    // 1. PRODUCTS CRUD OPERATIONS
    // =========================================================================

    /**
     * CREATE / INSERT a new product or overwrite by ID.
     */
    suspend fun createProduct(product: Product): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (product.id.isNotBlank()) {
                firestore.collection(COLLECTION_PRODUCTS).document(product.id)
            } else {
                firestore.collection(COLLECTION_PRODUCTS).document()
            }
            val productId = docRef.id
            val productToSave = if (product.id.isBlank()) product.copy(id = productId) else product

            docRef.set(productToMap(productToSave)).awaitTask()
            Log.d(TAG, "Product created successfully with ID: $productId")
            Result.success(productId)
        } catch (e: Exception) {
            Log.w(TAG, "Error creating product: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * BATCH CREATE multiple products (useful for initial catalog seed/sync).
     */
    suspend fun batchCreateProducts(products: List<Product>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            for (product in products) {
                val docRef = firestore.collection(COLLECTION_PRODUCTS).document(product.id)
                batch.set(docRef, productToMap(product), SetOptions.merge())
            }
            batch.commit().awaitTask()
            Log.d(TAG, "Batch inserted ${products.size} products into Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error in batchCreateProducts: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ single product by ID.
     */
    suspend fun getProduct(productId: String): Result<Product?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .awaitTask()

            if (snapshot.exists()) {
                Result.success(mapToProduct(snapshot))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching product $productId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ all active products.
     */
    suspend fun getAllProducts(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = firestore.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("isActive", true)
                .get()
                .awaitTask()

            val products = querySnapshot.documents.mapNotNull { mapToProduct(it) }
            Result.success(products)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching all products: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ products by category.
     */
    suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = firestore.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("isActive", true)
                .get()
                .awaitTask()

            val products = querySnapshot.documents.mapNotNull { mapToProduct(it) }
            Result.success(products)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching products for category $categoryId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * OBSERVE products real-time via snapshot listener Flow.
     */
    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_PRODUCTS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error in observeProducts snapshot listener: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { mapToProduct(it) }
                    trySend(products)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * OBSERVE a single product real-time by ID.
     */
    fun observeProduct(productId: String): Flow<Product?> = callbackFlow {
        val registration = firestore.collection(COLLECTION_PRODUCTS)
            .document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error in observeProduct listener: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(mapToProduct(snapshot))
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * UPDATE an existing product.
     */
    suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(product.id)
                .set(productToMap(product), SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Product ${product.id} updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating product ${product.id}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * UPDATE product inventory stock.
     */
    suspend fun updateProductStock(productId: String, newStock: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .update("stock", newStock)
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating stock for product $productId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * UPDATE product rating and review count.
     */
    suspend fun updateProductRating(productId: String, rating: Double, reviewCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .update(
                    mapOf(
                        "rating" to rating,
                        "reviewCount" to reviewCount
                    )
                )
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating rating for product $productId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * DELETE product by ID.
     */
    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .delete()
                .awaitTask()
            Log.d(TAG, "Product $productId deleted successfully from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting product $productId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. USER PROFILES CRUD OPERATIONS
    // =========================================================================

    /**
     * CREATE / SAVE User Profile in Firestore.
     */
    suspend fun createUserProfile(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_USERS)
                .document(user.uid)
                .set(userToMap(user), SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "User profile created/saved in Firestore: ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error creating user profile ${user.uid}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ User Profile by UID.
     */
    suspend fun getUserProfile(userId: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .awaitTask()

            if (doc.exists()) {
                Result.success(mapToUser(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting user profile $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ User Profile by Email.
     */
    suspend fun getUserByEmail(email: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .awaitTask()

            val doc = snapshot.documents.firstOrNull()
            if (doc != null && doc.exists()) {
                Result.success(mapToUser(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user by email $email: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * OBSERVE User Profile real-time via Flow.
     */
    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        val registration = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing user profile: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(mapToUser(snapshot))
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * UPDATE User Profile.
     */
    suspend fun updateUserProfile(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_USERS)
                .document(user.uid)
                .update(
                    mapOf(
                        "fullName" to user.fullName,
                        "phone" to user.phone,
                        "photoUrl" to user.photoUrl
                    )
                )
                .awaitTask()
            Log.d(TAG, "User profile updated: ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating user profile ${user.uid}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * UPDATE specific fields in User Profile.
     */
    suspend fun updateUserProfileFields(userId: String, updates: Map<String, Any?>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val filtered = updates.filterValues { it != null }
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(filtered)
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating user fields for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * DELETE User Profile by UID.
     */
    suspend fun deleteUserProfile(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .delete()
                .awaitTask()
            Log.d(TAG, "User profile deleted: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting user profile $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. CART DATA CRUD OPERATIONS
    // =========================================================================

    /**
     * CREATE / ADD item to cart. If identical item exists, increments quantity.
     * Returns the cart item document ID.
     */
    suspend fun addToCart(cartItem: CartItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Find if item already exists for user with same product, size, and colour
            val existingQuery = firestore.collection(COLLECTION_CARTS)
                .whereEqualTo("userId", cartItem.userId)
                .whereEqualTo("productId", cartItem.productId)
                .whereEqualTo("selectedSize", cartItem.selectedSize)
                .whereEqualTo("selectedColour", cartItem.selectedColour)
                .limit(1)
                .get()
                .awaitTask()

            val existingDoc = existingQuery.documents.firstOrNull()
            if (existingDoc != null && existingDoc.exists()) {
                val currentQty = existingDoc.getLong("quantity")?.toInt() ?: 1
                val newQty = currentQty + cartItem.quantity
                existingDoc.reference.update(
                    mapOf(
                        "quantity" to newQty,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).awaitTask()
                Log.d(TAG, "Updated existing cart item quantity to $newQty (Doc ID: ${existingDoc.id})")
                Result.success(existingDoc.id)
            } else {
                // Generate consistent or auto-generated document ID
                val docRef = firestore.collection(COLLECTION_CARTS).document()
                val cartMap = cartItemToMap(cartItem).toMutableMap().apply {
                    put("id", docRef.id)
                    put("updatedAt", System.currentTimeMillis())
                }
                docRef.set(cartMap).awaitTask()
                Log.d(TAG, "Created new cart item (Doc ID: ${docRef.id})")
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error adding item to cart in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ all cart items for a specific user.
     */
    suspend fun getCartItems(userId: String): Result<List<CartItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_CARTS)
                .whereEqualTo("userId", userId)
                .get()
                .awaitTask()

            val items = snapshot.documents.mapNotNull { mapToCartItem(it) }
            Result.success(items)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching cart items for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ single cart item by Firestore document ID.
     */
    suspend fun getCartItem(cartDocId: String): Result<CartItem?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COLLECTION_CARTS)
                .document(cartDocId)
                .get()
                .awaitTask()

            if (doc.exists()) {
                Result.success(mapToCartItem(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting cart item $cartDocId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * OBSERVE cart items real-time for a specific user via Flow.
     */
    fun observeCartItems(userId: String): Flow<List<CartItem>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_CARTS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing cart items: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { mapToCartItem(it) }
                    trySend(items)
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * UPDATE complete cart item document by Firestore Document ID.
     */
    suspend fun updateCartItem(cartDocId: String, cartItem: CartItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val map = cartItemToMap(cartItem).toMutableMap().apply {
                put("id", cartDocId)
                put("updatedAt", System.currentTimeMillis())
            }
            firestore.collection(COLLECTION_CARTS)
                .document(cartDocId)
                .set(map, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating cart item $cartDocId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * UPDATE cart item quantity by Firestore Document ID.
     * Deletes item if quantity <= 0.
     */
    suspend fun updateCartItemQuantity(cartDocId: String, newQuantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newQuantity <= 0) {
                deleteCartItem(cartDocId)
            } else {
                firestore.collection(COLLECTION_CARTS)
                    .document(cartDocId)
                    .update(
                        mapOf(
                            "quantity" to newQuantity,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .awaitTask()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating cart quantity for $cartDocId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * DELETE a specific cart item document.
     */
    suspend fun deleteCartItem(cartDocId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_CARTS)
                .document(cartDocId)
                .delete()
                .awaitTask()
            Log.d(TAG, "Cart item $cartDocId deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting cart item $cartDocId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * CLEAR / DELETE all cart items for a user.
     */
    suspend fun clearCart(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_CARTS)
                .whereEqualTo("userId", userId)
                .get()
                .awaitTask()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitTask()
            }
            Log.d(TAG, "Cart cleared successfully for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing cart for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. REVIEWS CRUD OPERATIONS (Linked to Products & Users)
    // =========================================================================

    /**
     * CREATE a review in Firestore.
     */
    suspend fun submitReview(review: Review): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(COLLECTION_REVIEWS).document()
            val map = reviewToMap(review).toMutableMap().apply {
                put("id", docRef.id)
            }
            docRef.set(map).awaitTask()
            Log.d(TAG, "Review submitted to Firestore with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.w(TAG, "Error submitting review to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * READ reviews for a specific product.
     */
    suspend fun getReviewsForProduct(productId: String): Result<List<Review>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .awaitTask()

            val reviews = snapshot.documents.mapNotNull { mapToReview(it) }
            Result.success(reviews)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching reviews for product $productId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * OBSERVE reviews for a specific product real-time.
     */
    fun observeReviewsForProduct(productId: String): Flow<List<Review>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("productId", productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing reviews for $productId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reviews = snapshot.documents.mapNotNull { mapToReview(it) }
                    trySend(reviews)
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    // =========================================================================
    // HELPER MAPPERS (Serialization & Deserialization)
    // =========================================================================

    private fun productToMap(p: Product): Map<String, Any?> = mapOf(
        "id" to p.id,
        "name" to p.name,
        "description" to p.description,
        "categoryId" to p.categoryId,
        "priceLkr" to p.priceLkr,
        "discountPriceLkr" to p.discountPriceLkr,
        "discountPercent" to p.discountPercent,
        "sizesCsv" to p.sizesCsv,
        "coloursCsv" to p.coloursCsv,
        "imageUrlsCsv" to p.imageUrlsCsv,
        "stock" to p.stock,
        "brand" to p.brand,
        "rating" to p.rating,
        "reviewCount" to p.reviewCount,
        "isFeatured" to p.isFeatured,
        "isNewArrival" to p.isNewArrival,
        "isBestSeller" to p.isBestSeller,
        "isDiscounted" to p.isDiscounted,
        "isActive" to p.isActive
    )

    private fun mapToProduct(doc: DocumentSnapshot): Product? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return null
        return Product(
            id = id,
            name = name,
            description = doc.getString("description") ?: "",
            categoryId = doc.getString("categoryId") ?: "women",
            priceLkr = doc.getDouble("priceLkr") ?: 0.0,
            discountPriceLkr = doc.getDouble("discountPriceLkr"),
            discountPercent = doc.getLong("discountPercent")?.toInt() ?: 0,
            sizesCsv = doc.getString("sizesCsv") ?: "S,M,L,XL",
            coloursCsv = doc.getString("coloursCsv") ?: "Black,White",
            imageUrlsCsv = doc.getString("imageUrlsCsv") ?: "",
            stock = doc.getLong("stock")?.toInt() ?: 10,
            brand = doc.getString("brand") ?: "Lavina Atelier",
            rating = doc.getDouble("rating") ?: 4.8,
            reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
            isFeatured = doc.getBoolean("isFeatured") ?: false,
            isNewArrival = doc.getBoolean("isNewArrival") ?: false,
            isBestSeller = doc.getBoolean("isBestSeller") ?: false,
            isDiscounted = doc.getBoolean("isDiscounted") ?: false,
            isActive = doc.getBoolean("isActive") ?: true
        )
    }

    private fun userToMap(u: User): Map<String, Any?> = mapOf(
        "uid" to u.uid,
        "customerId" to u.customerId,
        "fullName" to u.fullName,
        "email" to u.email.trim().lowercase(),
        "phone" to u.phone,
        "role" to u.role,
        "photoUrl" to u.photoUrl,
        "createdAt" to u.createdAt,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun mapToUser(doc: DocumentSnapshot): User? {
        val uid = doc.getString("uid") ?: doc.id
        val email = doc.getString("email") ?: return null
        return User(
            uid = uid,
            customerId = doc.getString("customerId") ?: "CUS-1001",
            fullName = doc.getString("fullName") ?: "",
            email = email,
            phone = doc.getString("phone") ?: "",
            role = doc.getString("role") ?: "CUSTOMER",
            photoUrl = doc.getString("photoUrl") ?: "",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun cartItemToMap(c: CartItem): Map<String, Any?> = mapOf(
        "userId" to c.userId,
        "productId" to c.productId,
        "productName" to c.productName,
        "productImage" to c.productImage,
        "priceLkr" to c.priceLkr,
        "selectedSize" to c.selectedSize,
        "selectedColour" to c.selectedColour,
        "quantity" to c.quantity
    )

    private fun mapToCartItem(doc: DocumentSnapshot): CartItem? {
        val userId = doc.getString("userId") ?: return null
        val productId = doc.getString("productId") ?: return null
        val docId = doc.id
        // Generate stable Long hash from docId for Room compatibility if needed
        val longId = doc.getLong("id") ?: (docId.hashCode().toLong() and 0x7FFFFFFF)

        return CartItem(
            id = longId,
            userId = userId,
            productId = productId,
            productName = doc.getString("productName") ?: "Luxury Item",
            productImage = doc.getString("productImage") ?: "",
            priceLkr = doc.getDouble("priceLkr") ?: 0.0,
            selectedSize = doc.getString("selectedSize") ?: "M",
            selectedColour = doc.getString("selectedColour") ?: "Gold",
            quantity = doc.getLong("quantity")?.toInt() ?: 1
        )
    }

    private fun reviewToMap(r: Review): Map<String, Any?> = mapOf(
        "productId" to r.productId,
        "userId" to r.userId,
        "customerName" to r.customerName,
        "rating" to r.rating,
        "comment" to r.comment,
        "dateString" to r.dateString,
        "isVerifiedPurchase" to r.isVerifiedPurchase,
        "createdAt" to r.createdAt
    )

    private fun mapToReview(doc: DocumentSnapshot): Review? {
        val productId = doc.getString("productId") ?: return null
        val longId = doc.getLong("id") ?: (doc.id.hashCode().toLong() and 0x7FFFFFFF)
        return Review(
            id = longId,
            productId = productId,
            userId = doc.getString("userId") ?: "",
            customerName = doc.getString("customerName") ?: "Anonymous",
            rating = doc.getLong("rating")?.toInt() ?: 5,
            comment = doc.getString("comment") ?: "",
            dateString = doc.getString("dateString") ?: "",
            isVerifiedPurchase = doc.getBoolean("isVerifiedPurchase") ?: true,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }
}
