package com.example.nearbychat

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class NearbyManager(
    private val context: Context,
    private val roomName: String,
    private val onStatus: (String) -> Unit,
    private val onPeersChanged: (Int) -> Unit,
    private val onMessage: (String) -> Unit
) {
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.nearbychat.SERVICE"
    private val client = Nearby.getConnectionsClient(context)
    private val peers = ConcurrentHashMap<String, String>()

    private var isHosting = false
    private var isDiscovering = false
    private var localName = android.os.Build.MODEL

    fun host() {
        stopAll()
        isHosting = true
        val opts = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(roomName, serviceId, lifecycle, opts)
            .addOnSuccessListener { onStatus("يتم استضافة الغرفة «$roomName»…") }
            .addOnFailureListener { onStatus("فشل الاستضافة: ${it.message}") }
    }

    fun join() {
        stopAll()
        isDiscovering = true
        val opts = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, discovery, opts)
            .addOnSuccessListener { onStatus("يتم البحث عن «$roomName»…") }
            .addOnFailureListener { onStatus("فشل البحث: ${it.message}") }
    }

    fun stopAll() {
        if (isHosting) client.stopAdvertising()
        if (isDiscovering) client.stopDiscovery()
        client.stopAllEndpoints()
        isHosting = false; isDiscovering = false
        peers.clear(); onPeersChanged(0)
        onStatus("تم الإيقاف")
    }

    fun sendToAll(text: String) {
        val payload = Payload.fromBytes(text.toByteArray(StandardCharsets.UTF_8))
        client.sendPayload(peers.keys.toList(), payload)
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
            onStatus("طلب اتصال من ${connectionInfo.endpointName}")
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                peers[endpointId] = endpointId
                onPeersChanged(peers.size)
                onStatus("اتصل: ${endpointId.take(4)}")
            } else onStatus("رفض الاتصال/فشل")
        }
        override fun onDisconnected(endpointId: String) {
            peers.remove(endpointId)
            onPeersChanged(peers.size)
            onStatus("انقطع: ${endpointId.take(4)}")
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.endpointName == roomName) {
                client.requestConnection(localName, endpointId, lifecycle)
                    .addOnSuccessListener { onStatus("إرسال طلب انضمام لـ «$roomName»…") }
                    .addOnFailureListener { onStatus("تعذر طلب الانضمام: ${it.message}") }
            }
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val text = payload.asBytes()!!.toString(StandardCharsets.UTF_8)
                onMessage(text)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}