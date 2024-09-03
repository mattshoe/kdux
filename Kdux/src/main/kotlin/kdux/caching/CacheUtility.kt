package kdux.caching

import kdux.KduxMenu

object CacheUtility {
    val cacheLocation: String
        get() =  "${KduxMenu.cacheDirectory}/kdux/cache"

    fun cacheLocation(key: String): String {
        val fileName = encodeFileName(key)
        return "$cacheLocation/${fileName}.kdux"
    }

    private fun encodeFileName(fileName: String): String {
        // Perfectly fine encoding. Efficient, consistent, and filename readability doesn't matter
        return "${fileName.hashCode()}"
    }
}