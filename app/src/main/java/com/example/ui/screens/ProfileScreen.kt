package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedAddress
import com.example.ui.components.LuminaBottomNav
import com.example.ui.components.LuminaTopBar
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.LuminaViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ProfileScreen(viewModel: LuminaViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddressManagerDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LuminaTopBar(
                title = "Customer Profile",
                showBackButton = false,
                cartItemCount = cartItems.sumOf { it.quantity },
                notificationCount = unreadNotifs,
                onSearchClick = { viewModel.navigateTo(Screen.PRODUCT_LIST) },
                onCartClick = { viewModel.navigateTo(Screen.CART) },
                onNotificationClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) }
            )
        },
        bottomBar = {
            LuminaBottomNav(
                currentScreen = Screen.PROFILE,
                cartItemCount = cartItems.sumOf { it.quantity },
                wishlistCount = wishlistItems.size,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { paddingValues ->
        val user = currentUser ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.5.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = GoldPrimary,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Surface(
                        color = GoldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Customer ID: ${user.customerId}",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = user.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Actions List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    ProfileMenuRow(
                        icon = Icons.Default.ShoppingBag,
                        label = "My Orders & Receipts",
                        onClick = { viewModel.navigateTo(Screen.ORDERS) }
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Favorite,
                        label = "Saved Wishlist Pieces",
                        onClick = { viewModel.navigateTo(Screen.WISHLIST) }
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Place,
                        label = "Manage Delivery Addresses (${savedAddresses.size})",
                        onClick = { showAddressManagerDialog = true }
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Settings,
                        label = "App Preferences & Settings",
                        onClick = { viewModel.navigateTo(Screen.SETTINGS) }
                    )

                    if (user.role == "ADMIN") {
                        HorizontalDivider()
                        ProfileMenuRow(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "Atelier Management Portal (Admin)",
                            isSpecial = true,
                            onClick = { viewModel.navigateTo(Screen.ADMIN) }
                        )
                    }

                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Logout,
                        label = "Sign Out from Atelier",
                        isDestructive = true,
                        onClick = { viewModel.logout() }
                    )
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(currentUser?.fullName ?: "") }
        var editPhone by remember { mutableStateOf(currentUser?.phone ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Update Profile", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Contact Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editPhone)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Address Manager Dialog
    if (showAddressManagerDialog) {
        AlertDialog(
            onDismissRequest = { showAddressManagerDialog = false },
            title = { Text("Saved Addresses", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (savedAddresses.isEmpty()) {
                        Text("No delivery addresses saved yet.")
                    } else {
                        savedAddresses.forEach { addr ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = addr.label, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 12.sp)
                                    Text(text = "${addr.street}, ${addr.city} (${addr.postalCode})", fontSize = 12.sp)
                                    Text(text = "Recipient: ${addr.recipientName} (${addr.phone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddressManagerDialog = false
                        showAddAddressDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Add New Address")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressManagerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Address Dialog
    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Office") }
        var recipient by remember { mutableStateOf(currentUser?.fullName ?: "") }
        var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
        var street by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("Colombo") }
        var postal by remember { mutableStateOf("00100") }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Delivery Address", fontFamily = FontFamily.Serif) },
            text = {
                Column {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Home, Office)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street Address") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = postal, onValueChange = { postal = it }, label = { Text("Postal Code") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (street.isNotBlank()) {
                            viewModel.addAddress(label, recipient, phone, street, city, postal)
                            showAddAddressDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Save Address")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    isSpecial: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) AccentRed else if (isSpecial) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
                color = if (isDestructive) AccentRed else if (isSpecial) GoldPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
