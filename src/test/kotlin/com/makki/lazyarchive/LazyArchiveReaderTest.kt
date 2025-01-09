package com.makki.lazyarchive

import com.makki.lazyarchive.testutils.*
import org.junit.jupiter.api.Assertions
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class LazyArchiveReaderTest {
	private val sampleText = "test text text text text text"
	private val sampleText2 = "test nested text"
	private val archiveOneNestedZip = TestArchive("test", ArchiveType.ZIP)
		.addArchive("f/nested_zip", ArchiveType.ZIP) { nested ->
			nested.addPlainFile("nested_folder/nested_text", "txt", sampleText2)
		}
		.addPlainFile("text", "txt", sampleText)

	private val defaultErrorHandler: (IOException) -> Unit = { e -> Assertions.fail(e) }
	private val tempDirectory = DirectoryProvider.initInBuildDirectory("test-temp")

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
				Assertions.assertTrue(result.extracted.any { it.meta.fileName == "text.txt" })
				Assertions.assertTrue(result.extracted.any { it.meta.fileName == "nested_zip.zip" })
				Assertions.assertTrue(result.extracted.any { it.meta.fileName == "nested_text.txt" })
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
				Assertions.assertEquals("text.txt", fileResult.file.name)
				Assertions.assertEquals(sampleText, fileResult.file.readText())
				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(1, result.extracted.size)
			}

		reader
			.extract { h -> h.fileName.endsWith(".zip") }
			.successOrThrow()
			.use { result ->
				val fileResult = result.extracted.getOrNull(0) ?: throw NullPointerException("File not found")
				Assertions.assertEquals("nested_zip.zip", fileResult.file.name)
				Assertions.assertNotEquals(0, fileResult.file.length())
				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(1, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}
}
