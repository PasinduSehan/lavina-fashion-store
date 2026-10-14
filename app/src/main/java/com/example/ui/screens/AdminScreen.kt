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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.ui.components.LuminaTopBar
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.LuminaViewModel
import java.util.Locale

@Composable
fun AdminScreen(viewModel: LuminaViewModel) {
    val adminProducts by viewModel.adminProducts.collectAsState()
    val adminOrders by viewModel.allAdminOrders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inventory & Stock (${adminProducts.size})", "Customer Orders (${adminOrders.size})")

    Scaffold(
        topBar = {
            LuminaTopBar(
                title = "Atelier Admin Console",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // Products / Stock Management
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(adminProducts) { product ->
                        AdminProductCard(
                            product = product,
                            onStockChange = { delta ->
                                val newStock = (product.stock + delta).coerceAtLeast(0)
                                viewModel.updateProductStock(product.id, newStock)
                            }
                        )
                    }
                }
            } else {
                // Orders Management
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(adminOrders) { order ->
                        AdminOrderCard(
                            order = order,
                            onAdvanceStatus = { nextStatus ->
                                viewModel.updateOrderStatus(order.orderNumber, nextStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProductCard(
    product: Product,
    onStockChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("admin_product_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.primaryImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(text = "LKR ${String.format(Locale.US, "%,.0f", product.priceLkr)}  •  Cat: ${product.categoryId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Current Stock: ${product.stock} units",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (product.stock == 0) Color.Red else GoldPrimary
                )
            }

            // Quick Stock Adjust Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStockChange(-1) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Stock")
                }
                IconButton(onClick = { onStockChange(5) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add 5 Stock", tint = GoldPrimary)
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onAdvanceStatus: (String) -> Unit
) {
    var showStatusModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("admin_order_${order.orderNumber}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = order.orderNumber, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                Surface(
                    color = GoldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = order.orderStatus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Customer: ${order.customerName} (${order.phone})", fontSize = 12.sp)
            Text(text = "Destination: ${order.streetAddress}, ${order.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Amount: LKR ${String.format(Locale.US, "%,.0f", order.totalLkr)} (${order.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showStatusModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text("Update Order Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showStatusModal) {
        val statuses = listOf("Pending", "Confirmed", "Processing", "Shipped", "Out for Delivery", "Delivered", "Cancelled")
        AlertDialog(
            onDismissRequest = { showStatusModal = false },
            title = { Text("Update Order Status") },
            text = {
                Column {
                    statuses.forEach { st ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onAdvanceStatus(st)
                                    showStatusModal = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(text = st, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusModal = false }) { Text("Cancel") }
            }
        )
    }
}
