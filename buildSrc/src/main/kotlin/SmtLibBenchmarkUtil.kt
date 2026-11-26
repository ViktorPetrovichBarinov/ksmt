import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun Project.mkSmtLibBenchmarkTestData(name: String) = tasks.register("smtLibBenchmark-$name") {
    doLast {
        val path = buildDir.resolve("smtLibBenchmark/$name")
        val downloadTarget = path.resolve("$name.tar.zst")
        val url = "$BENCHMARK_REPO_URL/$name.tar.zst/content"

        download(url, downloadTarget)

        path.executeIfNotReady("unpack-complete") {
            exec {
                commandLine("tar", "--zstd", "-xf", downloadTarget.absolutePath, "-C", path.absolutePath)
            }
        }

        val testResources = testResourceDir() ?: error("No resource directory found for $name benchmark")
        val testData = testResources.resolve("testData")

        testData.executeIfNotReady("$name-copy-complete") {
            val smtFiles = path.walkTopDown().filter { it.extension == "smt2" }.toList()
            copy {
                from(smtFiles.toTypedArray())
                into(testData)
                rename { "${name}_$it" }
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }
}

fun Project.usePreparedSmtLibBenchmarkTestData(path: File) = tasks.register("smtLibBenchmark-data-use") {
    doLast {
        check(path.exists()) { "No test data provided" }
        val testResources = testResourceDir() ?: error("No resource directory found for benchmarks")
        val testData = testResources.resolve("testData")

        testData.executeIfNotReady("test-data-ready") {
            copy {
                from(path)
                into(testData)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }
}

/**
 * Flat QF_S archive for the long string-theory runs.
 * Duplicate basenames are dropped, matching [mkSmtLibBenchmarkTestData].
 * The resulting zip is what CI caches; a later unpack only reads that zip.
 */
fun Project.downloadStringSmtLibBenchmarkTestData(downloadPath: File, testDataPath: File) =
    tasks.register("downloadStringSmtLibBenchmarkTestData") {
        doLast {
            if (!downloadPath.exists()) {
                val name = "QF_S"
                val path = buildDir.resolve("smtLibBenchmark/$name")
                path.mkdirs()
                val archive = path.resolve("$name.tar.zst")
                download("$BENCHMARK_REPO_URL/$name.tar.zst/content", archive)

                path.executeIfNotReady("unpack-complete") {
                    exec {
                        commandLine("tar", "--zstd", "-xf", archive.absolutePath, "-C", path.absolutePath)
                    }
                }

                val partial = File("${downloadPath.absolutePath}.partial")
                if (partial.exists()) partial.delete()
                downloadPath.parentFile.mkdirs()
                ZipOutputStream(partial.outputStream().buffered()).use { zip ->
                    val seen = mutableSetOf<String>()
                    path.walkTopDown()
                        .filter { it.isFile && it.extension == "smt2" }
                        .forEach { file ->
                            val entryName = "${name}_${file.name}"
                            if (!seen.add(entryName)) return@forEach
                            zip.putNextEntry(ZipEntry(entryName))
                            file.inputStream().buffered().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                }
                check(partial.renameTo(downloadPath)) {
                    "Failed to publish string benchmark archive to ${downloadPath.absolutePath}"
                }
            }

            testDataPath.mkdirs()
            copy {
                from(zipTree(downloadPath))
                into(testDataPath)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

fun Project.downloadPreparedSmtLibBenchmarkTestData(downloadPath: File, testDataPath: File, version: String) =
    tasks.register("downloadPreparedSmtLibBenchmarkTestData") {
        doLast {
            val benchmarksUrl = "https://github.com/UnitTestBot/ksmt/releases/download/$version/benchmarks.zip"

            download(benchmarksUrl, downloadPath)

            copy {
                from(zipTree(downloadPath))
                into(testDataPath)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

private fun Project.testResourceDir(): File? {
    val sourceSets = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer
    return sourceSets["test"]?.output?.resourcesDir
}

private const val BENCHMARK_REPO_URL = "https://zenodo.org/api/records/16740866/files"

private inline fun File.executeIfNotReady(markerName: String, body: () -> Unit) {
    val marker = this.resolve(markerName)
    if (marker.exists()) return

    body()

    marker.createNewFile()
}
