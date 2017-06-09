package fi.haapatalo.android.eyeball

import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager

class ControllerHandler(context: Context, private val uiHandler: Handler, val control: EyeballController) {

    private val controllerListener = ControllerEventListener()
    private val controllerManager = ControllerManager(context, controllerListener)
    private val controller = controllerManager.controller

    init {
        controller.setEventListener(controllerListener)
    }

    fun onStart() = controllerManager.start()
    fun onStop() = controllerManager.stop()

    private inner class ControllerEventListener : Controller.EventListener(), ControllerManager.EventListener, Runnable {

        override fun onApiStatusChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onConnectionStateChanged(state: Int) {
            uiHandler.post(this)
        }

        override fun onRecentered() {
            Log.d(Viewer.TAG, "Recentered")
        }

        override fun onUpdate() {
            uiHandler.post(this)
        }

        private var clickButtonState = false
        private var appButtonState = false

        fun trackChange(cur: Boolean, new: Boolean, runIfChangedToTrue: () -> Unit, message: String): Boolean {
            if (new && !cur) {
                Log.i(Viewer.TAG, message)
                runIfChangedToTrue()
            }
            return new
        }

        override fun run() {
            controller.update()

            appButtonState = trackChange(appButtonState, controller.appButtonState,
                    { control.previous() }, "Switching to previous image")
            clickButtonState = trackChange(clickButtonState, controller.clickButtonState,
                    { control.next() }, "Switching to next image")
        }
    }
}

interface EyeballController {
    enum class MediaType { IMAGE, VIDEO }
    fun activateMedia(type: MediaType): Unit
    fun next(): Unit
    fun previous(): Unit
}
