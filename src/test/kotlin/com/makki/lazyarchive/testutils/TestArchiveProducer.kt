package com.makki.lazyarchive.testutils

import com.makki.lazyarchive.ArchiveType
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.io.OutputStream

/**
 * The purpose of this producer is to avoid packing archive files as resources,
 * which could introduce doubt in the integrity of the library.
 * Thus, they will be generated as part of the tests.
 */
object TestArchiveProducer {
	fun createArchive(testArchive: TestArchive, tempDir: File): File {
		val file = File(tempDir, testArchive.fullName)
		when (testArchive.type) {
			ArchiveType.ZIP -> ZipArchiveOutputStream(file).use {
				writeChildrenInZip(testArchive, it)
			}

			ArchiveType.SEVEN_ZIP -> TODO()
		}
		return file
	}

	private fun writeChildrenInZip(testArchive: TestArchive, zaos: ZipArchiveOutputStream) {
		for (child in testArchive.children) {
			val entry = ZipArchiveEntry(child.fullName)
			zaos.putArchiveEntry(entry)
			writeEntry(child, zaos)
			zaos.closeArchiveEntry()
		}
		zaos.finish()
	}

	private fun writeEntry(testFile: TestFile, outputStream: OutputStream) {
		when (testFile) {
			is TestArchive -> {
				when (testFile.type) {
					ArchiveType.ZIP -> writeZip(testFile, outputStream)
					ArchiveType.SEVEN_ZIP -> TODO()
				}
			}

			is TestPlainFile -> writeFile(testFile, outputStream)
		}
		outputStream.flush()
	}

	private fun writeFile(testPlainFile: TestPlainFile, outputStream: OutputStream) {
		outputStream.writer().also {
			it.write(testPlainFile.content)
			it.flush()
		}
	}

	private fun writeZip(testArchive: TestArchive, outputStream: OutputStream) {
		ZipArchiveOutputStream(outputStream).also {
			writeChildrenInZip(testArchive, it)
		}
	}
}
