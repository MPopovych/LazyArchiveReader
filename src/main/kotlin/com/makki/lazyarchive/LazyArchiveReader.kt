package com.makki.lazyarchive

import java.io.*

typealias Predicate = (header: FileMeta) -> Boolean
typealias ErrorHandler = (IOException) -> Unit

/**
 * Create this reader to configure
 * @param archive - which zip/7z to extract. For supported files check [ArchiveType]
 * @param temporaryDirectory - where to unpack the final or intermediate results
 * (intermediate files might be used for 7z)
 * @param archiveDepth - the depth allowed for nested archives
 * @param bufferSize - size in bytes of read stream buffer
 * @param loopBreaker - index of file at which the loop will interrupt,
 * used to ensure there are no nested loops or references
 */
class LazyArchiveReader(
	val archive: File,
	val temporaryDirectory: File,
	val archiveDepth: Int = 1,
	val bufferSize: Int = 16384,
	val loopBreaker: Int = 10000,
	val errorHandler: ErrorHandler? = null,
) {
	val type = ArchiveType.fromExtension(archive.extension)

	init {
		require(temporaryDirectory.isDirectory)
	}

	fun extract(filter: Predicate): LazyArchiveResult {
		if (type == null) return LazyArchiveResult.Fail(
			IllegalStateException("Unsupported archive type: ${archive.name}"), emptyList()
		)

		val readContext = ReadContext(
			filter, temporaryDirectory,
			archiveDepth = archiveDepth,
			loopBreaker = loopBreaker,
			errorHandler = errorHandler
		)

		try {
			BufferedInputStream(FileInputStream(archive), bufferSize).use { bufferedIS ->
				GeneralArchiveReader.extractArchive(
					readContext,
					bufferedIS,
					parentArchiveName = archive.name,
					fullParentPath = "",
					currentDepth = 1
				)
			}
		} catch (e: Throwable) {
			return LazyArchiveResult.Fail(e, readContext.buffer)
		}
		return LazyArchiveResult.Success(readContext.buffer, readContext.fullRead)
	}
}

class ReadContext(
	val predicate: Predicate,
	val temporaryDirectory: File,
	val archiveDepth: Int,
	val loopBreaker: Int,
	val errorHandler: ErrorHandler?,
) {
	val buffer: ArrayList<TemporaryFile> = ArrayList()
	var fullRead: Boolean = true
	var iteration: Int = 0
	fun nextExtractionDirectory() = File(temporaryDirectory, "extract_${buffer.size}")
}

sealed interface LazyArchiveResult : Closeable {
	fun successOrThrow(): Success {
		return when (this) {
			is Fail -> throw this.error.also { this.close() }
			is Success -> this
		}
	}

	/**
	 * Fail might occur at any point, due to this the extracted files are provided too.
	 * Clean up them to avoid leaking them into storage.
	 */
	class Fail(
		val error: Throwable,
		/**
		 * All extracted files up to this point
		 */
		val extracted: List<TemporaryFile>,
	) : LazyArchiveResult {
		override fun close() {
			extracted.forEach {
				it.file.deleteRecursively()
				it.folderForDeletion?.deleteRecursively()
			}
		}
	}

	class Success(
		/**
		 * Extracted files located in the temporary folder, each in its own subdirectory.
		 */
		val extracted: List<TemporaryFile>,
		/**
		 * Marks the result as partial due to reaching the loopBreak value
		 */
		val fullRead: Boolean,
	) : LazyArchiveResult {
		override fun close() {
			extracted.forEach {
				it.file.deleteRecursively()
				it.folderForDeletion?.deleteRecursively()
			}
		}
	}
}
