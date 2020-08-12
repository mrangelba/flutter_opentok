package com.flutter_opentok

import android.content.Context

class VoIPProviderDelegateImpl(override val context: Context,
                               override val view: FlutterOpenTokView,
                               override val methodCallHandler: MethodCallHandlerImpl) : VoIPProviderDelegate {


    override fun willConnect() {
        methodCallHandler.channelInvokeMethod("onWillConnect", null)
    }

    override fun didConnect() {
        view.configureAudioSession()
        view.refreshViews()
        methodCallHandler.channelInvokeMethod("onSessionConnect", null)
    }

    override fun didDisconnect() {
        methodCallHandler.channelInvokeMethod("onSessionDisconnect", null)
    }

    override fun didReceiveVideo() {
        if (FlutterOpentokPlugin.loggingEnabled) {
            print("[FlutterOpenTokView] Receive video")
        }
        view.refreshViews()
        methodCallHandler.channelInvokeMethod("onReceiveVideo", null)
    }

    override fun didCreateStream() {
        methodCallHandler.channelInvokeMethod("onCreateStream", null)
    }

    override fun didCreatePublisherStream() {
        methodCallHandler.channelInvokeMethod("onCreatePublisherStream", null)
    }

    override fun didDropStream() {
        methodCallHandler.channelInvokeMethod("onDroppedStream", null)

        view.refreshViews()
    }
}