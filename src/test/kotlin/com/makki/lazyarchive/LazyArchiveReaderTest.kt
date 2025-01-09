package com.makki.lazyarchive

import org.junit.jupiter.api.Assertions
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class LazyArchiveReaderTest {

	private val defaultErrorHandler: (IOException) -> Unit = { e -> Assertions.fail(e) }
	private val tempDirectory = DirectoryProvider.initInBuildDirectory("test-temp")

	private val archiveOneNestedZip = TestArchive("test", ArchiveType.ZIP)
		.addArchive("f/nested_zip", ArchiveType.ZIP) { nested ->
			nested.addPlainFile("nested_folder/nested_text", "txt", "test nested text")
		}
		.addPlainFile("text", "txt", "test text text text text text")

	@BeforeTest
	fun initial() {
		println("Using temporary directory at ${tempDirectory.absolutePath}")
	}

	@AfterTest
	fun cleanup() {
		tempDirectory.listFiles()?.forEach {
			it.deleteRecursively()
		}
	}

	@Test
	fun testAllUnpacked() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with allocating all files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = defaultErrorHandler
		)
		reader
			.extract { true } // extract all files
			.successOrThrow()
			.use { result ->
				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(3, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun test1DepthUnpacked() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with allocating all files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 1,
			temporaryDirectory = tempDirectory,
			errorHandler = defaultErrorHandler
		)
		reader
			.extract { true } // extract all files
			.successOrThrow()
			.use { result ->
				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(2, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun testSpecificExtract() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with allocating all files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 1,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e }
		)
		reader
			.extract { h -> h.fileName.endsWith(".txt") }
			.successOrThrow()
			.use { result ->
				val fileResult = result.extracted.getOrNull(0) ?: throw NullPointerException("File not found")
				val file = fileResult.file
				val meta = fileResult.meta
				println("Extracted file: ${meta.fileName}, zip size: ${meta.compressedSize} -> ${file.length()}")

				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(1, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun demoSample() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with allocating all files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 1,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e }
		)
		reader
			.extract { h -> h.fileName.endsWith(".txt") }
			.use { result ->
				when (result) {
					is LazyArchiveResult.Fail -> throw result.error
					is LazyArchiveResult.Success -> return@use result.extracted.map { it.file }
				}
				result.extracted.forEach { extraction ->
					val f = extraction.file
					// read and parse the file - perform the action
				}
				val fileResult = result.extracted.getOrNull(0) ?: throw NullPointerException("File not found")
				val file = fileResult.file
				val meta = fileResult.meta
				println("Extracted file: ${meta.fileName}, zip size: ${meta.compressedSize} -> ${file.length()}")

				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(1, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}
}
