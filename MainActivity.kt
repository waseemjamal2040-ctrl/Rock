package com.example.nearbychat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nearbychat.model.Message
import com.example.nearbychat.ui.MessageAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvPeers: TextView
    private lateinit var rv: RecyclerView
    private lateinit var et: EditText
    private lateinit var btnSend: Button
    private lateinit var btnHost: Button
    private lateinit var btnJoin: Button
    private lateinit var btnStop: Button

    private lateinit var adapter: MessageAdapter
    private lateinit var nearby: NearbyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvPeers = findViewById(R.id.tvPeers)
        rv = findViewById(R.id.rvMessages)
        et = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnHost = findViewById(R.id.btnHost)
        btnJoin = findViewById(R.id.btnJoin)
        btnStop = findViewById(R.id.btnStop)

        adapter = MessageAdapter(mutableListOf())
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        nearby = NearbyManager(
            context = this,
            roomName = getString(R.string.room_name),
            onStatus = { setStatus(it) },
            onPeersChanged = { n -> setPeers(n) },
            onMessage = { msg -> addMsg(msg, mine = false) }
        )

        btnHost.setOnClickListener { if (ensurePerms()) nearby.host() }
        btnJoin.setOnClickListener { if (ensurePerms()) nearby.join() }
        btnStop.setOnClickListener { nearby.stopAll() }

        btnSend.setOnClickListener { sendCurrent() }
        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendCurrent(); true } else false
        }

        ensurePerms()
    }

    private fun sendCurrent() {
        val t = et.text.toString().trim()
        if (t.isNotEmpty()) {
            nearby.sendToAll(t)
            addMsg(t, mine = true)
            et.setText("")
        }
    }

    private fun addMsg(text: String, mine: Boolean) {
        runOnUiThread {
            adapter.add(Message(text, mine))
            rv.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun setStatus(s: String) {
        runOnUiThread { tvStatus.text = "الحالة: $s" }
    }

    private fun setPeers(n: Int) {
        runOnUiThread { tvPeers.text = "المتصلون: $n" }
    }

    private fun ensurePerms(): Boolean {
        val req = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 30) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                req += Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                req += Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                req += Manifest.permission.BLUETOOTH_SCAN
        }
        return if (req.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, req.toTypedArray(), 99)
            false
        } else true
    }
}