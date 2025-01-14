package com.makki.lazyarchive


sealed interface LazyArchiveView {
	fun successOrThrow(): Success {
		return when (this) {
			is Fail -> throw this.error
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
		val files: List<FileMeta>,
	) : LazyArchiveView

	class Success(
		/**
		 * Extracted file metas.
		 */
		val files: List<FileMeta>,
		/**
		 * Marks the result as partial due to reaching the loopBreak value
		 */
		val fullRead: Boolean,
	) : LazyArchiveView
}
