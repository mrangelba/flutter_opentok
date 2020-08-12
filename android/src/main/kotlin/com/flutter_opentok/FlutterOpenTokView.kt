package com.flutter_opentok

import android.content.Context
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.opentok.android.AudioDeviceManager
import com.opentok.android.BaseAudioDevice
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.platform.PlatformView
import kotlinx.serialization.json.Json

class FlutterOpenTokView(
        registrar: PluginRegistry.Registrar,
        var context: Context,
        var viewId: Int,
        args: Any?) : PlatformView, View.OnTouchListener {

    private var delegate: VoIPProviderDelegate? = null
    private val openTokView: FrameLayout

    var methodCallHandler: MethodCallHandlerImpl? = null
    var publisherSettings: PublisherSettings? = null
    var switchedToSpeaker: Boolean = true
    var provider: VoIPProvider? = null
    var screenHeight: Int = LinearLayout.LayoutParams.MATCH_PARENT
    var screenWidth: Int = LinearLayout.LayoutParams.MATCH_PARENT
    var publisherHeight: Int = 500
    var publisherWidth: Int = 350
    var messenger: BinaryMessenger = registrar.messenger()

    init {
        val arguments: Map<*, *>? = args as? Map<*, *>
        if (arguments?.containsKey("height") == true)
            screenHeight = arguments?.get("height") as Int
        if (arguments?.containsKey("width") == true)
            screenWidth = arguments?.get("width") as Int

        if (arguments?.containsKey("publisherHeight") == true)
            publisherHeight = arguments?.get("publisherHeight") as Int
        if (arguments?.containsKey("publisherWidth") == true)
            publisherWidth = arguments?.get("publisherWidth") as Int

        openTokView = FrameLayout(context)
        openTokView.layoutParams = LinearLayout.LayoutParams(screenWidth, screenHeight)
        openTokView.setBackgroundColor(Color.TRANSPARENT)

        val publisherArg = arguments?.get("publisherSettings") as? String
        try {
            publisherSettings = publisherArg?.let { Json.parse(PublisherSettings.serializer(), it) }
        } catch (e: Exception) {
            if (FlutterOpentokPlugin.loggingEnabled) {
                print("OpenTok publisher settings error: ${e.message}")
            }
        }

        if (FlutterOpentokPlugin.loggingEnabled) {
            print("[FlutterOpenTokViewController] initialized")
        }
    }

    fun setup() {
        methodCallHandler = MethodCallHandlerImpl(viewId, messenger, this)

        delegate = VoIPProviderDelegateImpl(context, this, methodCallHandler!!)

        // Create VoIP provider
        createProvider()
    }

    override fun getView(): View {
        return openTokView
    }

    override fun dispose() {

    }

    fun configureAudioSession() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            print("[FlutterOpenTokViewController] Configure audio session")
            print("[FlutterOpenTokViewController] Switched to speaker = $switchedToSpeaker")
        }

        if (switchedToSpeaker) {
            AudioDeviceManager.getAudioDevice().setOutputMode(BaseAudioDevice.OutputMode.SpeakerPhone)
        } else {
            AudioDeviceManager.getAudioDevice().setOutputMode(BaseAudioDevice.OutputMode.Handset)
        }
    }

    // Convenience getter for current video view based on provider implementation
    val subscriberView: View?
        get() {
            if (provider is VoIPProviderImpl)
                return (provider as VoIPProviderImpl).subscriberView
            return null
        }

    val publisherView: View?
        get() {
            if (provider is VoIPProviderImpl)
                return (provider as VoIPProviderImpl).publisherView
            return null
        }

    /** Create an instance of VoIPProvider. This is what implements VoIP for the application.*/
    private fun createProvider() {
        provider = VoIPProviderImpl(delegate, publisherSettings)
    }

    fun refreshViews() {
        if (openTokView.childCount > 0) {
            openTokView.removeAllViews()
        }

        if (subscriberView != null) {
            val subView: View = subscriberView!!
            openTokView.addView(subView)

            if (subView is GLSurfaceView) {
                (subView as GLSurfaceView).setZOrderOnTop(true)
            }
        }

        if (provider?.isAudioOnly == false && publisherView != null && subscriberView != null) {
            val pubView: View = publisherView!!
            openTokView.addView(pubView)
            pubView.setOnTouchListener(this)
            val layout = FrameLayout.LayoutParams(publisherWidth, publisherHeight, Gravity.TOP or Gravity.RIGHT)
            layout.setMargins(0,0,0,0)
            pubView.layoutParams = layout
            if (pubView is GLSurfaceView) {
                (pubView as GLSurfaceView).setZOrderOnTop(true)
            }
        }

        if (publisherView != null && subscriberView == null) {
            val pubView: View = publisherView!!
            openTokView.addView(pubView)
            pubView.setOnTouchListener(null)

            val layout = FrameLayout.LayoutParams(screenWidth, screenWidth, Gravity.TOP or Gravity.LEFT)
            layout.setMargins(20,20,20,20)
            pubView.layoutParams = layout

            if (pubView is GLSurfaceView) {
                (pubView as GLSurfaceView).setZOrderOnTop(true)
            }
        }
    }

    /// TouchListener
    var dX: Float = 0F
    var dY: Float = 0F
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = view!!.x - event.rawX
                dY = view.y - event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                var newX = event.rawX + dX
                if (newX < 0)
                    newX = 0F
                if (newX > openTokView.width - view!!.width)
                    newX = (openTokView.width - view!!.width).toFloat()

                var newY = event.rawY + dY
                if (newY < 0)
                    newY = 0F
                if (newY > openTokView.height - view!!.height)
                    newY = (openTokView.height - view!!.height).toFloat()

                view!!.animate()
                        .x(newX)
                        .y(newY)
                        .setDuration(0)
                        .start()
            }
            else -> return false
        }
        return true
    }
}