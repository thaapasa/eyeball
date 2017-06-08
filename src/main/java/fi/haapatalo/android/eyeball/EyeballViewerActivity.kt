package fi.haapatalo.android.eyeball

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.google.vr.sdk.widgets.video.VrVideoView

class EyeballViewerActivity : Activity() {

    // The various events we need to handle happen on arbitrary threads. They need to be reposted to
    // the UI thread in order to manipulate the TextViews. This is only required if your app needs to
    // perform actions on the UI thread in response to controller events.
    private val uiHandler = Handler()

    // All state is managed in these classes that maintain non-null values
    private var imageViewer: ImageViewer? = null
    private var videoViewer: VideoViewer? = null
    private var controller: ControllerHandler? = null
    private val dirs = ImageBrowser.directories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val panoView = findViewById(R.id.pano_view) as VrPanoramaView
        val videoView = findViewById(R.id.video_view) as VrVideoView

        imageViewer = ImageViewer(this, uiHandler, panoView)
        videoViewer = VideoViewer(this, uiHandler, videoView)

        val control = object: EyeballController {
            override fun next() {
                imageViewer?.next()
                videoViewer?.next()
            }

            override fun previous() {
                imageViewer?.previous()
                videoViewer?.previous()
            }
        }
        controller = ControllerHandler(this, uiHandler, control)

        val dirSpinner = findViewById(R.id.dir_selector) as Spinner
        val dirAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dirs.map { it.first })
        dirSpinner.adapter = dirAdapter
        dirSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                imageViewer?.select(dirs[position].second)
                videoViewer?.select(dirs[position].second)
            }
        }

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
        Log.i(TAG, "Start from intent $intent")
        imageViewer?.load()
        videoViewer?.load()
    }

    override fun onStart() {
        super.onStart()
        controller?.onStart()
    }

    override fun onStop() {
        controller?.onStop()
        super.onStop()
    }

    override fun onPause() {
        imageViewer?.onPause()
        videoViewer?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        imageViewer?.onResume()
        videoViewer?.onResume()
    }

    override fun onDestroy() {
        imageViewer?.onDestroy()
        videoViewer?.onDestroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = EyeballViewerActivity::class.java.simpleName
    }
}
