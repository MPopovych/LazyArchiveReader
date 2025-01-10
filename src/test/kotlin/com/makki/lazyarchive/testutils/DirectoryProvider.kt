package com.makki.lazyarchive.testutils

import java.io.File
import java.nio.file.Files

object DirectoryProvider {
	fun initInBuildDirectory(subfolder: String): File {
		val projectPath = System.getProperty("user.dir") + "/build/"
		return File(File(projectPath), "$subfolder/").also {
			it.mkdirs()
		}
	}

	fun getTempDeleteOnExit(): File {
		val temp: File = Files.createTempDirectory("test").toFile()
		temp.deleteOnExit()
		return temp
	}
}
