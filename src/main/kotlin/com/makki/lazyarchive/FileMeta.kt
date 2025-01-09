package com.makki.lazyarchive

/**
 * Describes the file roughly, without peeking into the content its
 */
data class FileMeta(
	val fileName: String,
	val parentArchiveName: String,
	val pathInArchive: String,
	val fullPath: String,
	val fileSize: Long,
	val compressedSize: Long,
	val fileIndex: Int,
)
