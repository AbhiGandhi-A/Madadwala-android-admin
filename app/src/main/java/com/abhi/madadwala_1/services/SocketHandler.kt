package com.abhi.madadwala_1.services

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    private var mSocket: Socket? = null
    private var currentUrl: String? = null

    @Synchronized
    fun setSocket(url: String) {
        if (currentUrl == url && mSocket != null) return
        
        try {
            mSocket?.disconnect()
            mSocket = IO.socket(url)
            currentUrl = url
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun getSocket(): Socket? {
        return mSocket
    }

    @Synchronized
    fun establishConnection() {
        mSocket?.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket?.disconnect()
    }
}
