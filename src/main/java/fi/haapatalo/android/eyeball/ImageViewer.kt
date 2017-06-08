package fi.haapatalo.android.eyeball

import android.R
import android.content.Context
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import java.io.File

class ImageViewer(private val context: Context, private val uiHandler: Handler, private val panoramaView: VrPanoramaView, dirSelector: Spinner) {

    private val controllerListener = ControllerEventListener()
    private val controllerManager = ControllerManager(context, controllerListener)
    private val controller = controllerManager.controller
    private val browser = ImageBrowser(object : ImageBrowser.ImageLoader {
        override fun load(image: File?) {
            loadImage(image)
        }
    })
    private val dirs = browser.directories
    private val dirAdapter = ArrayAdapter<String>(context, R.layout.simple_spinner_item, dirs.map { it.first })

    init {
        controller.setEventListener(controllerListener)
        panoramaView.setEventListener(ActivityEventListener())
        Log.i(TAG, "Directories: ${browser.directories}")
        dirSelector.adapter = dirAdapter
        dirSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                browser.select(dirs[position].second)
            }
        }
    }

    private var backgroundImageLoaderTask: ImageLoaderTask? = null

    fun load() = browser.load()
    fun onStart() = controllerManager.start()
    fun onStop() = controllerManager.stop()
    fun onPause() = panoramaView.pauseRendering()
    fun onResume() = panoramaView.resumeRendering()
    fun onDestroy() {
        // Destroy the panoramaView and free memory.
        panoramaView.shutdown()
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

    private inner class ActivityEventListener : VrPanoramaEventListener() {
        override fun onLoadSuccess() {}

        override fun onLoadError(errorMessage: String?) {
            val msg = "Error loading image: $errorMessage"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Log.e(TAG, msg)
        }
    }

    private inner class ControllerEventListener : Controller.EventListener(), ControllerManager.EventListener, Runnable {

        override fun onApiStatusChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onConnectionStateChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onRecentered() {
            Log.d(TAG, "Recentered")
        }

        override fun onUpdate() {
            uiHandler.post(this)
        }

        private var clickButtonState = false
        private var appButtonState = false

        fun trackChange(cur: Boolean, new: Boolean, runIfChangedToTrue: () -> Unit, message: String): Boolean {
            if (new && !cur) {
                Log.i(TAG, message)
                runIfChangedToTrue()
            }
            return new
        }

        override fun run() {
            controller.update()

            appButtonState = trackChange(appButtonState, controller.appButtonState,
                    { browser.previous() }, "Switching to previous image")
            clickButtonState = trackChange(clickButtonState, controller.clickButtonState,
                    { browser.next() }, "Switching to next image")
        }
    }


    internal inner class ImageLoaderTask : AsyncTask<File, Void, Boolean>() {

        override fun doInBackground(vararg fileInformation: File): Boolean = loadImage(fileInformation[0])

        private fun loadImage(image: File): Boolean {
            Log.i(TAG, "Loading image $image...")
            val opts = VrPanoramaView.Options()
            opts.inputType = VrPanoramaView.Options.TYPE_STEREO_OVER_UNDER
            val bmp = BitmapFactory.decodeFile(image.path)
            panoramaView.loadImageFromBitmap(bmp, opts)
            return true
        }
    }

    companion object {
        private val TAG = ImageViewer::class.java.simpleName
    }
}