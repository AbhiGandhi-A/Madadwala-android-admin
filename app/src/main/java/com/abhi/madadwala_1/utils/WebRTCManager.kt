package com.abhi.madadwala_1.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.*

class WebRTCManager(
    private val context: Context,
    private val onEmitSignaling: (String, JSONObject) -> Unit
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    fun startCall(targetId: String) {
        setupPeerConnection(targetId)
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            val data = JSONObject().apply {
                                put("to", targetId)
                                put("offer", sdp.description)
                                put("type", sdp.type.canonicalForm())
                            }
                            onEmitSignaling("offer", data)
                        }
                    }, sdp)
                }
            }
        }, constraints)
    }

    fun handleOffer(data: JSONObject) {
        try {
            val fromId = data.getString("from")
            val offerData = data.get("offer")
            val sdpString = when (offerData) {
                is JSONObject -> offerData.getString("sdp")
                is String -> offerData
                else -> offerData.toString()
            }
            
            setupPeerConnection(fromId)
            
            val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdpString)
            peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    drainIceCandidates()
                    val constraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    }
                    peerConnection?.createAnswer(object : SimpleSdpObserver() {
                        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                            sessionDescription?.let { answerSdp ->
                                peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                                    override fun onSetSuccess() {
                                        val answerData = JSONObject().apply {
                                            put("to", fromId)
                                            put("answer", answerSdp.description)
                                            put("type", answerSdp.type.canonicalForm())
                                        }
                                        onEmitSignaling("answer", answerData)
                                    }
                                }, answerSdp)
                            }
                        }
                    }, constraints)
                }
            }, sessionDescription)
        } catch (e: Exception) {
            Log.e("WebRTC", "Error handling offer", e)
        }
    }

    fun handleAnswer(data: JSONObject) {
        try {
            val answerData = data.get("answer")
            val sdpString = when (answerData) {
                is JSONObject -> answerData.getString("sdp")
                is String -> answerData
                else -> answerData.toString()
            }
            
            val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpString)
            peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    Log.d("WebRTC", "Remote description set (Answer)")
                    drainIceCandidates()
                }
            }, sessionDescription)
        } catch (e: Exception) {
            Log.e("WebRTC", "Error handling answer", e)
        }
    }

    fun handleIceCandidate(data: JSONObject) {
        try {
            // Handle both flat and nested candidate objects
            val (sdpMid, sdpMLineIndex, sdp) = if (data.has("sdpMid")) {
                Triple(
                    data.getString("sdpMid"),
                    data.getInt("sdpMLineIndex"),
                    data.getString("candidate")
                )
            } else {
                val cand = data.getJSONObject("candidate")
                Triple(
                    cand.getString("sdpMid"),
                    cand.getInt("sdpMLineIndex"),
                    cand.getString("candidate")
                )
            }

            val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
            if (peerConnection != null && peerConnection?.remoteDescription != null) {
                peerConnection?.addIceCandidate(candidate)
            } else {
                pendingIceCandidates.add(candidate)
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "Error handling ICE candidate", e)
        }
    }

    private fun drainIceCandidates() {
        pendingIceCandidates.forEach {
            peerConnection?.addIceCandidate(it)
        }
        pendingIceCandidates.clear()
    }

    private fun setupPeerConnection(targetId: String) {
        if (peerConnection != null) {
            Log.d("WebRTC", "PeerConnection already exists for target: $targetId")
            return
        }

        // Added more STUN servers and suggested TURN servers for different network support
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer()
            // NOTE: For reliable cross-network calls, you MUST add a TURN server here.
            // Example: PeerConnection.IceServer.builder("turn:your-turn-server.com")
            //          .setUsername("user").setPassword("pass").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val data = JSONObject().apply {
                        put("to", targetId)
                        put("candidate", it.sdp)
                        put("sdpMid", it.sdpMid)
                        put("sdpMLineIndex", it.sdpMLineIndex)
                    }
                    onEmitSignaling("ice_candidate", data)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d("WebRTC", "IceConnectionState: $state")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d("WebRTC", "Remote track added")
            }
        })

        // Ensure local audio is captured and added to the connection
        localAudioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", localAudioSource)
        localAudioTrack?.setEnabled(true)
        
        peerConnection?.addTrack(localAudioTrack, listOf("main_stream"))
        Log.d("WebRTC", "Local audio track added and enabled")
        
        // Ensure audio is routed to earpiece by default (not speaker)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }

    fun toggleSpeaker(isOn: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.isSpeakerphoneOn = isOn
    }

    fun toggleMic(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    @Synchronized
    fun stopCall() {
        if (peerConnection == null && localAudioTrack == null && localAudioSource == null) return

        peerConnection?.close()
        peerConnection = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        localAudioSource?.dispose()
        localAudioSource = null

        pendingIceCandidates.clear()
        
        // Reset audio settings
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) { Log.e("WebRTC", "SDP Create failure: $error") }
    override fun onSetFailure(error: String?) { Log.e("WebRTC", "SDP Set failure: $error") }
}
