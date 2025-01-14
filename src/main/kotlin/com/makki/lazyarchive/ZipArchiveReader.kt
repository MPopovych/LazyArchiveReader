package com.makki.lazyarchive

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.*
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * Zip specific code to iterate through the ZipEntry-ies
 * Performs file extraction from nested archives without any temporary files.
 */
object ZipArchiveReader {
	fun extractZip(
		readContext: ReadContext<TemporaryFile>,
		inputStream: InputStream,
		parentArchiveName: String,
		fullParentPath: String,
		currentDepth: Int,
	) {
		val zipStream = ZipArchiveInputStream(inputStream)
		var zipEntry = zipStream.tryNextEntry(errorHandler = readContext.errorHandler)

		while (zipEntry != null) {
			val path = Path(zipEntry.name)
			val archiveType = ArchiveType.fromExtension(path.extension)

			val header = FileMeta(
				fileName = path.name,
				parentArchiveName = parentArchiveName,
				pathInArchive = path.pathString,
				fullPath = fullParentPath + "/" + zipEntry.name,
				fileSize = zipEntry.size,
				compressedSize = zipEntry.compressedSize,
				fileIndex = readContext.iteration++
			)

			var extract: File? = null
			if (readContext.predicate(header)) {
				extract = readContext.copyFileToTemp(zipStream, header.fileName)
				readContext.buffer.add(TemporaryFile(extract, header))
				readContext.cleanUpFilesPostUse.add(extract.parentFile) // temporary folders that need cleaning up
			}

			if (archiveType != null && currentDepth < readContext.archiveDepth) {
				val nextStream = if (extract != null) FileInputStream(extract) else zipStream

				// might be zipped, might be 7z - use general
				GenericArchiveReader.extractArchive(
					readContext,
					nextStream,
					parentArchiveName = header.fileName,
					fullParentPath = header.fullPath,
					currentDepth = currentDepth + 1
				)
			}

			if (readContext.checkAndSetLoopBreak()) return
			zipEntry = zipStream.tryNextEntry(errorHandler = readContext.errorHandler)
		}
	}

	fun peekZip(
		readContext: ReadContext<FileMeta>,
		inputStream: InputStream,
		parentArchiveName: String,
		fullParentPath: String,
		currentDepth: Int,
	) {
		val zipStream = ZipArchiveInputStream(inputStream)
		var zipEntry = zipStream.tryNextEntry(errorHandler = readContext.errorHandler)

		while (zipEntry != null) {
			val path = Path(zipEntry.name)
			val archiveType = ArchiveType.fromExtension(path.extension)

			val header = FileMeta(
				fileName = path.name,
				parentArchiveName = parentArchiveName,
				pathInArchive = path.pathString,
				fullPath = fullParentPath + "/" + zipEntry.name,
				fileSize = zipEntry.size,
				compressedSize = zipEntry.compressedSize,
				fileIndex = readContext.iteration++
			)
			readContext.buffer.add(header)

			val shouldExtract = archiveType != null
					&& currentDepth < readContext.archiveDepth
					&& readContext.predicate(header)

			if (shouldExtract) {
				val extract = readContext.copyFileToTemp(zipStream, header.fileName)

				// might be zipped, might be 7z - use general
				GenericArchiveReader.peekArchive(
					readContext,
					FileInputStream(extract),
					parentArchiveName = header.fileName,
					fullParentPath = header.fullPath,
					currentDepth = currentDepth + 1
				)
			}

			if (readContext.checkAndSetLoopBreak()) return
			zipEntry = zipStream.tryNextEntry(errorHandler = readContext.errorHandler)
		}
	}

	private fun ReadContext<*>.copyFileToTemp(ins: InputStream, fileName: String): File {
		val extractDir = this.nextExtractionDirectory().also {
			it.mkdirs()
		}
		val extract = File(extractDir, fileName).also {
			it.createNewFile()
		}
		FileOutputStream(extract).use { fos ->
			ins.copyTo(fos)
		}
		return extract
	}

	private tailrec fun ZipArchiveInputStream.tryNextEntry(
		iteration: Int = 0, // loop breaking
		errorHandler: ((IOException) -> Unit)?,
	): ZipArchiveEntry? {
		if (iteration >= 10) return null

		try {
			val entry = this.nextEntry
			if (entry != null) return entry
		} catch (e: IOException) {
			errorHandler?.invoke(e)
		}
		return this.tryNextEntry(iteration + 1, errorHandler)
	}
}
