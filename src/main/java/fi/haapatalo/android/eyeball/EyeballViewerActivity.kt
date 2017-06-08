package fi.haapatalo.android.eyeball

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Spinner
import com.google.vr.sdk.widgets.pano.VrPanoramaView

class EyeballViewerActivity : Activity() {

    // The various events we need to handle happen on arbitrary threads. They need to be reposted to
    // the UI thread in order to manipulate the TextViews. This is only required if your app needs to
    // perform actions on the UI thread in response to controller events.
    private val uiHandler = Handler()

    // All state is managed in the viewer class that maintains non-null values
    private var viewer: ImageViewer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val widgetView = findViewById(R.id.pano_view) as VrPanoramaView
        val dirSpinner = findViewById(R.id.dir_selector) as Spinner
        viewer = ImageViewer(this, uiHandler, widgetView, dirSpinner)

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
