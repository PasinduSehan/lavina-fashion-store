package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LuminaTopBar
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.LuminaViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SettingsScreen(viewModel: LuminaViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var notifPref by remember { mutableStateOf(true) }
    var promoPref by remember { mutableStateOf(true) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var showSupportModal by remember { mutableStateOf(false) }
    var showPolicyModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LuminaTopBar(
                title = "Settings & Preferences",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance & Notifications
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "App Theme & Appearance", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Theme Selector Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Light Mode Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (!isDarkMode) 2.dp else 1.dp,
                                    color = if (!isDarkMode) GoldPrimary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { if (isDarkMode) viewModel.toggleDarkMode() }
                                .testTag("theme_option_light"),
                            color = Color(0xFFFAF7F2)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("☀️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Ivory Couture", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1816))
                                Text("Light Theme", fontSize = 10.sp, color = Color(0xFF746F66))
                            }
                        }

                        // Dark Mode Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isDarkMode) 2.dp else 1.dp,
                                    color = if (isDarkMode) GoldPrimary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { if (!isDarkMode) viewModel.toggleDarkMode() }
                                .testTag("theme_option_dark"),
                            color = Color(0xFF141416)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌙", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Luxury Noir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF8F6F0))
                                Text("Dark Theme", fontSize = 10.sp, color = Color(0xFFA39E93))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Dark Mode", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(
                                text = if (isDarkMode) "Active: Luxury Noir theme" else "Active: Ivory Couture light theme",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = Color.Black)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Order Notifications", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Real-time courier updates", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notifPref,
                            onCheckedChange = { notifPref = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = Color.Black)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("VIP Offers & Private Sales", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Early notifications for seasonal drops", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = promoPref,
                            onCheckedChange = { promoPref = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = Color.Black)
                        )
                    }
                }
            }

            // Region & Currency
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Regional Standards", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 15.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Currency", fontSize = 13.sp)
                        Text("LKR (Sri Lankan Rupee)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Language", fontSize = 13.sp)
                        Text("English (Sri Lanka)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Legal & Concierge Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    ProfileMenuRow(
                        icon = Icons.Default.HelpCenter,
                        label = "Concierge Support & Contact",
                        onClick = { showSupportModal = true }
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        label = "Privacy Policy & Atelier Terms",
                        onClick = { showPolicyModal = true }
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.Info,
                        label = "About Lumina Atelier (v1.0.0)",
                        onClick = {}
                    )
                    HorizontalDivider()
                    ProfileMenuRow(
                        icon = Icons.Default.DeleteForever,
                        label = "Delete Customer Account",
                        isDestructive = true,
                        onClick = { showDeleteModal = true }
                    )
                }
            }
        }
    }

    // Support Modal
    if (showSupportModal) {
        AlertDialog(
            onDismissRequest = { showSupportModal = false },
            title = { Text("Atelier Concierge Support", fontFamily = FontFamily.Serif) },
            text = {
                Column {
                    Text("Our customer relationship team is at your bespoke service.")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📞 Telephone: +94 11 234 5678", fontWeight = FontWeight.Medium)
                    Text("✉️ Email: concierge@lumina.fashion", fontWeight = FontWeight.Medium)
                    Text("📍 Boutique: Lotus Grove, Colombo 07, Sri Lanka", fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(onClick = { showSupportModal = false }) { Text("Close") }
            }
        )
    }

    // Policy Modal
    if (showPolicyModal) {
        AlertDialog(
            onDismissRequest = { showPolicyModal = false },
            title = { Text("Privacy Policy & Terms", fontFamily = FontFamily.Serif) },
            text = {
                Text(
                    text = "Lumina Atelier respects your discretion. Customer profiles, addresses, and order histories are encrypted and reserved exclusively for bespoke delivery operations. We do not sell or exchange customer records with third-party advertising brokers."
                )
            },
            confirmButton = {
                Button(onClick = { showPolicyModal = false }) { Text("Acknowledge") }
            }
        )
    }

    // Delete Account Modal
    if (showDeleteModal) {
        AlertDialog(
            onDismissRequest = { showDeleteModal = false },
            title = { Text("Delete Customer Account", color = AccentRed) },
            text = {
                Text(
                    "Are you sure you wish to delete your account? This action will permanently remove your delivery addresses and disconnect your customer ID."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteModal = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Yes, Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
