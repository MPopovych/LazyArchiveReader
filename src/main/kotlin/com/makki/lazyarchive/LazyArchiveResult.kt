package com.makki.lazyarchive

import java.io.Closeable
import java.io.File


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
		/**
		 * Folders that hold all the temporary files, if not using the closable - be sure to delete them manually
		 */
		val temporaryFolders: List<File>,
	) : LazyArchiveResult {
		override fun close() {
			temporaryFolders.forEach { temp ->
				temp.deleteRecursively()
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
		/**
		 * Folders that hold all the temporary files, if not using the closable - be sure to delete them manually
		 */
		val temporaryFolders: List<File>,
	) : LazyArchiveResult {
		override fun close() {
			temporaryFolders.forEach { temp ->
				temp.deleteRecursively()
			}
		}
	}
}
