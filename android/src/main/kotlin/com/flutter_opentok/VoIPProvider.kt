package com.flutter_opentok

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
