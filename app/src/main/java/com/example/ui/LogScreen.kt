package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LogRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    currentRoute: String = "log",
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val logs by LogRepository.logs.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nhật ký hoạt động", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${logs.size} dòng",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = LogRepository.snapshot()
                        if (text.isBlank()) {
                            Toast.makeText(context, "Chưa có log", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Việt AI - Log")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Chia sẻ log"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                    }
                    IconButton(onClick = {
                        LogRepository.clear()
                        Toast.makeText(context, "Đã xoá log", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xoá")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chưa có nhật ký nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { line -> LogLine(line) }
                }
            }
        }
    }
}

@Composable
private fun LogLine(line: String) {
    val (color, bg) = when {
        line.contains("[ERROR]", ignoreCase = true) || line.contains("failed", ignoreCase = true)
            -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
        line.contains("[TOOL]", ignoreCase = true)
            -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
        line.contains("[OK]", ignoreCase = true) || line.contains("success", ignoreCase = true)
            -> Color(0xFF388E3C) to Color(0xFFE8F5E9)
        else -> Color(0xFF424242) to Color(0xFFF5F5F5)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(
            text = line,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
