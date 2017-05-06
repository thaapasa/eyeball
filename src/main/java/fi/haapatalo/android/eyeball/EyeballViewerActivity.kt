package fi.haapatalo.android.eyeball

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView

import java.io.File

/**
 * A basic PanoWidget Activity to load panorama images from disk. It will load a test image by
 * default. It can also load an arbitrary image from disk using:
 * adb shell am start -a "android.intent.action.VIEW" \
 * -n "com.google.vr.sdk.samples.simplepanowidget/.EyeballViewerActivity" \
 * -d "/sdcard/FILENAME.JPG"
 *
 *
 * To load stereo images, "--ei inputType 2" can be used to pass in an integer extra which will set
 * VrPanoramaView.Options.inputType.
 */
class EyeballViewerActivity : Activity() {
    /**
     * Actual panorama widget.
     */
    private var panoWidgetView: VrPanoramaView? = null
    /**
     * Arbitrary variable to track load status. In this example, this variable should only be accessed
     * on the UI thread. In a real app, this variable would be code that performs some UI actions when
     * the panorama is fully loaded.
     */
    var loadImageSuccessful: Boolean = false
    private var backgroundImageLoaderTask: ImageLoaderTask? = null

    // These two objects are the primary APIs for interacting with the Daydream controller.
    private var controllerManager: ControllerManager? = null
    private var controller: Controller? = null

    // The various events we need to handle happen on arbitrary threads. They need to be reposted to
    // the UI thread in order to manipulate the TextViews. This is only required if your app needs to
    // perform actions on the UI thread in response to controller events.
    private val uiHandler = Handler()

    private var browser: ImageBrowser? = null

    /**
     * Called when the app is launched via the app icon or an intent using the adb command above. This
     * initializes the app and loads the image to render.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

        panoWidgetView = findViewById(R.id.pano_view) as VrPanoramaView
        panoWidgetView!!.setEventListener(ActivityEventListener())

        // Start the ControllerManager and acquire a Controller object which represents a single
        // physical controller. Bind our listener to the ControllerManager and Controller.
        val listener = EventListener()
        controllerManager = ControllerManager(this, listener)
        controller = controllerManager!!.controller
        controller!!.setEventListener(listener)

        browser = ImageBrowser(object : ImageBrowser.ImageLoader {
            override fun load(image: File?) {
                loadImage(image)
            }
        })
        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(intent)
    }

    /**
     * Called when the Activity is already running and it's given a new intent.
     */
    override fun onNewIntent(intent: Intent) {
        Log.i(TAG, this.hashCode().toString() + ".onNewIntent()")
        // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
        // future invocations.
        setIntent(intent)
        // Load the new image.
        handleIntent(intent)
    }

    /**
     * Load custom images based on the Intent or load the default image. See the Javadoc for this
     * class for information on generating a custom intent via adb.
     */
    private fun handleIntent(intent: Intent) {
        // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
        // take 100s of milliseconds.
        browser!!.load()
    }

    private fun loadImage(image: File?) {
        if (image == null) {
            Log.w(TAG, "No image specified!")
            Toast.makeText(this@EyeballViewerActivity,
                    "No images found in /Pictures/Eyeball", Toast.LENGTH_LONG).show()
            return
        }
        Log.i(TAG, "Preparing to load image $image")
        if (backgroundImageLoaderTask != null) {
            // Cancel any task from a previous intent sent to this activity.
            backgroundImageLoaderTask!!.cancel(true)
        }
        backgroundImageLoaderTask = ImageLoaderTask()
        backgroundImageLoaderTask!!.execute(image)
    }

    override fun onStart() {
        super.onStart()
        controllerManager!!.start()
    }

    override fun onStop() {
        controllerManager!!.stop()
        super.onStop()
    }

    override fun onPause() {
        panoWidgetView!!.pauseRendering()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        panoWidgetView!!.resumeRendering()
    }

    override fun onDestroy() {
        // Destroy the widget and free memory.
        panoWidgetView!!.shutdown()

        // The background task has a 5 second timeout so it can potentially stay alive for 5 seconds
        // after the activity is destroyed unless it is explicitly cancelled.
        if (backgroundImageLoaderTask != null) {
            backgroundImageLoaderTask!!.cancel(true)
        }
        super.onDestroy()
    }

    /**
     * Helper class to manage threading.
     */
    internal inner class ImageLoaderTask : AsyncTask<File, Void, Boolean>() {

        /**
         * Reads the bitmap from disk in the background and waits until it's loaded by pano widget.
         */
        override fun doInBackground(vararg fileInformation: File): Boolean? {
            return loadImage(fileInformation[0])
        }

        protected fun loadImage(image: File): Boolean? {
            Log.i(TAG, "Loading image $image...")
            val panoOptions = VrPanoramaView.Options()
            panoOptions.inputType = VrPanoramaView.Options.TYPE_STEREO_OVER_UNDER
            val bmp = BitmapFactory.decodeFile(image.path)
            panoWidgetView!!.loadImageFromBitmap(bmp, panoOptions)
            return true
        }
    }

    /**
     * Listen to the important events from widget.
     */
    private inner class ActivityEventListener : VrPanoramaEventListener() {
        /**
         * Called by pano widget on the UI thread when it's done loading the image.
         */
        override fun onLoadSuccess() {
            loadImageSuccessful = true
        }

        /**
         * Called by pano widget on the UI thread on any asynchronous error.
         */
        override fun onLoadError(errorMessage: String?) {
            loadImageSuccessful = false
            Toast.makeText(
                    this@EyeballViewerActivity, "Error loading pano: " + errorMessage!!, Toast.LENGTH_LONG)
                    .show()
            Log.e(TAG, "Error loading pano: " + errorMessage)
        }
    }


    // We receive all events from the Controller through this listener. In this example, our
    // listener handles both ControllerManager.EventListener and Controller.EventListener events.
    // This class is also a Runnable since the events will be reposted to the UI thread.
    private inner class EventListener : Controller.EventListener(), ControllerManager.EventListener, Runnable {

        override fun onApiStatusChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onConnectionStateChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onRecentered() {
            // In a real GVR application, this would have implicitly called recenterHeadTracker().
            // Most apps don't care about this, but apps that want to implement custom behavior when a
            // recentering occurs should use this callback.
            Log.d(TAG, "Recentered")
        }

        override fun onUpdate() {
            uiHandler.post(this)
        }

        private var clickButtonState = false
        private var appButtonState = false

        // Update the various TextViews in the UI thread.
        override fun run() {
            controller!!.update()

            if (controller!!.appButtonState != appButtonState) {
                appButtonState = controller!!.appButtonState
                if (appButtonState) {
                    Log.i(TAG, "App button pressed, switching to previous image")
                    browser!!.previous()
                }
            }
            if (controller!!.clickButtonState != clickButtonState) {
                clickButtonState = controller!!.clickButtonState
                if (clickButtonState) {
                    Log.i(TAG, "Click button pressed, switching to next image")
                    browser!!.next()
                }
            }
        }
    }

    companion object {
        private val TAG = EyeballViewerActivity::class.java.simpleName
    }
}
