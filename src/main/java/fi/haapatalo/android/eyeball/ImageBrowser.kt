package fi.haapatalo.android.eyeball

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileFilter
import java.util.*

class ImageBrowser(private val loader: ImageBrowser.ImageLoader) {
    private var index = 0

    interface ImageLoader {
        fun load(image: File?)
    }

    operator fun next() {
        index += 1
        load()
    }

    fun previous() {
        index -= 1
        load()
    }

    fun load() {
        val image = image
        Log.i(TAG, "Loading image $index: $image")
        loader.load(image)
    }

    val image: File?
        get() {
            val files = images
            if (files.isEmpty()) {
                Log.w(TAG, "No images in " + DIR)
                return null
            }
            index = bound(index, files.size)
            return files[index]
        }

    private val images: List<File>
        get() {
            Log.i(TAG, "Loading images from " + DIR)
            if (DIR.isDirectory) {
                if (!DIR.canRead()) {
                    Log.w(TAG, "Cannot read image directory")
                }
                val images = DIR.listFiles(notDirectoryFilter)
                return if (images != null) Arrays.asList(*images) else noFiles
            } else
                return noFiles
        }

    companion object {

        private val TAG = "ImageBrowser"
        private val DIR = File("${Environment.getExternalStorageDirectory().path}/Pictures/Eyeball")

        private fun bound(value: Int, max: Int): Int = when {
            value < 0 -> bound(value + max, max)
            value > max -> bound(value - max, max)
            else -> value
        }

        private val notDirectoryFilter = FileFilter { !it.isDirectory }

        private val noFiles: List<File> = listOf()
    }
}
