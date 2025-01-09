# Lazy Archive Reader

## A kotlin library for reading archived files on demand.

### Description

This library provides an api to read the archive structure and to extract only specific files. 

The code snipped below extracts a file of specific extension. File metadata is available to control what is extracted.
With the metadata it's possible to check size for zip bombs, track nested archives, peek into structure without extraction.

```kotlin 
val testArchive = File("samples/some.zip")
val temporaryDirectory = File("temp-storage/")
val reader = LazyArchiveReader(testArchive, temporaryDirectory)
reader
    .extract { m -> m.fileName.endsWith(".txt") }
    .successOrThrow()
    .use { result ->
        val fileResult = result.extracted.getOrNull(0) ?: throw NullPointerException("File not found")
        val file = fileResult.file
        val meta = fileResult.meta
        println("Extracted file: ${meta.fileName}, size: ${meta.compressedSize} -> ${file.length()}")
    }
```

### Usage
**Step 1.** Create an instance of **LazyArchiveReader**
```kotlin
val reader = LazyArchiveReader(
    testArchive, // the file of the archive itself
    archiveDepth = 5, // level of nested archives that will be scanned
    temporaryDirectory = tempDirectory, // a directory for temporary files, you should delete the contents when done
    errorHandler = defaultErrorHandler // callback for logging or propagating an exception
)
```
For more details see the source code

**Step 2.** Perform the extraction with a predicate
```kotlin
val result = reader.extract { meta ->
    meta.fileName.endsWith(".txt") && meta.fileSize < ALLOWED_FILE_SIZE 
}
```
To deflate the file the predicate should return **true**. 
Also see **FileMeta** source code for the data available in the predicate.
The *extract* functions returns an object of **LazyArchiveResult**.

**Step 3.** Handle the read result of **LazyArchiveResult**.
This interface represents a result pattern of two states: **Fail** and **Success**.
It can be handled by a when statement, casting or using a method that will throw on Fail.

**The result object is a *Closable* which will remove the extraction results once done.**

```kotlin
result.successOrThrow().use { success ->
    success.extracted.forEach { extraction ->
        val f = extraction.file
        // read and parse the file - perform the action
    }
}
// or
val files = result.use { result ->
    when (result) {
        is LazyArchiveResult.Fail -> throw result.error
        is LazyArchiveResult.Success -> return@use result.extracted.map { it.file }
    }
}

```

## WIP
This project is still in development and might never be finalised. Feel free to contribute with a PR to this project. 

## Plans and tasks
- Implement 7z support
- Implement a read-only function to produce a data class that mirrors the archive structure
- More unit tests
- Benchmarks
