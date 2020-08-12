package com.flutter_opentok

import android.util.Log
import android.content.Context
import android.view.View
import com.opentok.android.*

interface VoIPProviderDelegate {
    fun willConnect()
    fun didConnect()
    fun didDisconnect()
    fun didReceiveVideo()
    fun didCreateStream()
    fun didDropStream()
    fun didCreatePublisherStream()

    val context: Context
}

interface VoIPProvider {
    /// Whether VoIP connection has been established.
    val isConnected: Boolean

    // Set whether publisher has audio or not.
    var isAudioOnly: Boolean

    fun connect(apiKey: String, sessionId: String, token: String)
    fun disconnect()

    fun mutePublisherAudio()
    fun unmutePublisherAudio()

    fun muteSubscriberAudio()
    fun unmuteSubscriberAudio()

    fun enablePublisherVideo()
    fun disablePublisherVideo()

    fun switchCamera()
}

class OpenTokVoIPImpl(
        var delegate: VoIPProviderDelegate?,
        var publisherSettings: PublisherSettings?) : VoIPProvider, Session.SessionListener, PublisherKit.PublisherListener {

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private var videoReceived: Boolean = false

    val subscriberView: View?
        get() {
            return subscriber?.view
        }

    val publisherView: View?
        get() {
            return publisher?.view
        }

    var publishVideo: Boolean
        get() {
            return publisher?.publishVideo!!
        }
        set(value) {
            publisher?.publishVideo = value
        }

    /// VoIPProvider

    override val isConnected: Boolean
        get() {
            return session?.connection != null
        }

    override var isAudioOnly: Boolean
        get() {
            return !publishVideo
        }
        set(value) {
            publishVideo = !value
        }

    override fun connect(apiKey: String, sessionId: String, token: String) {
        delegate?.willConnect()

        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[OpenTokVoIPImpl] Create OTSession")
            Log.d("[VOIPProvider]", "[OpenTokVoIPImpl] API key: $apiKey")
            Log.d("[VOIPProvider]", "[OpenTokVoIPImpl] Session ID: $sessionId")
            Log.d("[VOIPProvider]", "[OpenTokVoIPImpl] Token: $token")
        }

        if (apiKey == "" || sessionId == "" || token == "") {
            return
        }

        session = Session.Builder(delegate?.context, apiKey, sessionId).build()
        session?.setSessionListener(this)
        session?.connect(token)
    }

    override fun disconnect() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Disconnecting from session")
        }

        session?.disconnect()
    }

    override fun mutePublisherAudio() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Mute publisher audio")
        }

        publisher?.publishAudio = false
    }

    override fun unmutePublisherAudio() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "UnMute publisher audio")
        }

        publisher?.publishAudio = true
    }

    override fun muteSubscriberAudio() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Mute subscriber audio")
        }

        subscriber?.subscribeToAudio = false
    }

    override fun unmuteSubscriberAudio() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "UnMute subscriber audio")
        }

        subscriber?.subscribeToAudio = true
    }

    override fun enablePublisherVideo() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Enable publisher video")
        }

        publisher?.publishVideo = true
    }

    override fun disablePublisherVideo() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Disable publisher video")
        }

        publisher?.publishVideo = false
    }

    override fun switchCamera() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "Switch Camera")
        }

        publisher?.cycleCamera()
    }

    /// SessionListener
    override fun onConnected(session: Session?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[SessionListener] onConnected")
        }
        publish()
        delegate?.didConnect()
    }

    override fun onDisconnected(session: Session?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[SessionListener] onDisconnected")
        }

        unsubscribe()
        unpublish()
        this.session = null
        videoReceived = false

        delegate?.didDisconnect()
    }

    override fun onStreamDropped(session: Session?, stream: Stream?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[SessionListener] onStreamDropped")
        }
        unsubscribe()
        delegate?.didDropStream()
    }

    override fun onStreamReceived(session: Session?, stream: Stream?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[SessionListener] onStreamReceived")
        }
        stream?.let { subscribe(it) }
        delegate?.didCreateStream()
        if (stream?.hasVideo() == true) {
            delegate?.didReceiveVideo()
        }
    }

    override fun onError(session: Session?, error: OpentokError?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[SessionListener] onError ${error?.message}")
        }
    }

    /// PublisherListener

    override fun onStreamCreated(p0: PublisherKit?, p1: Stream?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[PublisherListener] onStreamCreated")
        }
        delegate?.didCreatePublisherStream()
    }

    override fun onStreamDestroyed(p0: PublisherKit?, p1: Stream?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[PublisherListener] onStreamDestroyed")
        }
        unpublish()
    }

    override fun onError(p0: PublisherKit?, error: OpentokError?) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[PublisherListener] onError ${error?.message}")
        }
    }

    /// Private

    fun publish() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[VOIPProvider] publish")
        }

        publisher = Publisher.Builder(delegate?.context)
                .audioTrack(publisherSettings?.audioTrack ?: true)
                .videoTrack(publisherSettings?.videoTrack ?: true)
                .audioBitrate(publisherSettings?.audioBitrate ?: 400000)
                .frameRate(Publisher.CameraCaptureFrameRate.FPS_30)
                .resolution(Publisher.CameraCaptureResolution.HIGH)
                .build()

        publisher?.setPublisherListener(this)
        publisher?.publishVideo = false
        session?.publish(publisher)
    }

    fun unpublish() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[VOIPProvider] unpublish")
        }
        if (publisher != null) {
            session?.unpublish(publisher)
            publisher = null
        }
    }

    fun subscribe(stream: Stream) {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", "[VOIPProvider] subscribe")
        }

        subscriber = Subscriber.Builder(delegate?.context, stream).build()
        session?.subscribe(subscriber)
    }

    fun unsubscribe() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            Log.d("[VOIPProvider]", " unsubscribe")
        }

        if (subscriber != null) {
            subscriber = null
        }

    }
}
