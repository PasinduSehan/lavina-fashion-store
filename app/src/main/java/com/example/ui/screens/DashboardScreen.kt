package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Category
import com.example.ui.components.LuminaBottomNav
import com.example.ui.components.LuminaTopBar
import com.example.ui.components.ProductCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.LuminaViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun DashboardScreen(viewModel: LuminaViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    val featuredProducts = allProducts.filter { it.isFeatured }
    val newArrivals = allProducts.filter { it.isNewArrival }
    val bestSellers = allProducts.filter { it.isBestSeller }
    val discountedProducts = allProducts.filter { it.isDiscounted }
    val recommendedProducts = allProducts.take(4)
    val recentlyViewed = allProducts.takeLast(4)

    val wishlistedIds = wishlistItems.map { it.productId }.toSet()

    val isDark by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            LuminaTopBar(
                greeting = "Hello, ${currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Pasindu"}",
                cartItemCount = cartItems.sumOf { it.quantity },
                notificationCount = unreadNotifs,
                onSearchClick = { viewModel.navigateTo(Screen.PRODUCT_LIST) },
                onCartClick = { viewModel.navigateTo(Screen.CART) },
                onNotificationClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
                isDarkMode = isDark,
                onThemeToggle = { viewModel.toggleDarkMode() }
            )
        },
        bottomBar = {
            LuminaBottomNav(
                currentScreen = Screen.DASHBOARD,
                cartItemCount = cartItems.sumOf { it.quantity },
                wishlistCount = wishlistItems.size,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Search Bar Input Trigger
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo(Screen.PRODUCT_LIST) }
                            .testTag("dashboard_search_bar"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search silk blazers, dresses, shirts...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Promotional Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.selectedCategory.value = "new_arrivals"
                            viewModel.navigateTo(Screen.PRODUCT_LIST)
                        }
                        .testTag("hero_banner_card")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lumina_hero_banner_1791740336374),
                        contentDescription = "The Autumn Edit - Luxury Redefined",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // Overlay Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = GoldPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LIMITED COUTURE DROP",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "THE AUTUMN EDIT",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Text(
                            text = "Use code LUXURY20 for 20% off selected luxury tailoring",
                            color = Color(0xFFE5DFC9),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Category Chips / Avatars Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Shop by Category",
                    subtitle = "Explore signature tailoring and essentials",
                    onViewAllClick = { viewModel.navigateTo(Screen.CATEGORIES) }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(viewModel.categories.take(10)) { cat ->
                        CategoryItem(
                            category = cat,
                            onClick = {
                                viewModel.selectedCategory.value = cat.id
                                viewModel.navigateTo(Screen.PRODUCT_LIST)
                            }
                        )
                    }
                }
            }

            // Promotional Special Offer Card
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.appliedPromoCode.value = "LUXURY20"
                            viewModel.navigateTo(Screen.CART)
                        },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "VIP Atelier Privilege: 20% Off",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Tap to apply voucher 'LUXURY20' to your bag",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = GoldPrimary
                        )
                    }
                }
            }

            // Featured Products Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(
                    title = "Featured Collections",
                    subtitle = "Curated luxury designs crafted for distinction",
                    onViewAllClick = {
                        viewModel.selectedCategory.value = "all"
                        viewModel.sortOption.value = "featured"
                        viewModel.navigateTo(Screen.PRODUCT_LIST)
                    }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featuredProducts) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // New Arrivals Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "New Arrivals",
                    subtitle = "Fresh tailoring, silk gowns, and seasonal textures",
                    onViewAllClick = {
                        viewModel.selectedCategory.value = "new_arrivals"
                        viewModel.navigateTo(Screen.PRODUCT_LIST)
                    }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newArrivals) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // Best Sellers Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Best Sellers",
                    subtitle = "Most celebrated wardrobe essentials",
                    onViewAllClick = {
                        viewModel.selectedCategory.value = "all"
                        viewModel.sortOption.value = "best_selling"
                        viewModel.navigateTo(Screen.PRODUCT_LIST)
                    }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(bestSellers) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // Discounted Products Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Seasonal Sale & Privilege",
                    subtitle = "Exclusive markdowns on premier apparel",
                    onViewAllClick = {
                        viewModel.selectedCategory.value = "sale"
                        viewModel.navigateTo(Screen.PRODUCT_LIST)
                    }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(discountedProducts) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // Recommended Products
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Recommended For You",
                    subtitle = "Handpicked based on your taste",
                    onViewAllClick = {
                        viewModel.selectedCategory.value = "all"
                        viewModel.navigateTo(Screen.PRODUCT_LIST)
                    }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recommendedProducts) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // Recently Viewed Products
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Recently Viewed",
                    subtitle = "Quickly resume your shopping journey",
                    onViewAllClick = null
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyViewed) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = {
                                viewModel.openProductDetails(product)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("category_chip_${category.id}")
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape)
        ) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
