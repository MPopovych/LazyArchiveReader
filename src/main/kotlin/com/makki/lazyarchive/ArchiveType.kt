package com.makki.lazyarchive

enum class ArchiveType {
	ZIP, SEVEN_ZIP;

	val extension: String
		get() = when (this) {
			ZIP -> "zip"
			SEVEN_ZIP -> "7z"
		}

	companion object {
		fun fromExtension(extension: String): ArchiveType? {
			return entries.find { it.extension == extension }
		}
	}
}