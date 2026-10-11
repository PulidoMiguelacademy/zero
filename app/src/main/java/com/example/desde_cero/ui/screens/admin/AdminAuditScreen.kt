package com.example.desde_cero.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desde_cero.model.CurrencyTransaction
import com.example.desde_cero.model.TransactionStatus
import com.example.desde_cero.model.TransactionType
import com.example.desde_cero.ui.screens.user.StatusBadge
import com.example.desde_cero.viewmodel.AuthViewModel
import com.example.desde_cero.viewmodel.CurrencyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditScreen(
    authViewModel: AuthViewModel,
    currencyViewModel: CurrencyViewModel,
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allTransactions by currencyViewModel.allTransactions.collectAsState()
    val currencies by currencyViewModel.currencies.collectAsState()
    val auditMessage by currencyViewModel.auditMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply { maximumFractionDigits = 0 } }
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 } }

    var filterStatus by remember { mutableStateOf<TransactionStatus?>(TransactionStatus.PENDIENTE_AUDITORIA) }
    var selectedTxForReject by remember { mutableStateOf<CurrencyTransaction?>(null) }
    var rejectObservationInput by remember { mutableStateOf("") }

    LaunchedEffect(auditMessage) {
        auditMessage?.let {
            snackbarHostState.showSnackbar(it)
            currencyViewModel.clearAuditMessage()
        }
    }

    val pendingCount = allTransactions.count { it.status == TransactionStatus.PENDIENTE_AUDITORIA }
    val successCount = allTransactions.count { it.status == TransactionStatus.EXITOSA }
    val rejectedCount = allTransactions.count { it.status == TransactionStatus.RECHAZADA }

    val displayedTransactions = remember(allTransactions, filterStatus) {
        if (filterStatus == null) allTransactions else allTransactions.filter { it.status == filterStatus }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panel de Auditoría",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Auditor: ${currentUser?.fullName ?: "Admin"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { currencyViewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Business Rule Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Regla de Auditoría de Transición",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "El auditor valida que la compra/venta sea exitosa si la variación del valor de la divisa al momento de la transición NO SUPERA EL 2.5% de la tasa inicial acordada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Counters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Pendientes", "$pendingCount", Color(0xFFE65100), Color(0xFFFFF3E0), Modifier.weight(1f))
                MetricCard("Aprobadas", "$successCount", Color(0xFF2E7D32), Color(0xFFE8F5E9), Modifier.weight(1f))
                MetricCard("Rechazadas", "$rejectedCount", Color(0xFFC62828), Color(0xFFFFEBEE), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Evaluator Demo Controls (Simulate Volatility)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Herramientas de Simulación de Transición:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Modifica la tasa de mercado para comprobar el cumplimiento de la regla del 2.5%:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Aplica variación válida (ej. +1.2%)
                                currencyViewModel.applyControlledVariation("USD", 1.2)
                                currencyViewModel.applyControlledVariation("EUR", 0.9)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tasa Válida (+1.2%)", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Aplica variación excesiva (ej. +3.8% > 2.5%)
                                currencyViewModel.applyControlledVariation("USD", 3.8)
                                currencyViewModel.applyControlledVariation("EUR", 4.2)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Excede 2.5% (+3.8%)", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val apiSyncState by currencyViewModel.apiSyncState.collectAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (apiSyncState.isLiveConnected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Token: ${currencyViewModel.maskedApiKey} • Finnhub Live API",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (apiSyncState.isLiveConnected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { currencyViewModel.fetchLiveApiRates() },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Sincronizar en Vivo", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStatus == TransactionStatus.PENDIENTE_AUDITORIA,
                    onClick = { filterStatus = TransactionStatus.PENDIENTE_AUDITORIA },
                    label = { Text("Pendientes ($pendingCount)", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = filterStatus == TransactionStatus.EXITOSA,
                    onClick = { filterStatus = TransactionStatus.EXITOSA },
                    label = { Text("Aprobadas ($successCount)", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = filterStatus == TransactionStatus.RECHAZADA,
                    onClick = { filterStatus = TransactionStatus.RECHAZADA },
                    label = { Text("Rechazadas ($rejectedCount)", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("Todas", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transactions list
            if (displayedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay transacciones en esta categoría.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                displayedTransactions.forEach { tx ->
                    val currentRate = currencies.firstOrNull { it.code == tx.currencyCode }?.baseRate ?: tx.rateAtOrder
                    val deviation = currencyViewModel.calculateDeviation(tx, currentRate)
                    val exceedsLimit = deviation > 2.5

                    AdminTransactionAuditCard(
                        tx = tx,
                        currentTransitionRate = currentRate,
                        deviationPercentage = deviation,
                        exceedsLimit = exceedsLimit,
                        currencyFormatter = currencyFormatter,
                        numberFormatter = numberFormatter,
                        onApprove = {
                            currentUser?.let { admin ->
                                currencyViewModel.auditTransaction(
                                    tx = tx,
                                    adminUser = admin,
                                    approve = true,
                                    simulatedRateAtTransition = currentRate
                                )
                            }
                        },
                        onReject = {
                            selectedTxForReject = tx
                            rejectObservationInput = if (exceedsLimit) {
                                "Rechazado en auditoría: la variación de $deviation% supera el límite permitido del 2.5%."
                            } else {
                                "Rechazado por decisión de auditoría."
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Modal de rechazo con nota de auditoría
    selectedTxForReject?.let { tx ->
        AlertDialog(
            onDismissRequest = { selectedTxForReject = null },
            title = { Text("Rechazar Transacción ${tx.id}") },
            text = {
                Column {
                    Text("Indica la observación de auditoría para el rechazo:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectObservationInput,
                        onValueChange = { rejectObservationInput = it },
                        label = { Text("Motivo del rechazo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { admin ->
                            currencyViewModel.auditTransaction(
                                tx = tx,
                                adminUser = admin,
                                approve = false,
                                observation = rejectObservationInput
                            )
                        }
                        selectedTxForReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForReject = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 20.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun AdminTransactionAuditCard(
    tx: CurrencyTransaction,
    currentTransitionRate: Double,
    deviationPercentage: Double,
    exceedsLimit: Boolean,
    currencyFormatter: NumberFormat,
    numberFormatter: NumberFormat,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isPending = tx.status == TransactionStatus.PENDIENTE_AUDITORIA
    val isCompra = tx.type == TransactionType.COMPRA

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending && exceedsLimit) Color(0xFFFFF7ED)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${tx.type.name} • ${tx.currencyCode}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCompra) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = "Cliente: ${tx.userFullName} • ID: ${tx.id.takeLast(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(tx.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Montos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monto: ${numberFormatter.format(tx.amountForeign)} ${tx.currencyCode}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = currencyFormatter.format(tx.totalBaseAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tasas y Variación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tasa inicial (Orden):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(tx.rateAtOrder),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tasa al momento de transición:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(if (isPending) currentTransitionRate else tx.rateAtTransition),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de la Regla del 2.5%
                    val activeDeviation = if (isPending) deviationPercentage else tx.deviationPercentage
                    val activeExceeds = activeDeviation > 2.5

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeExceeds) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeExceeds) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (activeExceeds) Color(0xFFC62828) else Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeExceeds) {
                                    "Variación: $activeDeviation% > 2.5% (SUPERA LÍMITE PERMITIDO)"
                                } else {
                                    "Variación: $activeDeviation% <= 2.5% (DENTRO DEL MARGEN PERMITIDO)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeExceeds) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // Si está pendiente: Botones de decisión para el Auditor
            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        enabled = !exceedsLimit, // Bloqueado si supera el 2.5% cumpliendo la regla de auditoría
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aprobar Exitosa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (exceedsLimit) {
                    Text(
                        text = "* La aprobación está inhabilitada porque la variación supera el límite reglamentario del 2.5%. Debes rechazar la operación.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auditado por ${tx.auditedBy ?: "Admin"} el ${if (tx.auditedAt != null) dateFormat.format(Date(tx.auditedAt)) else ""}: ${tx.auditObservation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
