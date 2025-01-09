package com.makki.lazyarchive

import java.io.InputStream

object GeneralArchiveReader {
	fun extractArchive(
		readContext: ReadContext,
		inputStream: InputStream,
		parentArchiveName: String,
		fullParentPath: String,
		currentDepth: Int,
	) {
		if (readContext.iteration > readContext.loopBreaker) {
			readContext.fullRead = false
			return
		}

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
}