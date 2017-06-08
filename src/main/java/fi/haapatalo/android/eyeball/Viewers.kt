package fi.haapatalo.android.eyeball

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.widget.Spinner
import android.widget.Toast
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.widgets.common.VrWidgetView
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import java.io.File

class ImageViewer(private val context: Context, uiHandler: Handler, panoramaView: VrPanoramaView) :
        Viewer<VrPanoramaView>(context, uiHandler, panoramaView, "jpg", "jpeg") {

    override fun initEventListeners(view: VrPanoramaView) = view.setEventListener(ActivityEventListener())

    private inner class ActivityEventListener : VrPanoramaEventListener() {
        override fun onLoadSuccess() {}

        override fun onLoadError(errorMessage: String?) {
            val msg = "Error loading image: $errorMessage"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Log.e(Companion.TAG, msg)
        }
    }

    override fun loadContents(view: VrPanoramaView, file: File): Boolean {
        val opts = VrPanoramaView.Options()
        opts.inputType = VrPanoramaView.Options.TYPE_STEREO_OVER_UNDER
        val bmp = BitmapFactory.decodeFile(file.path)
        view.loadImageFromBitmap(bmp, opts)
        return true
    }

}

class VideoViewer(private val context: Context, uiHandler: Handler, videoView: VrVideoView) :
        Viewer<VrVideoView>(context, uiHandler, videoView, "mp4", "mkv", "mov") {

    override fun initEventListeners(view: VrVideoView) = view.setEventListener(ActivityEventListener())

    private inner class ActivityEventListener : VrVideoEventListener() {
        override fun onLoadSuccess() {}

        override fun onLoadError(errorMessage: String?) {
            val msg = "Error loading video: $errorMessage"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Log.e(Companion.TAG, msg)
        }
    }

    override fun loadContents(view: VrVideoView, file: File): Boolean {
        val opts = VrVideoView.Options()
        opts.inputType = VrVideoView.Options.TYPE_STEREO_OVER_UNDER
        view.loadVideo(Uri.fromFile(file), opts)
        return true
    }
}

abstract class Viewer<in T: VrWidgetView>(private val context: Context, private val uiHandler: Handler, private val view: T, vararg extensions: String) {

    private val browser = ImageBrowser(object : ImageBrowser.ImageLoader {
        override fun load(image: File?) {
            loadImage(image)
        }
    }, *extensions)

    init {
        initEventListeners(view)
        Log.i(TAG, "Directories: ${ImageBrowser.directories}")
    }

    abstract fun initEventListeners(view: T): Unit

    private var backgroundImageLoaderTask: ImageLoaderTask? = null

    fun next() = browser.next()
    fun previous() = browser.previous()

    fun select(dir: File) = browser.select(dir)
    fun load() = browser.load()
    fun onPause() = view.pauseRendering()
    fun onResume() = view.resumeRendering()
    fun onDestroy() {
        // Destroy the view and free memory.
        view.shutdown()
        // The background task has a 5 second timeout so it can potentially stay alive for 5 seconds
        // after the activity is destroyed unless it is explicitly cancelled.
        backgroundImageLoaderTask?.cancel(true)
    }

    private fun loadImage(image: File?) {
        if (image == null) {
            Log.w(TAG, "No image specified!")
            Toast.makeText(context,
                    "No images found in /Pictures/Eyeball", Toast.LENGTH_LONG).show()
            return
        }
        Log.i(TAG, "Preparing to load image $image")
        backgroundImageLoaderTask?.cancel(true)

        val task = ImageLoaderTask()
        backgroundImageLoaderTask = task
        task.execute(image)
    }

    internal inner class ImageLoaderTask : AsyncTask<File, Void, Boolean>() {

        override fun doInBackground(vararg fileInformation: File): Boolean = loadImage(fileInformation[0])

        private fun loadImage(image: File): Boolean {
            Log.i(TAG, "Loading image $image...")
            return loadContents(view, image)
        }
    }

    abstract fun loadContents(view: T, file: File): Boolean

    companion object {
        val TAG = ImageViewer::class.java.simpleName
    }
}
