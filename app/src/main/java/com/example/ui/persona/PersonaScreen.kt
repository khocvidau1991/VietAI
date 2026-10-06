package com.example.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Persona
import com.example.data.PersonaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    onBack: () -> Unit = {},
    onPersonaSelected: (Persona) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PersonaRepository(context) }
    val allPersonas by repo.personas.collectAsState(initial = emptyList())

    var currentPath by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<Persona?>(null) }
    var deletingPersona by remember { mutableStateOf<Persona?>(null) }

    LaunchedEffect(Unit) { repo.ensureDefaults() }

    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(allPersonas, searchQuery) {
        if (!isSearching) emptyList()
        else allPersonas.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val treeItems = remember(allPersonas, currentPath) {
        buildTreeItems(allPersonas, currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nhân vật AI", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${allPersonas.size} nhân vật • " +
                                if (currentPath.isEmpty()) "Gốc" else currentPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            searchQuery = ""
                        } else if (currentPath.isNotEmpty()) {
                            currentPath = currentPath.substringBeforeLast("/", "")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Tạo nhân vật")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Tìm nhân vật (VD: Toán, Y, Giải phẫu...)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Xoá")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            if (!isSearching && currentPath.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        BreadcrumbChip(
                            label = "Gốc",
                            selected = false,
                            onClick = { currentPath = "" }
                        )
                    }
                    val segments = currentPath.split("/")
                    segments.forEachIndexed { idx, seg ->
                        item {
                            Text("›",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            val pathUpTo = segments.take(idx + 1).joinToString("/")
                            BreadcrumbChip(
                                label = seg,
                                selected = idx == segments.size - 1,
                                onClick = { currentPath = pathUpTo }
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isSearching) "${searchResults.size} kết quả"
                       else "${treeItems.size} mục",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            val displayItems: List<Any> = if (isSearching) searchResults else treeItems
            if (displayItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Không có nhân vật nào",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isSearching) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Xoá tìm kiếm")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayItems) { item ->
                        when (item) {
                            is TreeItem.Folder -> FolderCard(
                                name = item.segment,
                                childCount = item.childCount,
                                onClick = { currentPath = item.fullPath }
                            )
                            is TreeItem.Leaf -> {
                                // ==== FIX: tách lambda ra biến để tránh parse sai ====
                                val editAction: (() -> Unit)? =
                                    if (!item.persona.builtIn) {
                                        { editingPersona = item.persona }
                                    } else {
                                        null
                                    }
                                val deleteAction: (() -> Unit)? =
                                    if (!item.persona.builtIn) {
                                        { deletingPersona = item.persona }
                                    } else {
                                        null
                                    }

                                PersonaCard(
                                    persona = item.persona,
                                    selected = selectedId == item.persona.id,
                                    onClick = {
                                        selectedId = item.persona.id
                                        onPersonaSelected(item.persona)
                                    },
                                    onEdit = editAction,
                                    onDelete = deleteAction
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tạo nhân vật mới")
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PersonaEditorDialog(
            title = "Tạo nhân vật mới",
            initial = null,
            defaultPath = if (currentPath.isEmpty()) "Chung" else currentPath,
            onDismiss = { showCreateDialog = false },
            onSave = { newPersona ->
                scope.launch {
                    val id = repo.insert(newPersona)
                    showCreateDialog = false
                    val created = newPersona.copy(id = id)
                    selectedId = id
                    onPersonaSelected(created)
                }
            }
        )
    }

    editingPersona?.let { editing ->
        PersonaEditorDialog(
            title = "Sửa nhân vật",
            initial = editing,
            defaultPath = editing.category,
            onDismiss = { editingPersona = null },
            onSave = { updated ->
                scope.launch {
                    repo.update(updated)
                    editingPersona = null
                    if (selectedId == updated.id) onPersonaSelected(updated)
                }
            }
        )
    }

    deletingPersona?.let { deleting ->
        AlertDialog(
            onDismissRequest = { deletingPersona = null },
            title = { Text("Xoá nhân vật") },
            text = { Text("Bạn chắc chắn muốn xoá \"${deleting.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.delete(deleting)
                        if (selectedId == deleting.id) selectedId = null
                        deletingPersona = null
                    }
                }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPersona = null }) { Text("Huỷ") }
            }
        )
    }
}

// ============ TREE BUILDER ============
sealed class TreeItem {
    data class Folder(
        val segment: String,
        val fullPath: String,
        val childCount: Int
    ) : TreeItem()
    data class Leaf(val persona: Persona) : TreeItem()
}

private fun buildTreeItems(all: List<Persona>, currentPath: String): List<TreeItem> {
    val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
    val folders = mutableMapOf<String, Int>()
    val leaves = mutableListOf<Persona>()

    for (p in all) {
        if (currentPath.isNotEmpty() && !p.category.startsWith(prefix)) continue
        if (currentPath.isNotEmpty() && p.category == currentPath) continue

        val rest = if (currentPath.isEmpty()) p.category
                   else p.category.removePrefix(prefix)
        if (rest.isBlank()) continue

        val slashIdx = rest.indexOf('/')
        if (slashIdx < 0) {
            leaves.add(p)
        } else {
            val seg = rest.substring(0, slashIdx)
            folders[seg] = (folders[seg] ?: 0) + 1
        }
    }

    val folderItems = folders.map { (seg, count) ->
        val fullPath = if (currentPath.isEmpty()) seg else "$currentPath/$seg"
        TreeItem.Folder(seg, fullPath, count)
    }.sortedBy { it.segment }

    val leafItems = leaves.sortedBy { it.name }.map { TreeItem.Leaf(it) }

    return folderItems + leafItems
}

// ============ UI COMPOSABLES ============
@Composable
private fun BreadcrumbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FolderCard(name: String, childCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("$childCount mục",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PersonaCard(
    persona: Persona,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(persona.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(persona.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1)
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Check, "Đang chọn",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(persona.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2)
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Sửa",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Xoá",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaEditorDialog(
    title: String,
    initial: Persona?,
    defaultPath: String,
    onDismiss: () -> Unit,
    onSave: (Persona) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🤖") }
    var systemPrompt by remember { mutableStateOf(initial?.systemPrompt ?: "") }
    var voiceName by remember { mutableStateOf(initial?.voiceName ?: "") }
    var allowMusic by remember { mutableStateOf(initial?.allowMusic ?: false) }
    var path by remember {
        mutableStateOf(initial?.category ?: if (defaultPath.isBlank()) "Chung" else defaultPath)
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var promptError by remember { mutableStateOf<String?>(null) }
    var emojiPickerOpen by remember { mutableStateOf(false) }

    val emojiOptions = listOf(
        "🤖", "🧑‍🏫", "📐", "💻", "💙", "🇬🇧", "🇻🇳", "🧠", "🎨", "🎵",
        "📚", "🔬", "⚕️", "⚖️", "💰", "🍳", "✈️", "🏋️", "🎮", "🌸",
        "🎒", "🔢", "📝", "⚡", "🧪", "🧬", "📖", "📜", "🌍", "💾",
        "📊", "📈", "🎯", "📰", "🏛️", "🏗️", "🔌", "🔧", "🌾", "🐾",
        "🌿", "🏨", "🇨🇳", "🇯🇵", "🇰🇷"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Tên nhân vật *") },
                    placeholder = { Text("VD: Gia sư Hoá lớp 11") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Đường dẫn trong cây") },
                    placeholder = { Text("VD: Học tập/THCS/Lớp 6/Hoá học") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Dùng dấu \"/\" để chia cấp. VD: Học tập/THCS/Lớp 6/Toán",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { emojiPickerOpen = !emojiPickerOpen }) {
                        Text(if (emojiPickerOpen) "Đóng" else "Chọn")
                    }
                }
                if (emojiPickerOpen) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojiOptions) { e ->
                            Box(
                                modifier = Modifier.size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (e == emoji)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        emoji = e
                                        emojiPickerOpen = false
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text(e, fontSize = 22.sp) }
                        }
                    }
                }

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it; promptError = null },
                    label = { Text("System Prompt *") },
                    placeholder = {
                        Text("Bạn là chuyên gia... Bắt đầu bằng [emotion:xxx]...")
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 10,
                    isError = promptError != null,
                    supportingText = promptError?.let { { Text(it) } }
                )

                OutlinedTextField(
                    value = voiceName,
                    onValueChange = { voiceName = it },
                    label = { Text("Voice TTS (tuỳ chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cho phép phát nhạc",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Switch(
                        checked = allowMusic,
                        onCheckedChange = { allowMusic = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.trim()
                val trimmedPrompt = systemPrompt.trim()
                var valid = true
                if (trimmedName.isBlank()) {
                    nameError = "Bắt buộc"
                    valid = false
                }
                if (trimmedPrompt.isBlank()) {
                    promptError = "Bắt buộc"
                    valid = false
                }
                if (!valid) return@TextButton

                val finalPath = if (path.isBlank()) "Chung/$trimmedName" else path.trim()
                val result = (initial ?: Persona(
                    name = "", description = "", systemPrompt = ""
                )).copy(
                    name = trimmedName,
                    description = description.trim().ifBlank { "Nhân vật tự tạo" },
                    emoji = emoji.ifBlank { "🤖" }.take(4),
                    systemPrompt = trimmedPrompt,
                    voiceName = voiceName.trim(),
                    allowMusic = allowMusic,
                    category = finalPath,
                    builtIn = false
                )
                onSave(result)
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        }
    )
}
