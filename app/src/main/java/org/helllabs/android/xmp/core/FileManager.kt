package org.helllabs.android.xmp.core

import android.content.Context
import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.OutputStream

/**
 * Handles file operations, abstracting away Context from ViewModels
 */
class FileManager(private val context: Context) {

    /**
     * Create a file for module download
     */
    fun createModuleFile(
        parentDir: DocumentFileCompat,
        filename: String
    ): Result<DocumentFileCompat> = runCatching {
        parentDir.createFile("application/octet-stream", filename)
            ?: throw XmpException("Failed to create file: $filename")
    }

    /**
     * Open an output stream for a file
     */
    fun openOutputStream(file: DocumentFileCompat): Result<OutputStream> = runCatching {
        context.contentResolver.openOutputStream(file.uri)
            ?: throw XmpException("Failed to open output stream for ${file.name}")
    }

    /**
     * Write data to a file using a lambda
     */
    suspend fun writeToFile(
        file: DocumentFileCompat,
        writer: suspend (OutputStream) -> Unit
    ): Result<Unit> = runCatching {
        openOutputStream(file).getOrThrow().use { outputStream ->
            writer(outputStream)
        }
    }

    /**
     * Get URI from DocumentFileCompat
     */
    fun getUri(file: DocumentFileCompat): Uri = file.uri
}
