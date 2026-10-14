package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LuminaBottomNav
import com.example.ui.components.LuminaTopBar
import com.example.ui.components.ProductCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.LuminaViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(viewModel: LuminaViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val minPrice by viewModel.selectedMinPrice.collectAsState()
    val maxPrice by viewModel.selectedMaxPrice.collectAsState()
    val sizeFilter by viewModel.selectedSizeFilter.collectAsState()
    val colourFilter by viewModel.selectedColourFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val inStockOnly by viewModel.inStockOnly.collectAsState()
    val onSaleOnly by viewModel.onSaleOnly.collectAsState()

    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsState()

    val wishlistedIds = wishlistItems.map { it.productId }.toSet()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LuminaTopBar(
                title = "Catalogue",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                cartItemCount = cartItems.sumOf { it.quantity },
                notificationCount = unreadNotifs,
                onSearchClick = {},
                onCartClick = { viewModel.navigateTo(Screen.CART) },
                onNotificationClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) }
            )
        },
        bottomBar = {
            LuminaBottomNav(
                currentScreen = Screen.PRODUCT_LIST,
                cartItemCount = cartItems.sumOf { it.quantity },
                wishlistCount = wishlistItems.size,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Box
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name, brand, or style...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            // Category Quick Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.categories) { cat ->
                    val isSelected = selectedCat.equals(cat.id, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                viewModel.selectedCategory.value = cat.id
                            }
                            .testTag("chip_category_${cat.id}"),
                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = cat.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Filter & Sort Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProducts.size} Luxury Pieces",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Filter Button
                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("open_filters_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filters", fontSize = 12.sp)
                    }

                    // Sort Button
                    OutlinedButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.testTag("open_sort_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortOption) {
                                "price_asc" -> "Price ↑"
                                "price_desc" -> "Price ↓"
                                "newest" -> "Newest"
                                "best_selling" -> "Best Selling"
                                "rating" -> "Top Rated"
                                else -> "Featured"
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Product Grid or Empty State
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Apparel Matches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing filters or search terms to view all collections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedCategory.value = "all"
                                viewModel.selectedSizeFilter.value = null
                                viewModel.selectedColourFilter.value = null
                                viewModel.inStockOnly.value = false
                                viewModel.onSaleOnly.value = false
                                viewModel.selectedMinPrice.value = 0f
                                viewModel.selectedMaxPrice.value = 60000f
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = wishlistedIds.contains(product.id),
                            onCardClick = { viewModel.openProductDetails(product) },
                            onWishlistClick = { viewModel.toggleWishlist(product) },
                            onAddToCartClick = { viewModel.openProductDetails(product) }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Collections",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        viewModel.selectedSizeFilter.value = null
                        viewModel.selectedColourFilter.value = null
                        viewModel.inStockOnly.value = false
                        viewModel.onSaleOnly.value = false
                        viewModel.selectedMinPrice.value = 0f
                        viewModel.selectedMaxPrice.value = 60000f
                    }) {
                        Text("Reset", color = GoldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price Range Slider
                Text(
                    text = "Price Range: LKR ${String.format(Locale.US, "%,.0f", minPrice)} - LKR ${String.format(Locale.US, "%,.0f", maxPrice)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                RangeSlider(
                    value = minPrice..maxPrice,
                    onValueChange = { range ->
                        viewModel.selectedMinPrice.value = range.start
                        viewModel.selectedMaxPrice.value = range.endInclusive
                    },
                    valueRange = 0f..60000f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Size Filter
                Text(text = "Sizes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val sizes = listOf("XS", "S", "M", "L", "XL", "XXL")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(sizes) { sz ->
                        val isSelected = sizeFilter == sz
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectedSizeFilter.value = if (isSelected) null else sz
                            },
                            label = { Text(sz) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Colour Filter
                Text(text = "Signature Tones", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val colours = listOf("Black", "White", "Beige", "Navy", "Emerald", "Camel", "Ivory")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(colours) { col ->
                        val isSelected = colourFilter == col
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectedColourFilter.value = if (isSelected) null else col
                            },
                            label = { Text(col) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Checkbox Options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = inStockOnly,
                        onCheckedChange = { viewModel.inStockOnly.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text("In Stock Only", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = onSaleOnly,
                        onCheckedChange = { viewModel.onSaleOnly.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text("On Sale / Special Discount", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        scope.launch { filterSheetState.hide() }.invokeOnCompletion {
                            showFilterSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Sort Apparel By",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val sortOptions = listOf(
                    "featured" to "Featured At Lavina",
                    "price_asc" to "Price: Low to High",
                    "price_desc" to "Price: High to Low",
                    "newest" to "Newest Arrivals",
                    "best_selling" to "Best Selling Pieces",
                    "rating" to "Highest Customer Rating"
                )

                sortOptions.forEach { (key, label) ->
                    val isSelected = sortOption == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.sortOption.value = key
                                scope.launch { sortSheetState.hide() }.invokeOnCompletion {
                                    showSortSheet = false
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(GoldPrimary, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
