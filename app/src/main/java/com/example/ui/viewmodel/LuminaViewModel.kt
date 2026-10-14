package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CatalogSeeder
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SavedAddress
import com.example.data.model.User
import com.example.data.model.WishlistItem
import com.example.data.repository.LuminaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN,
    DASHBOARD,
    CATEGORIES,
    PRODUCT_LIST,
    PRODUCT_DETAIL,
    CART,
    CHECKOUT,
    ORDER_CONFIRMATION,
    ORDERS,
    ORDER_TRACKING,
    WISHLIST,
    PROFILE,
    SETTINGS,
    NOTIFICATIONS,
    ADMIN
}

class LuminaViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LuminaRepository(application)

    // Navigation Stack
    private val _screenStack = MutableStateFlow(listOf(Screen.LOGIN))
    val currentScreen: StateFlow<Screen> = _screenStack
        .combine(MutableStateFlow(Unit)) { stack, _ -> stack.lastOrNull() ?: Screen.LOGIN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Screen.LOGIN)

    fun navigateTo(screen: Screen) {
        val current = _screenStack.value
        _screenStack.value = current + screen
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            return true
        }
        return false
    }

    fun navigateToRoot(screen: Screen) {
        _screenStack.value = listOf(screen)
    }

    fun syncScreenFromNav(screen: Screen) {
        val current = _screenStack.value
        if (current.lastOrNull() != screen) {
            val index = current.lastIndexOf(screen)
            if (index >= 0) {
                _screenStack.value = current.take(index + 1)
            } else {
                _screenStack.value = current + screen
            }
        }
    }

    // Current User
    val currentUser: StateFlow<User?> = repository.currentUser

    // Auth States
    val authLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val showOnboardingModal = MutableStateFlow(false)
    val showForgotPasswordModal = MutableStateFlow(false)
    val forgotPasswordSuccess = MutableStateFlow<String?>(null)

    // Dark Mode Toggle
    val isDarkMode = MutableStateFlow(false)
    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // Catalog & Categories
    val categories: List<Category> = CatalogSeeder.categories
    val allProducts: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminProducts: StateFlow<List<Product>> = repository.getAllProductsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Filter & Search
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("all")
    val selectedMinPrice = MutableStateFlow(0f)
    val selectedMaxPrice = MutableStateFlow(60000f)
    val selectedSizeFilter = MutableStateFlow<String?>(null)
    val selectedColourFilter = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow("featured") // featured, price_asc, price_desc, newest, best_selling, rating
    val inStockOnly = MutableStateFlow(false)
    val onSaleOnly = MutableStateFlow(false)

    // Filtered Products
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        selectedCategory,
        selectedMinPrice,
        selectedMaxPrice,
        selectedSizeFilter,
        selectedColourFilter,
        sortOption,
        inStockOnly,
        onSaleOnly
    ) { params ->
        val prods = params[0] as List<Product>
        val query = (params[1] as String).trim().lowercase()
        val category = params[2] as String
        val minP = params[3] as Float
        val maxP = params[4] as Float
        val size = params[5] as String?
        val colour = params[6] as String?
        val sort = params[7] as String
        val inStock = params[8] as Boolean
        val onSale = params[9] as Boolean

        prods.filter { p ->
            val matchesCategory = when (category) {
                "all" -> true
                "new_arrivals" -> p.isNewArrival
                "sale" -> p.isDiscounted
                "mens" -> p.categoryId in listOf("shirts", "tshirts", "trousers", "jeans", "jackets", "hoodies")
                "womens" -> p.categoryId in listOf("dresses", "tshirts", "trousers", "jackets")
                else -> p.categoryId.equals(category, ignoreCase = true)
            }

            val matchesQuery = if (query.isEmpty()) true else {
                p.name.lowercase().contains(query) ||
                        p.description.lowercase().contains(query) ||
                        p.brand.lowercase().contains(query) ||
                        p.categoryId.lowercase().contains(query)
            }

            val price = p.effectivePrice
            val matchesPrice = price >= minP && price <= maxP

            val matchesSize = size == null || p.sizes.any { it.equals(size, ignoreCase = true) }
            val matchesColour = colour == null || p.colours.any { it.contains(colour, ignoreCase = true) }
            val matchesStock = !inStock || p.stock > 0
            val matchesSale = !onSale || p.isDiscounted

            matchesCategory && matchesQuery && matchesPrice && matchesSize && matchesColour && matchesStock && matchesSale
        }.let { list ->
            when (sort) {
                "price_asc" -> list.sortedBy { it.effectivePrice }
                "price_desc" -> list.sortedByDescending { it.effectivePrice }
                "newest" -> list.sortedByDescending { if (it.isNewArrival) 1 else 0 }
                "best_selling" -> list.sortedByDescending { if (it.isBestSeller) 1 else 0 }
                "rating" -> list.sortedByDescending { it.rating }
                else -> list.sortedByDescending { if (it.isFeatured) 1 else 0 }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Product Selection for Details
    val selectedProduct = MutableStateFlow<Product?>(null)
    val selectedSize = MutableStateFlow<String>("")
    val selectedColour = MutableStateFlow<String>("")
    val selectedQuantity = MutableStateFlow(1)
    val showSizeGuide = MutableStateFlow(false)

    // Purchase check & Review status
    val isUserPurchasedCurrentProduct = MutableStateFlow(false)
    val reviewSubmissionError = MutableStateFlow<String?>(null)
    val reviewSubmissionSuccess = MutableStateFlow<String?>(null)

    fun openProductDetails(product: Product) {
        selectedProduct.value = product
        selectedSize.value = product.sizes.firstOrNull() ?: "M"
        selectedColour.value = product.colours.firstOrNull() ?: "Black"
        selectedQuantity.value = 1
        reviewSubmissionError.value = null
        reviewSubmissionSuccess.value = null
        checkPurchaseStatus(product.id)
        loadReviewsForProduct(product.id)
        navigateTo(Screen.PRODUCT_DETAIL)
    }

    fun checkPurchaseStatus(productId: String) {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                isUserPurchasedCurrentProduct.value = repository.hasUserPurchasedProduct(user.uid, productId)
            }
        } else {
            isUserPurchasedCurrentProduct.value = false
        }
    }

    // Active Product Reviews
    val currentProductReviews = MutableStateFlow<List<Review>>(emptyList())
    fun loadReviewsForProduct(productId: String) {
        viewModelScope.launch {
            repository.getReviews(productId).collect {
                currentProductReviews.value = it
            }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        val prod = selectedProduct.value ?: return
        val user = currentUser.value
        if (user == null) {
            reviewSubmissionError.value = "Please sign in to leave a review."
            return
        }
        viewModelScope.launch {
            reviewSubmissionError.value = null
            reviewSubmissionSuccess.value = null
            val result = repository.submitReview(user.uid, prod.id, user.fullName, rating, comment)
            if (result.isSuccess) {
                reviewSubmissionSuccess.value = "Thank you! Your verified purchase review has been published."
            } else {
                reviewSubmissionError.value = result.exceptionOrNull()?.message ?: "Failed to submit review."
            }
        }
    }

    // Cart
    val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val appliedPromoCode = MutableStateFlow<String?>(null)
    val promoError = MutableStateFlow<String?>(null)

    val cartSubtotal: StateFlow<Double> = cartItems
        .combine(MutableStateFlow(Unit)) { items, _ ->
            items.sumOf { it.priceLkr * it.quantity }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartDiscount: StateFlow<Double> = combine(cartSubtotal, appliedPromoCode) { subtotal, promo ->
        when (promo?.uppercase()) {
            "LUXURY20" -> subtotal * 0.20
            "WELCOME10" -> subtotal * 0.10
            "SAVE5000" -> if (subtotal >= 20000) 5000.0 else 0.0
            else -> 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFee: StateFlow<Double> = cartSubtotal
        .combine(MutableStateFlow(Unit)) { subtotal, _ ->
            if (subtotal >= 10000.0 || subtotal == 0.0) 0.0 else 450.0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartGrandTotal: StateFlow<Double> = combine(cartSubtotal, cartDiscount, deliveryFee) { sub, disc, deliv ->
        (sub - disc + deliv).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Wishlist
    val wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())

    // Notifications
    val notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val unreadNotifCount = MutableStateFlow(0)

    // Orders
    val orders = MutableStateFlow<List<Order>>(emptyList())
    val allAdminOrders = MutableStateFlow<List<Order>>(emptyList())
    val trackingOrder = MutableStateFlow<Order?>(null)
    val confirmedOrder = MutableStateFlow<Order?>(null)

    // Addresses
    val savedAddresses = MutableStateFlow<List<SavedAddress>>(emptyList())

    // Checkout Form
    val checkoutRecipient = MutableStateFlow("")
    val checkoutPhone = MutableStateFlow("")
    val checkoutStreet = MutableStateFlow("")
    val checkoutCity = MutableStateFlow("Colombo 07")
    val checkoutPostalCode = MutableStateFlow("00700")
    val checkoutInstructions = MutableStateFlow("")
    val checkoutDeliveryMethod = MutableStateFlow("Standard Courier")
    val checkoutPaymentMethod = MutableStateFlow("Cash on Delivery")
    val isPlacingOrder = MutableStateFlow(false)
    val checkoutError = MutableStateFlow<String?>(null)

    init {
        // Collect current user and attach listeners
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    checkoutRecipient.value = user.fullName
                    checkoutPhone.value = user.phone

                    // Cart Flow
                    launch {
                        repository.getCart(user.uid).collect { cartItems.value = it }
                    }
                    // Wishlist Flow
                    launch {
                        repository.getWishlist(user.uid).collect { wishlistItems.value = it }
                    }
                    // Orders Flow
                    launch {
                        repository.getOrders(user.uid).collect { orders.value = it }
                    }
                    // Notifications Flow
                    launch {
                        repository.getNotifications(user.uid).collect { notifs ->
                            notifications.value = notifs
                            unreadNotifCount.value = notifs.count { !it.isRead }
                        }
                    }
                    // Addresses Flow
                    launch {
                        repository.getAddresses(user.uid).collect { addresses ->
                            savedAddresses.value = addresses
                            val defaultAddr = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                            if (defaultAddr != null) {
                                checkoutStreet.value = defaultAddr.street
                                checkoutCity.value = defaultAddr.city
                                checkoutPostalCode.value = defaultAddr.postalCode
                            }
                        }
                    }
                    // Admin Orders Flow if admin
                    if (user.role == "ADMIN") {
                        launch {
                            repository.getAllOrders().collect { allAdminOrders.value = it }
                        }
                    }
                } else {
                    cartItems.value = emptyList()
                    wishlistItems.value = emptyList()
                    orders.value = emptyList()
                    notifications.value = emptyList()
                }
            }
        }
    }

    // Actions
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val result = repository.login(email, pass)
            authLoading.value = false
            if (result.isSuccess) {
                navigateToRoot(Screen.DASHBOARD)
            } else {
                authError.value = result.exceptionOrNull()?.message ?: "Authentication failed."
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmed = email.trim().lowercase()
        if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            authError.value = "Please enter a valid email address."
            return
        }
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val res = repository.sendPasswordReset(trimmed)
            authLoading.value = false
            if (res.isSuccess) {
                forgotPasswordSuccess.value = "Password reset instructions sent to $trimmed."
            } else {
                authError.value = res.exceptionOrNull()?.message ?: "Failed to send reset email."
            }
        }
    }

    fun registerNewUser(
        fullName: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String,
        inviteCode: String = ""
    ) {
        if (fullName.isBlank()) {
            authError.value = "Full Name cannot be empty."
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            authError.value = "Please enter a valid email address."
            return
        }
        if (phone.trim().length < 8) {
            authError.value = "Please enter a valid phone number."
            return
        }
        if (pass.length < 6) {
            authError.value = "Password must be at least 6 characters."
            return
        }
        if (pass != confirmPass) {
            authError.value = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val res = repository.registerNewUser(
                fullName = fullName,
                customerId = "",
                phone = phone,
                email = email,
                pass = pass,
                inviteCode = inviteCode
            )
            authLoading.value = false
            if (res.isSuccess) {
                showOnboardingModal.value = false
                navigateToRoot(Screen.DASHBOARD)
            } else {
                authError.value = res.exceptionOrNull()?.message ?: "Registration failed."
            }
        }
    }

    fun onboardCustomer(
        name: String,
        custId: String,
        phone: String,
        email: String,
        pass: String,
        code: String
    ) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val res = repository.onboardAuthorizedCustomer(name, custId, phone, email, pass, code)
            authLoading.value = false
            if (res.isSuccess) {
                showOnboardingModal.value = false
                navigateToRoot(Screen.DASHBOARD)
            } else {
                authError.value = res.exceptionOrNull()?.message ?: "Onboarding failed"
            }
        }
    }

    fun onGoogleSignInSuccess(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val res = repository.signInWithGoogle(firebaseUser)
            authLoading.value = false
            if (res.isSuccess) {
                navigateToRoot(Screen.DASHBOARD)
            } else {
                authError.value = res.exceptionOrNull()?.message ?: "Google Sign-In failed"
            }
        }
    }

    fun logout() {
        repository.logout()
        navigateToRoot(Screen.LOGIN)
    }

    fun addToCartCurrentProduct() {
        val prod = selectedProduct.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.addToCart(
                userId = user.uid,
                product = prod,
                size = selectedSize.value,
                colour = selectedColour.value,
                quantity = selectedQuantity.value
            )
            if (res.isSuccess) {
                navigateTo(Screen.CART)
            }
        }
    }

    fun buyNowCurrentProduct() {
        val prod = selectedProduct.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.addToCart(
                userId = user.uid,
                product = prod,
                size = selectedSize.value,
                colour = selectedColour.value,
                quantity = selectedQuantity.value
            )
            if (res.isSuccess) {
                navigateTo(Screen.CHECKOUT)
            }
        }
    }

    fun updateCartQty(item: CartItem, delta: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(item.id, item.quantity + delta)
        }
    }

    fun removeCartItem(item: CartItem) {
        viewModelScope.launch {
            repository.removeCartItem(item.id)
        }
    }

    fun applyPromoCode(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed == "LUXURY20" || trimmed == "WELCOME10" || trimmed == "SAVE5000") {
            appliedPromoCode.value = trimmed
            promoError.value = null
        } else {
            promoError.value = "Invalid promotional voucher code."
        }
    }

    fun removePromoCode() {
        appliedPromoCode.value = null
        promoError.value = null
    }

    fun toggleWishlist(product: Product) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleWishlist(user.uid, product)
        }
    }

    fun moveWishlistToCart(item: WishlistItem, size: String = "M", colour: String = "Black") {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.moveWishlistToCart(user.uid, item, size, colour)
        }
    }

    fun placeOrder() {
        val user = currentUser.value ?: return
        if (checkoutRecipient.value.isBlank() || checkoutPhone.value.isBlank() || checkoutStreet.value.isBlank()) {
            checkoutError.value = "Please fill in all mandatory delivery address fields."
            return
        }

        viewModelScope.launch {
            isPlacingOrder.value = true
            checkoutError.value = null
            val result = repository.createOrder(
                userId = user.uid,
                customerName = checkoutRecipient.value,
                phone = checkoutPhone.value,
                street = checkoutStreet.value,
                city = checkoutCity.value,
                postalCode = checkoutPostalCode.value,
                instructions = checkoutInstructions.value,
                deliveryMethod = checkoutDeliveryMethod.value,
                paymentMethod = checkoutPaymentMethod.value,
                promoCode = appliedPromoCode.value
            )
            isPlacingOrder.value = false
            if (result.isSuccess) {
                confirmedOrder.value = result.getOrNull()
                appliedPromoCode.value = null
                navigateTo(Screen.ORDER_CONFIRMATION)
            } else {
                checkoutError.value = result.exceptionOrNull()?.message ?: "Failed to process order."
            }
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            repository.cancelOrder(order.orderNumber)
        }
    }

    fun trackOrder(order: Order) {
        trackingOrder.value = order
        navigateTo(Screen.ORDER_TRACKING)
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.uid)
        }
    }

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            repository.updateProfile(name, phone)
        }
    }

    fun addAddress(label: String, recipient: String, phone: String, street: String, city: String, postal: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.addAddress(
                SavedAddress(
                    userId = user.uid,
                    label = label,
                    recipientName = recipient,
                    phone = phone,
                    street = street,
                    city = city,
                    postalCode = postal,
                    isDefault = false
                )
            )
        }
    }

    // Admin Controls
    fun updateProductStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, newStock)
        }
    }

    fun updateOrderStatus(orderNumber: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderNumber, newStatus)
        }
    }
}
