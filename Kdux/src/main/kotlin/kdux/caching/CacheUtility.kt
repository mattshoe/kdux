package kdux.caching

import kdux.KduxMenu
import java.io.File

object CacheUtility {
    private lateinit var cacheDirectory: String
    val cacheLocation: String
        get() = "${cacheDirectory}/kdux"

    fun cacheLocation(key: String): String {
        val fileName = encodeFileName(key)
        return "$cacheLocation/${fileName}.kdux"
    }

    internal fun setCacheDirectory(file: File) {
        cacheDirectory = file.absolutePath
        File(cacheLocation).mkdirs()
    }

    private fun encodeFileName(fileName: String): String {
        // Perfectly fine encoding. Efficient, consistent, and filename readability doesn't matter
        return "${fileName.hashCode()}"
    }
}