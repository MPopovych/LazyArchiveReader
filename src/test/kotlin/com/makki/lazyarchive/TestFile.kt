package com.makki.lazyarchive


sealed interface TestFile {
	val name: String
	val fullName: String
}

interface WithChildren {
	val children: ArrayList<TestFile>
}

data class TestPlainFile(
	override val name: String,
	val extension: String,
	val content: String,
) : TestFile {
	override val fullName: String
		get() = "$name.$extension"
}

data class TestArchive(
	override val name: String,
	val type: ArchiveType,
) : TestFile, WithChildren {
	override val children: ArrayList<TestFile> = ArrayList()
	override val fullName: String
		get() = "$name.${type.extension}"
}

fun <T : WithChildren> T.addArchive(name: String, type: ArchiveType, block: (nested: TestArchive) -> Unit) = this.also {
	val archive = TestArchive(name, type)
	block(archive)
	children.add(archive)
}

fun <T : WithChildren> T.addPlainFile(name: String, extension: String, content: String): T = this.also {
	children.add(TestPlainFile(name, extension, content))
}
