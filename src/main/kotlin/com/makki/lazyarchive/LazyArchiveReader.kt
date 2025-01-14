package com.makki.lazyarchive

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

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

	/**
	 * Perform an extraction of files based on the [filter] argument and settings passed to [LazyArchiveReader].
	 * @return [LazyArchiveResult] which has two states.
	 * Both will contain a buffer of extracted files to the point of finish.
	 */
	fun extract(filter: Predicate): LazyArchiveResult {
		if (type == null) return LazyArchiveResult.Fail(
			IllegalStateException("Unsupported archive type: ${archive.name}"), emptyList(), emptyList()
		)
		val readContext = initContext<TemporaryFile>(filter)
		try {
			BufferedInputStream(FileInputStream(archive), bufferSize)
				.use { bufferedIS ->
					GenericArchiveReader.extractArchive(
						readContext,
						bufferedIS,
						parentArchiveName = archive.name,
						fullParentPath = "",
						currentDepth = 1
					)
				}
		} catch (e: Throwable) {
			return LazyArchiveResult.Fail(e, readContext.buffer, readContext.cleanUpFilesPostUse)
		}
		return LazyArchiveResult.Success(readContext.buffer, readContext.fullRead, readContext.cleanUpFilesPostUse)
	}

	fun peek(filter: Predicate): LazyArchiveView {
		if (type == null) return LazyArchiveView.Fail(
			IllegalStateException("Unsupported archive type: ${archive.name}"), emptyList()
		)
		val readContext = initContext<FileMeta>(filter)
		try {
			BufferedInputStream(FileInputStream(archive), bufferSize)
				.use { bufferedIS ->
					GenericArchiveReader.peekArchive(
						readContext,
						bufferedIS,
						parentArchiveName = archive.name,
						fullParentPath = "",
						currentDepth = 1
					)
				}
		} catch (e: Throwable) {
			return LazyArchiveView.Fail(e, readContext.buffer)
		}
		return LazyArchiveView.Success(readContext.buffer, readContext.fullRead)
	}

	private fun <T> initContext(filter: Predicate): ReadContext<T> {
		return ReadContext(
			filter, temporaryDirectory,
			archiveDepth = archiveDepth,
			loopBreaker = loopBreaker,
			errorHandler = errorHandler
		)
	}
}

class ReadContext<T>(
	val predicate: Predicate,
	val temporaryDirectory: File,
	val archiveDepth: Int,
	val loopBreaker: Int,
	val errorHandler: ErrorHandler?,
) {
	val buffer: ArrayList<T> = ArrayList()
	val cleanUpFilesPostUse: ArrayList<File> = ArrayList()
	var fullRead: Boolean = true
	var iteration: Int = 0
	fun nextExtractionDirectory() = File(temporaryDirectory, "extract_${buffer.size}")

	fun checkAndSetLoopBreak(): Boolean {
		if (iteration >= loopBreaker) {
			fullRead = false
			return true
		}
		return false
	}
}
