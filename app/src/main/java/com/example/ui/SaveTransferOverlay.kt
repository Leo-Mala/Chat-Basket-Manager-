package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.GameViewModel
import com.example.domain.rules.SavedGameLoadState
import com.example.utils.SaveSlotManager
import com.example.utils.SaveSlotSummary
import com.example.utils.SaveTransferManager
import com.example.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-facing save transfer entry point. Android's Storage Access Framework owns the
 * document permissions, so the app never requests broad storage access.
 */
@Composable
fun SaveTransferOverlay(viewModel: GameViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val seasonProgress by viewModel.seasonSimulationProgressFlow.collectAsStateWithLifecycle()

    var showTransferDialog by remember { mutableStateOf(false) }
    var showSlotPicker by remember { mutableStateOf(false) }
    var overwriteSlot by remember { mutableStateOf<SaveSlotSummary?>(null) }
    var pendingImportSlot by remember { mutableStateOf<Int?>(null) }
    var slots by remember { mutableStateOf(SaveSlotManager.getSlots(context)) }

    val transferBlocked = seasonProgress != null || viewModel.savedGameLoadState == SavedGameLoadState.LOADING
    val activeSlot = SaveSlotManager.getActiveSlot(context)
    val canExport = viewModel.managedTeam != null && viewModel.season != null && !transferBlocked

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val exported = withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    SaveTransferManager.exportActiveSlot(context, output)
                } ?: false
            }
            ToastUtils.showToast(
                context,
                if (exported) "Save exportado com sucesso." else "Não foi possível exportar o save."
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetSlot = pendingImportSlot
        pendingImportSlot = null
        if (uri == null || targetSlot == null) return@rememberLauncherForActivityResult

        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    SaveTransferManager.importIntoSlot(context, input, targetSlot)
                } ?: false
            }
            if (imported) {
                slots = SaveSlotManager.getSlots(context)
                viewModel.retryLoadSavedGame()
                showTransferDialog = false
                ToastUtils.showToast(context, "Save importado no slot $targetSlot.")
            } else {
                ToastUtils.showToast(context, "Importação rejeitada. O save existente foi preservado.")
            }
        }
    }

    fun launchExport() {
        if (!canExport) return
        scope.launch {
            viewModel.saveGame()?.join()
            val current = SaveSlotManager.getActiveSlot(context)
            val team = viewModel.managedTeam?.name
                ?.replace("[^A-Za-z0-9_-]".toRegex(), "_")
                ?.take(32)
                ?.ifBlank { "career" }
                ?: "career"
            exportLauncher.launch("basket-manager-slot-$current-$team.json")
        }
    }

    fun launchImport(slotId: Int) {
        if (transferBlocked) return
        scope.launch {
            // Match the existing slot-switch discipline: finish the current save before
            // changing which physical Room database is active.
            viewModel.saveGame()?.join()
            pendingImportSlot = slotId
            importLauncher.launch(arrayOf("application/json", "text/plain"))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SmallFloatingActionButton(
            onClick = {
                slots = SaveSlotManager.getSlots(context)
                showTransferDialog = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Importar ou exportar save")
        }
    }

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("SAVE / IMPORTAÇÃO") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Slot ativo: $activeSlot")
                    if (transferBlocked) {
                        Text("A transferência fica bloqueada durante carregamento ou simulação de temporada.")
                    }
                    Button(
                        onClick = ::launchExport,
                        enabled = canExport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Text("  EXPORTAR SAVE ATUAL")
                    }
                    OutlinedButton(
                        onClick = {
                            slots = SaveSlotManager.getSlots(context)
                            showSlotPicker = true
                        },
                        enabled = !transferBlocked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text("  IMPORTAR SAVE")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransferDialog = false }) { Text("FECHAR") }
            }
        )
    }

    if (showSlotPicker) {
        AlertDialog(
            onDismissRequest = { showSlotPicker = false },
            title = { Text("IMPORTAR EM QUAL SLOT?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { slot ->
                        OutlinedButton(
                            onClick = {
                                showSlotPicker = false
                                if (slot.occupied) overwriteSlot = slot else launchImport(slot.slotId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("SLOT ${slot.slotId}${if (slot.slotId == activeSlot) " • ATIVO" else ""}")
                                Text(if (slot.occupied) slot.teamName ?: "Carreira salva" else "Vazio")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSlotPicker = false }) { Text("CANCELAR") }
            }
        )
    }

    overwriteSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { overwriteSlot = null },
            title = { Text("SUBSTITUIR SLOT ${slot.slotId}?") },
            text = {
                Text(
                    "O slot contém ${slot.teamName ?: "uma carreira salva"}. " +
                        "Ele só será substituído se o arquivo importado passar por todas as validações."
                )
            },
            confirmButton = {
                Button(onClick = {
                    overwriteSlot = null
                    launchImport(slot.slotId)
                }) { Text("CONTINUAR") }
            },
            dismissButton = {
                TextButton(onClick = { overwriteSlot = null }) { Text("CANCELAR") }
            }
        )
    }
}
