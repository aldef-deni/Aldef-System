package com.aldef.system.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.aldef.system.ui.theme.*
import java.io.File
import java.security.MessageDigest

data class HiddenFile(
    val name: String,
    val originalPath: String,
    val isLocked: Boolean,
    val size: String,
    val dateAdded: String
)

@Composable
fun HiddenFilesScreen(navController: NavController) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(mutableListOf<HiddenFile>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf<HiddenFile?>(null) }
    var showUnlockedFiles by remember { mutableStateOf(false) }

    val hiddenDir = remember {
        File(context.filesDir, "hidden").also { it.mkdirs() }
    }

    LaunchedEffect(Unit) {
        files = loadHiddenFiles(hiddenDir)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("File Tersembunyi", fontWeight = FontWeight.Bold, color = PremiumGold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = PremiumGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add", tint = PremiumGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PremiumGold,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add file")
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PremiumGold.copy(alpha = 0.1f),
                                DarkCard
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${files.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold
                        )
                        Text("Total File", fontSize = 12.sp, color = TextGray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${files.count { it.isLocked }}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumRed
                        )
                        Text("Terkunci", fontSize = 12.sp, color = TextGray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${files.count { !it.isLocked }}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGreen
                        )
                        Text("Terbuka", fontSize = 12.sp, color = TextGray)
                    }
                }
            }

            if (files.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.FolderOff,
                        contentDescription = null,
                        tint = PremiumGold.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Belum ada file tersembunyi",
                        fontSize = 16.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + untuk menambah file",
                        fontSize = 13.sp,
                        color = TextGrayDark
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { file ->
                        HiddenFileItem(
                            file = file,
                            onLockToggle = { toggledFile ->
                                files = files.map {
                                    if (it.originalPath == toggledFile.originalPath) {
                                        it.copy(isLocked = !it.isLocked)
                                    } else it
                                }.toMutableList()
                            },
                            onDelete = { deletedFile ->
                                files = files.filter { it.originalPath != deletedFile.originalPath }.toMutableList()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add file dialog
    if (showAddDialog) {
        AddFileDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { file ->
                files = (files + file).toMutableList()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun HiddenFileItem(
    file: HiddenFile,
    onLockToggle: (HiddenFile) -> Unit,
    onDelete: (HiddenFile) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // File icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (file.isLocked) {
                                listOf(PremiumRed.copy(alpha = 0.2f), PremiumRed.copy(alpha = 0.05f))
                            } else {
                                listOf(PremiumGreen.copy(alpha = 0.2f), PremiumGreen.copy(alpha = 0.05f))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (file.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = if (file.isLocked) PremiumRed else PremiumGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = "${file.size} • ${file.dateAdded}",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            // Lock toggle
            IconButton(onClick = { onLockToggle(file) }) {
                Icon(
                    if (file.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = "Toggle lock",
                    tint = if (file.isLocked) PremiumRed else PremiumGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete
            IconButton(onClick = { onDelete(file) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = PremiumRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddFileDialog(
    onDismiss: () -> Unit,
    onAdd: (HiddenFile) -> Unit
) {
    var fileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = PremiumGold,
        textContentColor = TextWhite,
        title = {
            Text("Tambah File", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Nama File", color = TextGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = TextGrayDark,
                        cursorColor = PremiumGold,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onAdd(
                            HiddenFile(
                                name = fileName,
                                originalPath = "",
                                isLocked = true,
                                size = "0 KB",
                                dateAdded = "Baru saja"
                            )
                        )
                        fileName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tambah", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextGray)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private fun loadHiddenFiles(hiddenDir: File): MutableList<HiddenFile> {
    val files = mutableListOf<HiddenFile>()
    hiddenDir.listFiles()?.forEach { file ->
        files.add(
            HiddenFile(
                name = file.name,
                originalPath = file.absolutePath,
                isLocked = file.extension == "locked",
                size = formatFileSize(file.length()),
                dateAdded = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(file.lastModified()))
            )
        )
    }
    return files
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
