package app.reportamelo.commons

import android.content.Context
import android.util.LruCache
import java.io.File
import java.util.Date

class CacheManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: CacheManager? = null

        fun getInstance(context: Context): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager(context).also { instance = it }
            }
        }
    }

    // In-memory cache
    private val memoryCache: LruCache<String, ByteArray>

    // Disk cache directory
    private val diskCacheDir: File = File(context.cacheDir, "CustomAPICache")

    init {
        // Allocate 1/8th of the available memory for this memory cache.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        memoryCache = object : LruCache<String, ByteArray>(cacheSize) {
            override fun sizeOf(key: String, value: ByteArray): Int {
                return value.size / 1024
            }
        }

        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    fun save(data: ByteArray, forKey: String) {
        // Save to Memory
        memoryCache.put(forKey, data)

        // Save to Disk
        val file = File(diskCacheDir, forKey)
        file.writeBytes(data)
    }

    fun getData(forKey: String): ByteArray? {
        // Check Memory Cache First
        memoryCache.get(forKey)?.let { return it }

        // Check Disk Cache
        val file = File(diskCacheDir, forKey)
        if (file.exists()) {
            val diskData = file.readBytes()
            // Repopulate memory cache
            memoryCache.put(forKey, diskData)
            return diskData
        }

        return null
    }

    fun clearCache() {
        memoryCache.evictAll()
        diskCacheDir.deleteRecursively()
        diskCacheDir.mkdirs()
    }
}

data class CacheEntry(
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() {
            // Data is stale after 1 hour (3600 * 1000 ms)
            return (System.currentTimeMillis() - timestamp) > 3600_000
        }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CacheEntry
        if (!data.contentEquals(other.data)) return false
        return timestamp == other.timestamp
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
