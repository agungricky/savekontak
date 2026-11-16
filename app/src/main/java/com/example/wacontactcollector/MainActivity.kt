package com.example.wacontactcollector

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wacontactcollector.data.CaptureHistoryEntity
import com.example.wacontactcollector.data.CustomerEntity
import com.example.wacontactcollector.data.ScanStatus
import com.example.wacontactcollector.data.isWhatsAppAccessibilityEnabled
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
    val monitoring by viewModel.monitoring.collectAsStateWithLifecycle()
    val pendingPhone by viewModel.pendingPhone.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val serviceEnabled = rememberAccessibilityEnabled(context)
    val snackbar = remember { SnackbarHostState() }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var contactsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { exportCsv(context, it, customers) } }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> contactsGranted = granted }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WA Contact Collector") },
                navigationIcon = {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan layanan")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(6.dp)) }
            item {
                Text("Pemantauan lokal yang transparan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Nomor hanya ditawarkan untuk disimpan setelah Anda menyetujuinya. Tidak ada server, login, atau sinkronisasi API.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total kontak", total.toString(), Modifier.weight(1f))
                    StatCard("Kontak baru", history.firstOrNull()?.newCount?.toString() ?: "0", Modifier.weight(1f))
                }
            }
            item {
                MonitoringCard(
                    serviceEnabled = serviceEnabled,
                    monitoring = monitoring,
                    scanStatus = scanStatus,
                    onStart = {
                        viewModel.startMonitoring()
                        if (!serviceEnabled) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onStop = viewModel::stopMonitoring
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { openWhatsApp(context, false, viewModel) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Message, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("WhatsApp")
                    }
                    OutlinedButton(onClick = { openWhatsApp(context, true, viewModel) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Business, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Business")
                    }
                }
            }
            if (pendingPhone != null) {
                item {
                    PendingPhoneCard(
                        phone = pendingPhone!!.normalizedPhone,
                        onSave = viewModel::approvePendingPhone,
                        onDismiss = viewModel::dismissPendingPhone
                    )
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pengecekan kontak HP", fontWeight = FontWeight.Bold)
                        Text(
                            if (contactsGranted) "READ_CONTACTS aktif. Nomor akan ditandai jika sudah ada di kontak perangkat."
                            else "Opsional. Izin hanya digunakan untuk memeriksa kecocokan nomor yang Anda setujui.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!contactsGranted) {
                            OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                                Text("Izinkan READ_CONTACTS")
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { createDocument.launch("wa-contacts.csv") }, enabled = customers.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Ekspor CSV")
                    }
                    OutlinedButton(onClick = { showDeleteConfirmation = true }, enabled = customers.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Hapus data")
                    }
                }
            }
            item { SectionTitle(Icons.Default.History, "Riwayat pencatatan") }
            if (history.isEmpty()) item { EmptyText("Belum ada pencatatan.") }
            else items(history, key = { it.id }) { CaptureHistoryRow(it) }
            item { SectionTitle(Icons.Default.AddCircleOutline, "Kontak terbaru") }
            if (customers.isEmpty()) item { EmptyText("Belum ada kontak yang disetujui.") }
            else items(customers.take(10), key = { it.phone }) { CustomerRow(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus semua data lokal?") },
            text = { Text("Kontak dan riwayat pencatatan akan dihapus dari perangkat ini.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); showDeleteConfirmation = false }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun MonitoringCard(
    serviceEnabled: Boolean,
    monitoring: Boolean,
    scanStatus: ScanStatus,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val status = when {
        !serviceEnabled -> "Layanan Accessibility belum diaktifkan di Pengaturan Android."
        monitoring -> "Pantauan aktif untuk WhatsApp dan WhatsApp Business."
        else -> "Layanan tersedia, tetapi pantauan sedang dihentikan."
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (monitoring) Icons.Default.CheckCircle else Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Status pemantauan", fontWeight = FontWeight.Bold)
                    Text(status)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = !monitoring, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Mulai Pantau")
                }
                OutlinedButton(onClick = onStop, enabled = monitoring, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Hentikan Pantauan")
                }
            }
            Text(
                "Tidak ada scrolling otomatis dan tidak ada pembacaan isi pesan, media, password, atau daftar chat massal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (!monitoring) {
                    "Scanning berhenti. Tekan Mulai Pantau untuk memulai pemantauan manual."
                } else if (!serviceEnabled) {
                    "Scanning menunggu AccessibilityService diaktifkan."
                } else {
                    "Scanning: ${scanStatus.scrollEventCount} event scroll diproses" +
                        (scanStatus.lastMonitoringAt?.let { " • terakhir ${formatTime(it)}" } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PendingPhoneCard(phone: String, onSave: () -> Unit, onDismiss: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nomor terdeteksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(phone, style = MaterialTheme.typography.headlineSmall)
            Text("Simpan hanya jika nomor ini memang disetujui untuk dicatat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Simpan nomor") }
                TextButton(onClick = onDismiss) { Text("Abaikan") }
            }
        }
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
private fun CaptureHistoryRow(item: CaptureHistoryEntity) {
    val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.finishedAt))
    Card { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (item.status == "SAVED") Icons.Default.CheckCircle else Icons.Default.History, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(item.status); Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("+${item.newCount}", fontWeight = FontWeight.Bold)
    } }
}

@Composable
private fun CustomerRow(customer: CustomerEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(customer.phone)
            Text(if (customer.isInDeviceContacts) "Sudah di kontak HP" else "Disetujui dan tersimpan lokal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (customer.isInDeviceContacts) Text("Di kontak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun rememberAccessibilityEnabled(context: Context): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(isWhatsAppAccessibilityEnabled(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = isWhatsAppAccessibilityEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

private fun openWhatsApp(context: Context, business: Boolean, viewModel: MainViewModel) {
    try {
        context.startActivity(viewModel.launchWhatsApp(business).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.whatsapp.com/download")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun exportCsv(context: Context, uri: Uri, customers: List<CustomerEntity>) {
    try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
            writer ?: return
            writer.appendLine("phone,source,collected_at,is_in_device_contacts")
            customers.forEach { customer ->
                writer.appendLine(listOf(customer.phone, customer.source, customer.collectedAt.toString(), customer.isInDeviceContacts.toString()).joinToString(",", transform = ::csvCell))
            }
        }
        Toast.makeText(context, "CSV berhasil diekspor", Toast.LENGTH_SHORT).show()
    } catch (error: Exception) {
        Toast.makeText(context, "Ekspor gagal: ${error.message}", Toast.LENGTH_LONG).show()
    }
}

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value

private fun formatTime(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))
