package fi.haapatalo.android.eyeball

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.vr.sdk.widgets.common.VrWidgetView
import com.google.vr.sdk.widgets.common.VrWidgetView.DisplayMode.FULLSCREEN_MONO
import com.google.vr.sdk.widgets.common.VrWidgetView.DisplayMode.FULLSCREEN_STEREO
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import java.io.File

class ImageViewer(private val context: Context, uiHandler: Handler, private val control: EyeballController, panoramaView: VrPanoramaView) :
        Viewer<VrPanoramaView>("image", context, uiHandler, panoramaView, "jpg", "jpeg") {

    init {
        panoramaView.setEventListener(ActivityEventListener())
    }

    private inner class ActivityEventListener : VrPanoramaEventListener() {
        override fun onLoadSuccess() {}

        override fun onLoadError(errorMessage: String?) {
            val msg = "Error loading image: $errorMessage"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Log.e(Companion.TAG, msg)
        }

        override fun onDisplayModeChanged(mode: Int) {
            super.onDisplayModeChanged(mode)
            if (mode == FULLSCREEN_STEREO || mode == FULLSCREEN_MONO) {
                control.activateMedia(EyeballController.MediaType.IMAGE)
                Log.i(TAG, "Activated image browser")
            }
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

class VideoViewer(private val context: Context, uiHandler: Handler, private val control: EyeballController, videoView: VrVideoView) :
        Viewer<VrVideoView>("video", context, uiHandler, videoView, "mp4", "mkv", "mov") {

    init {
        videoView.setEventListener(ActivityEventListener())
    }

    private inner class ActivityEventListener : VrVideoEventListener() {
        override fun onLoadSuccess() {}

        override fun onLoadError(errorMessage: String?) {
            val msg = "Error loading video: $errorMessage"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Log.e(Companion.TAG, msg)
        }

        override fun onDisplayModeChanged(mode: Int) {
            super.onDisplayModeChanged(mode)
            if (mode == FULLSCREEN_STEREO || mode == FULLSCREEN_MONO) {
                control.activateMedia(EyeballController.MediaType.VIDEO)
                Log.i(TAG, "Activated video browser")
            }
        }
    }

    override fun loadContents(view: VrVideoView, file: File): Boolean {
        val opts = VrVideoView.Options()
        opts.inputType = VrVideoView.Options.TYPE_STEREO_OVER_UNDER
        opts.inputFormat = VrVideoView.Options.FORMAT_DEFAULT
        view.loadVideo(Uri.fromFile(file), opts)
        return true
    }
}

abstract class Viewer<in T: VrWidgetView>(val type: String, private val context: Context, private val uiHandler: Handler, private val view: T, vararg extensions: String) {

    private val browser = ImageBrowser(type, object : ImageBrowser.ImageLoader {
        override fun load(image: File?) {
            loadImage(image)
        }
    }, *extensions)

    init {
        Log.i(TAG, "Directories: ${ImageBrowser.directories}")
    }

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
            Log.w(TAG, "No $type specified!")
            Toast.makeText(context,
                    "No ${type}s found in /Pictures/Eyeball", Toast.LENGTH_LONG).show()
            return
        }
        Log.i(TAG, "Preparing to load $type $image")
        backgroundImageLoaderTask?.cancel(true)

        val task = ImageLoaderTask()
        backgroundImageLoaderTask = task
        task.execute(image)
    }

    internal inner class ImageLoaderTask : AsyncTask<File, Void, Boolean>() {

        override fun doInBackground(vararg fileInformation: File): Boolean = loadImage(fileInformation[0])

        private fun loadImage(image: File): Boolean {
            Log.i(TAG, "Loading $type $image...")
            return loadContents(view, image)
        }
    }

    abstract fun loadContents(view: T, file: File): Boolean

    companion object {
        val TAG: String = ImageViewer::class.java.simpleName
    }
}
