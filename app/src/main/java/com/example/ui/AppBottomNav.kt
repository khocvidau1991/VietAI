package com.example.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class AppNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val APP_NAV_ITEMS = listOf(
    AppNavItem("chat", "Chat", Icons.Default.ChatBubble),
    AppNavItem("video", "Nhạc", Icons.Default.Headphones),
    AppNavItem("local", "Dữ liệu", Icons.Default.Storage),
    AppNavItem("privacy", "Riêng tư", Icons.Default.Security),
    AppNavItem("settings", "Cài đặt", Icons.Default.Settings),
)

@Composable
fun AppBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        APP_NAV_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { if (item.route != currentRoute) onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { androidx.compose.material3.Text(item.label) },
                alwaysShowLabel = true
            )
        }
    }
}
