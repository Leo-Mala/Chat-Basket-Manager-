package com.example.utils

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Bridges the validated native save importer/exporter with Android document streams.
 *
 * The active slot is changed only after the incoming document has been copied locally.
 * Failed imports restore the previously active slot, while a successful cross-slot import
 * intentionally leaves the imported slot active so the ViewModel can reload that career.
 */
object SaveTransferManager {
    fun exportActiveSlot(context: Context, output: OutputStream): Boolean = runCatching {
        val path = DataExporter.exportCurrentGame(context) ?: return false
        File(path).inputStream().use { input -> input.copyTo(output) }
        output.flush()
        true
    }.getOrDefault(false)

    fun importIntoSlot(context: Context, input: InputStream, targetSlot: Int): Boolean {
        if (targetSlot !in 1..SaveSlotManager.MAX_SLOTS) return false

        val appContext = context.applicationContext
        val previousSlot = SaveSlotManager.getActiveSlot(appContext)
        val staging = runCatching {
            File.createTempFile("basket-manager-import-", ".json", appContext.cacheDir)
        }.getOrNull() ?: return false

        return try {
            staging.outputStream().use { output -> input.copyTo(output) }
            SaveSlotManager.setActiveSlot(appContext, targetSlot)
            val imported = DataExporter.importGame(appContext, staging.absolutePath)
            if (!imported) SaveSlotManager.setActiveSlot(appContext, previousSlot)
            imported
        } catch (e: Exception) {
            e.printStackTrace()
            SaveSlotManager.setActiveSlot(appContext, previousSlot)
            false
        } finally {
            staging.delete()
        }
    }
}
