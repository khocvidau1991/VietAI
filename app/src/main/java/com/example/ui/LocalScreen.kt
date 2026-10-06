package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalScreen(
    viewModel: ChatViewModel = viewModel(),
    currentRoute: String = "local",
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()

    var dbSize by remember { mutableStateOf(0L) }

    fun refreshSizes() {
        dbSize = try {
            val dbFile = context.getDatabasePath("chat_database")
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (e: Exception) { 0L }
    }

    LaunchedEffect(Unit) { refreshSizes() }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.2f MB".format(kb / 1024.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dữ liệu cục bộ") },
                navigationIcon = {
                    IconButton(onClick = { onNavigate("chat") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshSizes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                    }
                }
            )
        },
        bottomBar = { AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Thống kê", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatCard("Tin nhắn đã lưu", "${messages.size} tin nhắn")
            StatCard("Cơ sở dữ liệu", formatBytes(dbSize))

            Divider()

            Text("Hành động", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            OutlinedButton(
                onClick = {
                    if (messages.isEmpty()) {
                        Toast.makeText(context, "Chưa có tin nhắn", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val sb = StringBuilder()
                    sb.append("=== Lịch sử trò chuyện ===\n\n")
                    messages.forEach { msg ->
                        val who = if (msg.isUser) "Bạn" else "AI"
                        sb.append("[$who] ${msg.text}\n\n")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Lịch sử trò chuyện")
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                    }
                    context.startActivity(Intent.createChooser(intent, "Chia sẻ"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xuất lịch sử trò chuyện")
            }

            Button(
                onClick = {
                    viewModel.clearHistory()
                    refreshSizes()
                    Toast.makeText(context, "Đã xoá lịch sử", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xoá lịch sử trò chuyện")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Tất cả dữ liệu lưu trong bộ nhớ riêng của ứng dụng. Không có máy chủ nào của chúng tôi lưu dữ liệu của bạn.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
