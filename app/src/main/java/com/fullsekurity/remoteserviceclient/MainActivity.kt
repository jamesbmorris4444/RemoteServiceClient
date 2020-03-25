package com.fullsekurity.remoteserviceclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private var mServiceConnected = false
    private var mTimestampText: TextView? = null

    companion object {
        const val MSG_GET_TIMESTAMP = 1000
    }

    private var timeStampRequestMessenger: Messenger? = null
    private var timeStampReceiveMessenger: Messenger? = null

    private lateinit var serviceIntent: Intent

    private var mServiceConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            timeStampRequestMessenger = null
            timeStampReceiveMessenger = null
            mServiceConnected = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            timeStampRequestMessenger =  Messenger(service);
            timeStampReceiveMessenger =  Messenger(ReceiveRandomNumberHandler(this@MainActivity))
            mServiceConnected = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTimestampText = findViewById<View>(R.id.timestamp_text) as TextView
        val printTimestampButton = findViewById<View>(R.id.print_timestamp) as Button
        val bindServiceButton = bind_service
        val unbindServiceButton = unbind_service

        serviceIntent = Intent()
        serviceIntent.component = ComponentName("com.fullsekurity.remoteserviceserver", "com.fullsekurity.remoteserviceserver.BoundService")

        bindServiceButton.setOnClickListener {
            mServiceConnection?. let {
                bindService(serviceIntent, it, Context.BIND_AUTO_CREATE)
            }
        }
        printTimestampButton.setOnClickListener {
            if (mServiceConnected) {
                try {
                    val msg = Message.obtain(null, MSG_GET_TIMESTAMP)
                    msg.replyTo = timeStampReceiveMessenger
                    timeStampRequestMessenger?.send(msg)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
        unbindServiceButton.setOnClickListener {
            if (mServiceConnected) {
                mServiceConnection?. let {
                    unbindService(it)
                }
                mServiceConnected = false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (mServiceConnected) {
            mServiceConnection?. let {
                unbindService(it)
            }
            mServiceConnected = false
        }
    }

    internal class ReceiveRandomNumberHandler(activity: MainActivity) : Handler() {
        private val mActivity = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_GET_TIMESTAMP -> {
                    mActivity.get()?.mTimestampText?.text = msg.data.getString("timestamp")}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceConnection = null
    }

}
