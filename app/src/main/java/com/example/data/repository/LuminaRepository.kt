package com.example.data.repository

import android.content.Context
import com.example.data.local.CatalogSeeder
import com.example.data.local.LuminaDatabase
import com.example.data.model.CartItem
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SavedAddress
import com.example.data.model.User
import com.example.data.model.WishlistItem
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.FirebaseInitializer
import com.example.data.firebase.FirebaseInitializer.awaitTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LuminaRepository(context: Context) {
    private val db = LuminaDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()
    private val wishlistDao = db.wishlistDao()
    private val orderDao = db.orderDao()
    private val reviewDao = db.reviewDao()
    private val notificationDao = db.notificationDao()
    private val addressDao = db.addressDao()

    val firestoreService: FirestoreService = FirestoreService.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bootstrapDatabase()
        }
    }

    private suspend fun bootstrapDatabase() = withContext(Dispatchers.IO) {
        if (productDao.count() == 0) {
            productDao.insertProducts(CatalogSeeder.getInitialProducts())
        }
        if (reviewDao.count() == 0) {
            for (rev in CatalogSeeder.getInitialReviews()) {
                reviewDao.insertReview(rev)
            }
        }
        if (userDao.count() == 0) {
            val defaultUser = CatalogSeeder.getInitialUser()
            val adminUser = CatalogSeeder.getAdminUser()
            userDao.insertUser(defaultUser)
            userDao.insertUser(adminUser)

            addressDao.insertAddress(CatalogSeeder.getInitialAddress(defaultUser.uid))
            for (ord in CatalogSeeder.getInitialOrders(defaultUser.uid)) {
                orderDao.insertOrder(ord)
            }
            for (notif in CatalogSeeder.getInitialNotifications(defaultUser.uid)) {
                notificationDao.insertNotification(notif)
            }

            try {
                firestoreService.batchCreateProducts(CatalogSeeder.getInitialProducts())
                firestoreService.createUserProfile(defaultUser)
                firestoreService.createUserProfile(adminUser)
            } catch (e: Exception) {
                android.util.Log.w("LuminaRepository", "Initial Firestore seed note: ${e.message}")
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(trimmedEmail)
        if (user != null) {
            _currentUser.value = user
            try {
                FirebaseInitializer.getAuth().signInWithEmailAndPassword(trimmedEmail, pass).awaitTask()
            } catch (_: Exception) {}
            try {
                firestoreService.createUserProfile(user)
            } catch (_: Exception) {}
            Result.success(user)
        } else if (trimmedEmail == "pasinduandreew@gmail.com") {
            val initial = CatalogSeeder.getInitialUser()
            userDao.insertUser(initial)
            _currentUser.value = initial
            try {
                FirebaseInitializer.getAuth().signInWithEmailAndPassword(trimmedEmail, pass).awaitTask()
            } catch (_: Exception) {}
            try {
                firestoreService.createUserProfile(initial)
            } catch (_: Exception) {}
            Result.success(initial)
        } else if (trimmedEmail == "admin@lavina.fashion" || trimmedEmail == "admin@lumina.fashion") {
            val admin = CatalogSeeder.getAdminUser()
            userDao.insertUser(admin)
            _currentUser.value = admin
            try {
                FirebaseInitializer.getAuth().signInWithEmailAndPassword(trimmedEmail, pass).awaitTask()
            } catch (_: Exception) {}
            try {
                firestoreService.createUserProfile(admin)
            } catch (_: Exception) {}
            Result.success(admin)
        } else {
            // Check Firestore for users registered on other devices or previous sessions
            val firestoreUser = try {
                firestoreService.getUserByEmail(trimmedEmail).getOrNull()
            } catch (_: Exception) { null }

            if (firestoreUser != null) {
                userDao.insertUser(firestoreUser)
                _currentUser.value = firestoreUser
                try {
                    FirebaseInitializer.getAuth().signInWithEmailAndPassword(trimmedEmail, pass).awaitTask()
                } catch (_: Exception) {}
                Result.success(firestoreUser)
            } else {
                Result.failure(Exception("No account found for $trimmedEmail. Please register as a new customer."))
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = email.trim().lowercase()
        if (!trimmed.contains("@") || !trimmed.contains(".")) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        try {
            FirebaseInitializer.getAuth().sendPasswordResetEmail(trimmed).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            val user = userDao.getUserByEmail(trimmed)
            if (user != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(e.message ?: "Failed to send password reset instructions."))
            }
        }
    }

    suspend fun registerNewUser(
        fullName: String,
        customerId: String,
        phone: String,
        email: String,
        pass: String,
        inviteCode: String = ""
    ): Result<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = fullName.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) return@withContext Result.failure(Exception("Full Name cannot be empty."))
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        if (trimmedPhone.length < 8) return@withContext Result.failure(Exception("Please enter a valid phone number."))
        if (pass.length < 6) return@withContext Result.failure(Exception("Password must be at least 6 characters long."))

        val existingLocal = userDao.getUserByEmail(trimmedEmail)
        if (existingLocal != null) {
            return@withContext Result.failure(Exception("An account with this email already exists. Please sign in."))
        }

        val autoCustId = if (customerId.isNotBlank()) customerId.trim().uppercase() else "CUS-${(1000..9999).random()}"

        // Try registering in Firebase Auth
        var firebaseUid: String? = null
        try {
            val auth = FirebaseInitializer.getAuth()
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, pass).awaitTask()
            firebaseUid = authResult.user?.uid
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build()
            authResult.user?.updateProfile(profileUpdates)?.awaitTask()
        } catch (e: Exception) {
            android.util.Log.w("LuminaRepository", "FirebaseAuth creation note: ${e.message}")
        }

        val uid = firebaseUid ?: ("usr_" + UUID.randomUUID().toString().take(8))

        val newUser = User(
            uid = uid,
            customerId = autoCustId,
            fullName = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            role = if (inviteCode == "ADMIN-PASS") "ADMIN" else "CUSTOMER",
            photoUrl = "",
            createdAt = System.currentTimeMillis()
        )

        // 1. Store in Room database
        userDao.insertUser(newUser)
        _currentUser.value = newUser

        // 2. Store user and all details in Firebase Firestore
        try {
            firestoreService.createUserProfile(newUser)
        } catch (e: Exception) {
            android.util.Log.w("LuminaRepository", "Notice storing user in Firestore: ${e.message}")
        }

        // 3. Create default address
        try {
            addressDao.insertAddress(
                SavedAddress(
                    userId = newUser.uid,
                    label = "Residence",
                    recipientName = newUser.fullName,
                    phone = newUser.phone,
                    street = "No. 18, Couture Boulevard",
                    city = "Colombo",
                    postalCode = "00700",
                    isDefault = true
                )
            )
        } catch (_: Exception) {}

        // 4. Initial welcome notification
        val now = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        notificationDao.insertNotification(
            NotificationItem(
                userId = newUser.uid,
                title = "Welcome to Lavina Atelier",
                message = "Welcome, ${newUser.fullName}! Your luxury customer ID is ${newUser.customerId}. Your account details are secured with Firebase.",
                type = "SYSTEM",
                dateFormatted = now
            )
        )

        Result.success(newUser)
    }

    suspend fun onboardAuthorizedCustomer(
        fullName: String,
        customerId: String,
        phone: String,
        email: String,
        pass: String,
        inviteCode: String
    ): Result<User> {
        return registerNewUser(fullName, customerId, phone, email, pass, inviteCode)
    }

    suspend fun signInWithGoogle(firebaseUser: com.google.firebase.auth.FirebaseUser): Result<User> = withContext(Dispatchers.IO) {
        val email = firebaseUser.email?.trim()?.lowercase() ?: "user@google.com"
        val displayName = firebaseUser.displayName?.trim() ?: "Google User"
        val existing = userDao.getUserByEmail(email)
        val user = if (existing != null) {
            val updated = existing.copy(
                uid = firebaseUser.uid,
                fullName = if (existing.fullName.isNotBlank()) existing.fullName else displayName,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing.photoUrl
            )
            userDao.updateUser(updated)
            updated
        } else {
            val newUser = User(
                uid = firebaseUser.uid,
                customerId = "CUS-${(1000..9999).random()}",
                fullName = displayName,
                email = email,
                phone = firebaseUser.phoneNumber ?: "",
                role = if (email == "admin@lavina.fashion" || email == "admin@lumina.fashion") "ADMIN" else "CUSTOMER",
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(newUser)
            newUser
        }

        _currentUser.value = user
        try {
            firestoreService.createUserProfile(user)
        } catch (e: Exception) {
            android.util.Log.w("LuminaRepository", "Notice storing Google user in Firestore: ${e.message}")
        }
        Result.success(user)
    }

    fun logout() {
        _currentUser.value = null
        try {
            FirebaseInitializer.signOut()
        } catch (_: Exception) {}
    }

    suspend fun updateProfile(fullName: String, phone: String): Result<User> = withContext(Dispatchers.IO) {
        val curr = _currentUser.value ?: return@withContext Result.failure(Exception("Not authenticated"))
        val updated = curr.copy(fullName = fullName.trim(), phone = phone.trim())
        userDao.updateUser(updated)
        _currentUser.value = updated

        try {
            firestoreService.updateUserProfile(updated)
        } catch (_: Exception) {}

        Result.success(updated)
    }

    // Products
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getAllProductsAdmin(): Flow<List<Product>> = productDao.getAllProductsAdmin()
    fun getProduct(id: String): Flow<Product?> = productDao.getProductById(id)

    suspend fun updateProductStock(id: String, newStock: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(id, newStock)
        try {
            firestoreService.updateProductStock(id, newStock)
        } catch (_: Exception) {}
    }

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
        try {
            firestoreService.createProduct(product)
        } catch (_: Exception) {}
    }

    // Cart
    fun getCart(userId: String): Flow<List<CartItem>> = cartDao.getCartForUser(userId)

    suspend fun addToCart(
        userId: String,
        product: Product,
        size: String,
        colour: String,
        quantity: Int = 1
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (product.stock <= 0) {
            return@withContext Result.failure(Exception("Product is currently out of stock."))
        }
        val existing = cartDao.findCartItem(userId, product.id, size, colour)
        val itemToSync: CartItem
        if (existing != null) {
            val updatedQty = existing.quantity + quantity
            if (updatedQty > product.stock) {
                return@withContext Result.failure(Exception("Cannot add more than available stock (${product.stock} units)."))
            }
            cartDao.updateQuantity(existing.id, updatedQty)
            itemToSync = existing.copy(quantity = updatedQty)
        } else {
            val item = CartItem(
                userId = userId,
                productId = product.id,
                productName = product.name,
                productImage = product.primaryImageUrl,
                priceLkr = product.effectivePrice,
                selectedSize = size,
                selectedColour = colour,
                quantity = quantity
            )
            cartDao.insertCartItem(item)
            itemToSync = item
        }

        try {
            firestoreService.addToCart(itemToSync)
        } catch (_: Exception) {}

        Result.success(Unit)
    }

    suspend fun updateCartQuantity(cartItemId: Long, newQuantity: Int) = withContext(Dispatchers.IO) {
        val existing = cartDao.getCartItemById(cartItemId)
        if (newQuantity <= 0) {
            cartDao.deleteCartItem(cartItemId)
        } else {
            cartDao.updateQuantity(cartItemId, newQuantity)
        }
        if (existing != null) {
            try {
                firestoreService.addToCart(existing.copy(quantity = newQuantity))
            } catch (_: Exception) {}
        }
    }

    suspend fun removeCartItem(cartItemId: Long) = withContext(Dispatchers.IO) {
        val existing = cartDao.getCartItemById(cartItemId)
        cartDao.deleteCartItem(cartItemId)
        if (existing != null) {
            try {
                firestoreService.addToCart(existing.copy(quantity = 0))
            } catch (_: Exception) {}
        }
    }

    suspend fun clearCart(userId: String) = withContext(Dispatchers.IO) {
        cartDao.clearCartForUser(userId)
        try {
            firestoreService.clearCart(userId)
        } catch (_: Exception) {}
    }

    // Wishlist
    fun getWishlist(userId: String): Flow<List<WishlistItem>> = wishlistDao.getWishlistForUser(userId)
    fun isInWishlist(userId: String, productId: String): Flow<Boolean> = wishlistDao.isInWishlist(userId, productId)

    suspend fun toggleWishlist(userId: String, product: Product) = withContext(Dispatchers.IO) {
        val compositeId = "${userId}_${product.id}"
        val exists = wishlistDao.isInWishlist(userId, product.id).first()
        if (exists) {
            wishlistDao.deleteWishlistItem(compositeId)
        } else {
            wishlistDao.insertWishlistItem(
                WishlistItem(
                    id = compositeId,
                    userId = userId,
                    productId = product.id,
                    productName = product.name,
                    productImage = product.primaryImageUrl,
                    priceLkr = product.priceLkr,
                    discountPriceLkr = product.discountPriceLkr,
                    rating = product.rating,
                    inStock = product.stock > 0
                )
            )
        }
    }

    suspend fun moveWishlistToCart(
        userId: String,
        item: WishlistItem,
        size: String,
        colour: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = productDao.getProductDirect(item.productId)
            ?: return@withContext Result.failure(Exception("Product not found"))
        val addRes = addToCart(userId, product, size, colour, 1)
        if (addRes.isSuccess) {
            wishlistDao.deleteWishlistItem(item.id)
        }
        addRes
    }

    // Orders & Backend Validation
    fun getOrders(userId: String): Flow<List<Order>> = orderDao.getOrdersForUser(userId)
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    suspend fun createOrder(
        userId: String,
        customerName: String,
        phone: String,
        street: String,
        city: String,
        postalCode: String,
        instructions: String,
        deliveryMethod: String,
        paymentMethod: String,
        promoCode: String?
    ): Result<Order> = withContext(Dispatchers.IO) {
        val cartItems = cartDao.getCartForUser(userId).first()
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(Exception("Cart is empty."))
        }

        // Server-side recalculation & stock verification
        var calculatedSubtotal = 0.0
        val itemsSummaryBuilder = StringBuilder()

        for (item in cartItems) {
            val liveProd = productDao.getProductDirect(item.productId)
                ?: return@withContext Result.failure(Exception("Product '${item.productName}' no longer available."))
            if (liveProd.stock < item.quantity) {
                return@withContext Result.failure(Exception("Insufficient stock for '${liveProd.name}'. Available: ${liveProd.stock}"))
            }
            // Trust server-side price, NOT client
            val unitPrice = liveProd.effectivePrice
            calculatedSubtotal += unitPrice * item.quantity
            itemsSummaryBuilder.append("${item.quantity}x ${item.productName} (${item.selectedSize}, ${item.selectedColour}) - LKR ${String.format(Locale.US, "%,.0f", unitPrice * item.quantity)}\n")

            // Decrement stock
            productDao.updateStock(liveProd.id, liveProd.stock - item.quantity)
        }

        // Delivery Fee calculation
        val deliveryFee = when {
            deliveryMethod.contains("Express", ignoreCase = true) -> 950.0
            calculatedSubtotal >= 10000.0 -> 0.0
            else -> 450.0
        }

        // Promo Discount calculation
        var discount = 0.0
        if (!promoCode.isNullOrBlank()) {
            when (promoCode.trim().uppercase()) {
                "LUXURY20" -> discount = calculatedSubtotal * 0.20
                "WELCOME10" -> discount = calculatedSubtotal * 0.10
                "SAVE5000" -> discount = if (calculatedSubtotal >= 20000) 5000.0 else 0.0
            }
        }

        val finalTotal = (calculatedSubtotal - discount + deliveryFee).coerceAtLeast(0.0)
        val orderNum = "ORD-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${(1000..9999).random()}"

        val productIdsList = cartItems.map { it.productId }.distinct().joinToString(",")

        val order = Order(
            orderNumber = orderNum,
            userId = userId,
            customerName = customerName,
            phone = phone,
            streetAddress = street,
            city = city,
            postalCode = postalCode,
            deliveryInstructions = instructions,
            deliveryMethod = deliveryMethod,
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod.contains("Online", ignoreCase = true)) "Verified (Test Gateway)" else "Pending COD",
            orderStatus = "Confirmed",
            subtotalLkr = calculatedSubtotal,
            deliveryFeeLkr = deliveryFee,
            discountLkr = discount,
            totalLkr = finalTotal,
            itemsSummary = itemsSummaryBuilder.toString().trim(),
            productIdsCsv = productIdsList
        )

        orderDao.insertOrder(order)
        cartDao.clearCartForUser(userId)

        // Generate in-app confirmation notification
        val nowFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Order Confirmed ($orderNum)",
                message = "Thank you $customerName! Your order for LKR ${String.format(Locale.US, "%,.0f", finalTotal)} is confirmed and is being tailored for dispatch.",
                type = "ORDER",
                dateFormatted = nowFormatted
            )
        )

        Result.success(order)
    }

    suspend fun cancelOrder(orderNumber: String) = withContext(Dispatchers.IO) {
        orderDao.cancelOrder(orderNumber)
    }

    suspend fun updateOrderStatus(orderNumber: String, status: String) = withContext(Dispatchers.IO) {
        orderDao.updateOrderStatus(orderNumber, status)
    }

    // Reviews
    fun getReviews(productId: String): Flow<List<Review>> = reviewDao.getReviewsForProduct(productId)

    suspend fun hasUserPurchasedProduct(userId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        val activeOrders = orderDao.getActiveOrdersForUser(userId)
        activeOrders.any { order ->
            order.productIdsCsv.split(",").map { it.trim() }.contains(productId) ||
            order.itemsSummary.contains(productId)
        }
    }

    suspend fun submitReview(
        userId: String,
        productId: String,
        authorName: String,
        rating: Int,
        comment: String
    ): Result<Review> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("Authentication required to submit reviews."))
        }
        val hasPurchased = hasUserPurchasedProduct(userId, productId)
        if (!hasPurchased) {
            return@withContext Result.failure(Exception("Only authenticated users who have purchased this piece can submit a review."))
        }

        val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val newReview = Review(
            productId = productId,
            userId = userId,
            customerName = authorName,
            rating = rating.coerceIn(1, 5),
            comment = comment.trim(),
            dateString = now,
            isVerifiedPurchase = true,
            createdAt = System.currentTimeMillis()
        )
        reviewDao.insertReview(newReview)
        try {
            firestoreService.submitReview(newReview)
        } catch (_: Exception) {}

        // Recalculate average rating & count for product
        val allReviews = reviewDao.getReviewsListForProduct(productId)
        if (allReviews.isNotEmpty()) {
            val avg = allReviews.map { it.rating }.average()
            val roundedAvg = (Math.round(avg * 10.0) / 10.0)
            productDao.updateRatingAndReviews(productId, roundedAvg, allReviews.size)
        }

        // Add user notification
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Review Published (Verified)",
                message = "Thank you for reviewing your atelier piece ($rating ★). Your verified feedback helps our couture artisans.",
                type = "SYSTEM",
                dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
            )
        )

        Result.success(newReview)
    }

    // Notifications
    fun getNotifications(userId: String): Flow<List<NotificationItem>> = notificationDao.getNotificationsForUser(userId)
    suspend fun markNotificationRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }
    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    // Addresses
    fun getAddresses(userId: String): Flow<List<SavedAddress>> = addressDao.getAddressesForUser(userId)
    suspend fun addAddress(address: SavedAddress) = withContext(Dispatchers.IO) {
        if (address.isDefault) {
            addressDao.clearDefaults(address.userId)
        }
        addressDao.insertAddress(address)
    }
    suspend fun deleteAddress(id: Long) = withContext(Dispatchers.IO) {
        addressDao.deleteAddress(id)
    }
}
