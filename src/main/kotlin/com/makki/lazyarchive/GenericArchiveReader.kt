package com.makki.lazyarchive

import java.io.InputStream

object GenericArchiveReader {
	fun extractArchive(
		readContext: ReadContext<TemporaryFile>,
		inputStream: InputStream,
		parentArchiveName: String,
		fullParentPath: String,
		currentDepth: Int,
	) {
		if (readContext.checkAndSetLoopBreak()) return

		val extension = parentArchiveName.substringAfterLast(".")
		val type = ArchiveType.fromExtension(extension)
		when (type) {
			ArchiveType.ZIP -> ZipArchiveReader.extractZip(
				readContext,
				inputStream,
				parentArchiveName = parentArchiveName,
				fullParentPath = fullParentPath,
				currentDepth = currentDepth
			)

			ArchiveType.SEVEN_ZIP -> TODO()
			else -> throw IllegalStateException("Unknown archive type: $parentArchiveName")
		}
	}

	fun peekArchive(
		readContext: ReadContext<FileMeta>,
		inputStream: InputStream,
		parentArchiveName: String,
		fullParentPath: String,
		currentDepth: Int,
	) {
		if (readContext.checkAndSetLoopBreak()) return

		val extension = parentArchiveName.substringAfterLast(".")
		val type = ArchiveType.fromExtension(extension)
		when (type) {
			ArchiveType.ZIP -> ZipArchiveReader.peekZip(
				readContext,
				inputStream,
				parentArchiveName = parentArchiveName,
				fullParentPath = fullParentPath,
				currentDepth = currentDepth
			)

			ArchiveType.SEVEN_ZIP -> TODO()
			else -> throw IllegalStateException("Unknown archive type: $parentArchiveName")
		}
	}
}
