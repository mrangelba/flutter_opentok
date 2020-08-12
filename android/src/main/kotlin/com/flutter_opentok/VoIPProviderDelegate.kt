package com.flutter_opentok

import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

interface VoIPProviderDelegate {
    fun willConnect()
    fun didConnect()
    fun didDisconnect()
    fun didReceiveVideo()
    fun didCreateStream()
    fun didDropStream()
    fun didCreatePublisherStream()

    val context: Context
    val view: PlatformView
    val methodCallHandler: MethodChannel.MethodCallHandler
}
