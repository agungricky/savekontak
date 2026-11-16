package com.example.wacontactcollector

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WAContactCollectorApp(viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WAContactCollectorApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val total by viewModel.total.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("dashboard") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { MainTopBar(screen = screen, onBack = { screen = "dashboard" }, onSettings = { screen = "settings" }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (screen == "dashboard") {
            DashboardScreen(
                modifier = Modifier.padding(padding),
                total = total,
                customers = customers,
                history = history,
                isSyncing = isSyncing,
                onSync = viewModel::syncNow,
                onExport = { exportCsv(context = context, uri = it, customers = customers) },
                onDelete = viewModel::deleteAll,
                onOpenWhatsApp = { openWhatsApp(context, it, viewModel) }
            )
        } else {
            SettingsScreen(
                modifier = Modifier.padding(padding),
                initial = settings,
                onSave = viewModel::saveSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(screen: String, onBack: () -> Unit, onSettings: () -> Unit) {
    TopAppBar(
        title = { Text(if (screen == "dashboard") "WA Contact Collector" else "Pengaturan") },
        navigationIcon = {
            if (screen == "settings") {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
            }
        },
        actions = {
            if (screen == "dashboard") {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                }
            }
        }
    )
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    total: Int,
    customers: List<com.example.wacontactcollector.data.CustomerEntity>,
    history: List<com.example.wacontactcollector.data.SyncHistoryEntity>,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onDelete: () -> Unit,
    onOpenWhatsApp: (Boolean) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExport) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Kontak pelanggan dari sumber resmi", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Data dinormalisasi, dideduplikasi, lalu disimpan lokal di perangkat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total nomor", total.toString(), Modifier.weight(1f))
                StatCard("Baru terakhir", history.firstOrNull()?.newCount?.toString() ?: "0", Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Sinkronisasi sumber resmi", fontWeight = FontWeight.Bold)
                            Text("CRM atau WhatsApp Business Platform API")
                        }
                    }
                    Button(onClick = onSync, enabled = !isSyncing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSyncing) "Menyinkronkan…" else "Sinkronkan sekarang")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onOpenWhatsApp(false) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Message, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("WhatsApp")
                }
                OutlinedButton(onClick = { onOpenWhatsApp(true) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Business, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Business")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { createDocument.launch("wa-contacts.csv") }, modifier = Modifier.weight(1f), enabled = customers.isNotEmpty()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Ekspor CSV")
                }
                OutlinedButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.weight(1f), enabled = customers.isNotEmpty()) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Hapus data")
                }
            }
        }
        item { SectionTitle(Icons.Default.History, "Riwayat sinkronisasi") }
        if (history.isEmpty()) {
            item { EmptyText("Belum ada riwayat sinkronisasi.") }
        } else {
            items(history, key = { it.id }) { item -> SyncHistoryRow(item) }
        }
        item { SectionTitle(Icons.Default.AddCircleOutline, "Nomor terbaru") }
        if (customers.isEmpty()) {
            item { EmptyText("Belum ada nomor. Konfigurasikan endpoint resmi di Pengaturan.") }
        } else {
            items(customers.take(10), key = { it.phone }) { customer -> CustomerRow(customer.phone, customer.displayName, customer.isInDeviceContacts) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus semua data lokal?") },
            text = { Text("Nomor pelanggan dan riwayat sinkronisasi akan dihapus dari perangkat ini.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirmation = false }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    initial: com.example.wacontactcollector.data.AppSettings,
    onSave: (com.example.wacontactcollector.data.AppSettings) -> Unit
) {
    val context = LocalContext.current
    var endpoint by remember(initial.endpoint) { mutableStateOf(initial.endpoint) }
    var token by remember(initial.token) { mutableStateOf(initial.token) }
    var enabled by remember(initial.syncEnabled) { mutableStateOf(initial.syncEnabled) }
    var checkContacts by remember(initial.checkDeviceContacts) { mutableStateOf(initial.checkDeviceContacts) }
    var interval by remember(initial.intervalHours) { mutableStateOf(initial.intervalHours) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "Izin kontak tidak diberikan", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Sumber data resmi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Masukkan endpoint API CRM/WhatsApp Business Platform yang Anda miliki aksesnya. Aplikasi tidak membaca daftar chat WhatsApp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint HTTPS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(token, { token = it }, label = { Text("Bearer token (opsional)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        }
        item {
            SettingToggle("Sinkronisasi berkala", "Berjalan di background sesuai batasan Android", enabled) { enabled = it }
        }
        item {
            Text("Interval: setiap $interval jam", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 6, 12, 24).forEach { hours ->
                    OutlinedButton(onClick = { interval = hours }) { Text("${hours}j") }
                }
            }
        }
        item {
            SettingToggle("Cek kontak HP", "Memerlukan izin READ_CONTACTS dan hanya membaca kecocokan nomor", checkContacts) {
                checkContacts = it
                if (it && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            }
            if (checkContacts && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                    Text("Berikan izin READ_CONTACTS")
                }
            }
        }
        item {
            Button(onClick = { onSave(com.example.wacontactcollector.data.AppSettings(endpoint, token, enabled, interval, checkContacts)) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp)); Text("Simpan pengaturan")
            }
        }
        item {
            Divider()
            Text("Privasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Nomor dinormalisasi dan dideduplikasi di perangkat. Token disimpan di DataStore privat aplikasi. Gunakan HTTPS dan pastikan dasar hukum/persetujuan pelanggan sesuai kebijakan Anda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
}

@Composable
private fun EmptyText(text: String) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }

@Composable
private fun SyncHistoryRow(item: com.example.wacontactcollector.data.SyncHistoryEntity) {
    val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.finishedAt))
    Card { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (item.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Refresh, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(item.status); Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("+${item.newCount}", fontWeight = FontWeight.Bold)
    } }
}

@Composable
private fun CustomerRow(phone: String, name: String?, inContacts: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name ?: "Tanpa nama"); Text(phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (inContacts) Text("Sudah di kontak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

private fun openWhatsApp(context: android.content.Context, business: Boolean, viewModel: MainViewModel) {
    try {
        context.startActivity(viewModel.launchWhatsApp(business).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.whatsapp.com/download")))
    }
}

private fun exportCsv(
    context: android.content.Context,
    uri: Uri,
    customers: List<com.example.wacontactcollector.data.CustomerEntity>
) {
    try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
            writer ?: return
            writer.appendLine("phone,name,source,collected_at,is_in_device_contacts")
            customers.forEach { customer ->
                writer.appendLine(
                    listOf(
                        customer.phone,
                        customer.displayName.orEmpty(),
                        customer.source,
                        customer.collectedAt.toString(),
                        customer.isInDeviceContacts.toString()
                    ).joinToString(",", transform = ::csvCell)
                )
            }
        }
        Toast.makeText(context, "CSV berhasil diekspor", Toast.LENGTH_SHORT).show()
    } catch (error: Exception) {
        Toast.makeText(context, "Ekspor gagal: ${error.message}", Toast.LENGTH_LONG).show()
    }
}

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
