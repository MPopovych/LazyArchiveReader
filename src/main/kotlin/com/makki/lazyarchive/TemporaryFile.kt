package com.makki.lazyarchive

import java.io.File

data class TemporaryFile(
	val file: File,
	val folderForDeletion: File?,
	val meta: FileMeta,
)
