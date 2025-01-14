package com.makki.lazyarchive

import com.makki.lazyarchive.testutils.*
import org.junit.jupiter.api.Assertions
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class LazyArchiveExtractTests {
	private val sampleText = "test text text text text text"
	private val sampleText2 = "test nested text"
	private val archiveOneNestedZip = TestArchive("test", ArchiveType.ZIP)
		.addArchive("f/nested_zip", ArchiveType.ZIP) { nested ->
			nested.addPlainFile("nested_folder/nested_text", "txt", sampleText2)
		}
		.addPlainFile("text", "txt", sampleText)

	private val defaultErrorHandler: (IOException) -> Unit = { e -> Assertions.fail(e) }
	private val tempDirectory = DirectoryProvider.initInBuildDirectory("test-temp-extract")

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

		// extract with unpacking all files
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
	fun testNoneUnpacked() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with unpacking no files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 3,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 100
		)
		reader
			.extract { false } // extract no files
			.successOrThrow()
			.use { result ->
				Assertions.assertTrue(result.fullRead)
				Assertions.assertEquals(0, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun test1DepthUnpacked() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with unpacking only level 1 files
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

		// extract with unpacking only txt files on level 1
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

	@Test
	fun testLoopBreaker1() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with loop break after first
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 1
		)
		reader
			.extract { true }
			.successOrThrow()
			.use { result ->
				Assertions.assertFalse(result.fullRead)
				Assertions.assertEquals(1, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun testLoopBreaker0() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with loop break before first
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 0
		)
		reader
			.extract { true }
			.successOrThrow()
			.use { result ->
				Assertions.assertFalse(result.fullRead)
				Assertions.assertEquals(0, result.extracted.size)
			}

		// clean up
		testArchive.delete()
	}

	@Test
	fun testCleanUpClosable() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// extract with loop break before first
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 100
		)
		val result = reader
			.extract { true }
			.successOrThrow()

		Assertions.assertTrue(result.temporaryFolders.isNotEmpty())
		Assertions.assertTrue(result.extracted.isNotEmpty())
		result.temporaryFolders.forEach {
			Assertions.assertTrue(it.exists())
		}
		result.extracted.forEach {
			Assertions.assertTrue(it.file.exists())
		}
		result.use { file ->
			Assertions.assertTrue(file.fullRead)
		}
		result.temporaryFolders.forEach {
			Assertions.assertFalse(it.exists())
		}
		result.extracted.forEach {
			Assertions.assertFalse(it.file.exists())
		}

		// clean up
		testArchive.delete()
	}
}
