package com.makki.lazyarchive

import com.makki.lazyarchive.testutils.*
import org.junit.jupiter.api.Assertions
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class LazyArchiveReadTests {
	private val sampleText = "test text text text text text"
	private val sampleText2 = "test nested text"
	private val archiveOneNestedZip = TestArchive("test", ArchiveType.ZIP)
		.addArchive("f/nested_zip", ArchiveType.ZIP) { nested ->
			nested.addPlainFile("nested_folder/nested_text", "txt", sampleText2)
		}
		.addPlainFile("text", "txt", sampleText)

	private val defaultErrorHandler: (IOException) -> Unit = { e -> Assertions.fail(e) }
	private val tempDirectory = DirectoryProvider.initInBuildDirectory("test-temp-peek")

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
	fun testAllFound() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// peek with listing all files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = defaultErrorHandler
		)
		val result = reader
			.peek { true } // extract all nested zips
			.successOrThrow()

		Assertions.assertTrue(result.files.any { it.fileName == "text.txt" })
		Assertions.assertTrue(result.files.any { it.fileName == "nested_zip.zip" })
		Assertions.assertTrue(result.files.any { it.fileName == "nested_text.txt" })
		Assertions.assertTrue(result.fullRead)
		Assertions.assertEquals(3, result.files.size)

		// clean up
		testArchive.delete()
	}

	@Test
	fun test1DepthUnpacked() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// peek with listing all 1 level files
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 1,
			temporaryDirectory = tempDirectory,
			errorHandler = defaultErrorHandler
		)
		val result = reader
			.peek { true } // extract all nested zips, but will miss due to depth
			.successOrThrow()

		Assertions.assertTrue(result.fullRead)
		Assertions.assertEquals(2, result.files.size)

		// clean up
		testArchive.delete()
	}

	@Test
	fun testNoNested() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// peek with listing all level 1 files, ignore all nested zips
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = defaultErrorHandler
		)
		val result = reader
			.peek { false } // extract no nested zips, but will miss due to depth
			.successOrThrow()

		Assertions.assertTrue(result.fullRead)
		Assertions.assertEquals(2, result.files.size)

		// clean up
		testArchive.delete()
	}

	@Test
	fun testLoopBreaker1() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// peek with listing all files, loop break on first
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 1
		)
		val result = reader
			.peek { true }
			.successOrThrow()

		Assertions.assertFalse(result.fullRead)
		Assertions.assertEquals(1, result.files.size)

		// clean up
		testArchive.delete()
	}

	@Test
	fun testLoopBreaker0() {
		// prepare example archive
		val testArchive = TestArchiveProducer.createArchive(archiveOneNestedZip, tempDirectory)

		// peek with listing all files, loop break before first
		val reader = LazyArchiveReader(
			testArchive,
			archiveDepth = 2,
			temporaryDirectory = tempDirectory,
			errorHandler = { e -> throw e },
			loopBreaker = 0
		)
		val result = reader
			.peek { true }
			.successOrThrow()

		Assertions.assertFalse(result.fullRead)
		Assertions.assertEquals(0, result.files.size)

		// clean up
		testArchive.delete()
	}
}
