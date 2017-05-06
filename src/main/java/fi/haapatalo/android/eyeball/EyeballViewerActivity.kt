package fi.haapatalo.android.eyeball

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.Toast
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView

import java.io.File
import android.widget.ArrayAdapter

class EyeballViewerActivity : Activity() {

    // The various events we need to handle happen on arbitrary threads. They need to be reposted to
    // the UI thread in order to manipulate the TextViews. This is only required if your app needs to
    // perform actions on the UI thread in response to controller events.
    private val uiHandler = Handler()

    // All state is managed in the inner class viewer that maintains non-null values
    private var viewer: EyeballViewer? = null

    inner class EyeballViewer(private val panoramaView: VrPanoramaView, dirSelector: Spinner) {
        private val controllerListener = ControllerEventListener()
        private val controllerManager = ControllerManager(this@EyeballViewerActivity, controllerListener)
        private val controller = controllerManager.controller
        val browser = ImageBrowser(object : ImageBrowser.ImageLoader {
            override fun load(image: File?) {
                loadImage(image)
            }
        })
        val dirs = browser.directories
        val dirAdapter = ArrayAdapter<String>(this@EyeballViewerActivity, android.R.layout.simple_spinner_item, dirs.map { it.first })
        init {
            controller.setEventListener(controllerListener)
            panoramaView.setEventListener(ActivityEventListener())
            Log.i(TAG, "Directories: ${browser.directories}")
            dirSelector.adapter = dirAdapter
            dirSelector.onItemSelectedListener = object: OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    browser.select(dirs[position].second)
                }
            }
        }

        private var backgroundImageLoaderTask: ImageLoaderTask? = null

        fun load() { browser.load() }
        fun onStart() { controllerManager.start() }
        fun onStop() { controllerManager.stop() }
        fun onPause() { panoramaView.pauseRendering() }
        fun onResume() { panoramaView.resumeRendering() }

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
                Toast.makeText(this@EyeballViewerActivity,
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
            override fun onLoadSuccess() {
            }

            override fun onLoadError(errorMessage: String?) {
                val msg = "Error loading image: $errorMessage"
                Toast.makeText(this@EyeballViewerActivity, msg, Toast.LENGTH_LONG).show()
                Log.e(TAG, msg)
            }
        }

        private inner class ControllerEventListener : Controller.EventListener(), ControllerManager.EventListener, Runnable {

            override fun onApiStatusChanged(state: Int) { uiHandler.post(this) }
            override fun onConnectionStateChanged(state: Int) { uiHandler.post(this) }

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val widgetView = findViewById(R.id.pano_view) as VrPanoramaView
        val dirSpinner = findViewById(R.id.dir_selector) as Spinner
        viewer = EyeballViewer(widgetView, dirSpinner)

        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        Log.i(TAG, "${hashCode()}.onNewIntent()")
        // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
        // future invocations.
        setIntent(intent)
        // Load the new image.
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        viewer?.load()
    }

    override fun onStart() {
        super.onStart()
        viewer?.onStart()
    }

    override fun onStop() {
        viewer?.onStop()
        super.onStop()
    }

    override fun onPause() {
        viewer?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewer?.onResume()
    }

    override fun onDestroy() {
        viewer?.onDestroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = EyeballViewerActivity::class.java.simpleName
    }
}
