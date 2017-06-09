package fi.haapatalo.android.eyeball

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileFilter

class ImageBrowser(val type: String, private val loader: ImageBrowser.ImageLoader, vararg extensions: String) {
    private var index = 0
    private var selectedDir = DIR
    private val lowerExts = extensions.map { it.toLowerCase() }

    interface ImageLoader {
        fun load(image: File?)
    }

    operator fun next() {
        Log.i(TAG, "Loading next $type")
        index += 1
        load()
    }

    fun previous() {
        Log.i(TAG, "Loading previous $type")
        index -= 1
        load()
    }

    fun load() {
        val image = image
        Log.i(TAG, "Loading $type $index: $image")
        loader.load(image)
    }

    fun select(dir: File) {
        if (dir.isDirectory && dir.canRead()) {
            Log.i(TAG, "Selected directory $dir")
            selectedDir = dir
            load()
        }
    }

    val image: File?
        get() {
            val files = images(selectedDir)
            if (files.isEmpty()) {
                Log.w(TAG, "No ${type}s in " + selectedDir)
                return null
            }
            index = bound(index, files.size)
            return files[index]
        }

    val filter: (File) -> Boolean = { !it.isDirectory && it.extension.toLowerCase() in lowerExts }

    fun images(dir: File): List<File> {
        Log.i(TAG, "Loading ${type}s from " + dir)
        if (dir.isDirectory) {
            if (!dir.canRead()) {
                Log.w(TAG, "Cannot read $type directory")
                return noFiles
            }
            val files = dir.listFiles(filter)
            return files?.sorted() ?: noFiles
        } else
            return noFiles
    }

    companion object {

        val TAG = "ImageBrowser"
        val DIR = File("${Environment.getExternalStorageDirectory().path}/Pictures/Eyeball")

        fun bound(value: Int, max: Int): Int = when {
            value < 0 -> bound(value + max, max)
            value >= max -> bound(value - max, max)
            else -> value
        }

        val noFiles: List<File> = listOf()

        fun directories(parent: File, prefix: String): List<Pair<String, File>> {
            val dirs = parent.listFiles(FileFilter { it.isDirectory })
            val namedDirs = dirs.sorted().map{ combine(prefix, it.name) to it }
            return namedDirs + namedDirs.map { directories(it.second, it.first) }.flatten()
        }

        fun combine(a: String, b: String): String = when {
            a.isBlank() -> b
            b.isBlank() -> a
            else -> "$a/$b"
        }

        val directories: List<Pair<String, File>>
            get() = listOf("[Eyeball]" to DIR) + directories(DIR, "")

    }
}
