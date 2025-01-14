package com.makki.lazyarchive

import java.io.File

data class TemporaryFile(
	val file: File,
	val meta: FileMeta,
)
