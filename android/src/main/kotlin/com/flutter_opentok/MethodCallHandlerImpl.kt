package com.flutter_opentok

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MethodCallHandlerImpl(viewId: Int, messenger: BinaryMessenger, var view: FlutterOpenTokView) : MethodChannel.MethodCallHandler {
    var channel: MethodChannel

    init {
        val channelName = "plugins.indoor.solutions/opentok_$viewId"
        channel = MethodChannel(messenger, channelName)

        // Listen for method calls from Dart.
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "create") {
            if (call.arguments == null) return

            val methodArgs = call.arguments as? Map<String, Any>
            val apiKey = methodArgs?.get("apiKey") as? String
            val sessionId = methodArgs?.get("sessionId") as? String
            val token = methodArgs?.get("token") as? String

            if (apiKey != null && sessionId != null && token != null) {
                view.provider?.connect(apiKey, sessionId, token)
                result.success(null)
            } else {
                result.error("CREATE_ERROR", "Android could not extract flutter arguments in method: (create)", "")
            }
        } else if (call.method == "destroy") {
            view.provider?.disconnect()
            result.success(null)
        } else if (call.method == "enablePublisherVideo") {
            view.provider?.enablePublisherVideo()
            view.refreshViews()
            result.success(null)
        } else if (call.method == "disablePublisherVideo") {
            view.provider?.disablePublisherVideo()
            view.refreshViews()
            result.success(null)
        } else if (call.method == "unmutePublisherAudio") {
            view.provider?.unmutePublisherAudio()
            result.success(null)
        } else if (call.method == "mutePublisherAudio") {
            view.provider?.mutePublisherAudio()
            result.success(null)
        } else if (call.method == "muteSubscriberAudio") {
            view.provider?.muteSubscriberAudio()
            result.success(null)
        } else if (call.method == "unmuteSubscriberAudio") {
            view.provider?.unmuteSubscriberAudio()
            result.success(null)
        } else if (call.method == "switchAudioToSpeaker") {
            view.switchedToSpeaker = true
            view.configureAudioSession()
            result.success(null)
        } else if (call.method == "switchAudioToReceiver") {
            view.switchedToSpeaker = false
            view.configureAudioSession()
            result.success(null)
        } else if (call.method == "getSdkVersion") {
            result.success("1")
        } else if (call.method == "switchCamera") {
            view.provider?.switchCamera()
            result.success(null)
        } else {
            result.notImplemented()
        }
    }

    fun channelInvokeMethod(method: String, arguments: Any?) {
        channel.invokeMethod(method, arguments, object : MethodChannel.Result {
            override fun notImplemented() {
                if (FlutterOpentokPlugin.loggingEnabled) {
                    print("Method $method is not implemented")
                }
            }

            override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
                if (FlutterOpentokPlugin.loggingEnabled) {
                    print("Method $method failed with error $errorMessage")
                }
            }

            override fun success(result: Any?) {
                if (FlutterOpentokPlugin.loggingEnabled) {
                    print("Method $method succeeded")
                }
            }

        })
    }

}