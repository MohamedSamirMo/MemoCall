package com.example.memocall

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.memocall.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val appId = "70707618d55b4ae4a838ac8147e8260d"
    private val cannelName = "memocall"
    private val token ="007eJxTYHg852f63fdxui09XQKKxq9X5k/X5ggWnBb65dOzY3390/UUGMwNgNDM0CLF1DTJJDHVJNHC2CIx2cLQxDzVwsjMIKUq8lhKQyAjw4PNxxkYoRDE52DITc3NT07MyWFgAAA1ESKN"
    private val uid = 0

    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null


    private val PERMISSION_ID = 12
    private val REQUESTED_PERMISSION =
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA

        )

    private fun CheckSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSION[1]
        ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun setUpVideoSdkEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()

        } catch (e: Exception) {
            showMessage(e.message!!)

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!CheckSelfPermission()) {
            ActivityCompat.requestPermissions(
                this, REQUESTED_PERMISSION, PERMISSION_ID
            )
        }
        setUpVideoSdkEngine()
        binding.join.setOnClickListener {
            JoinCall()
        }
        binding.leave.setOnClickListener {
            LeaveCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun LeaveCall() {

        if (!isJoined){
            showMessage("Joined channel first")
        }else{
            agoraEngine!!.leaveChannel()
            showMessage("you left channel")
            if (remoteSurfaceView!=null)remoteSurfaceView!!.visibility= GONE
            if (localSurfaceView!=null)localSurfaceView!!.visibility= GONE
            isJoined=false
        }
    }

    private fun JoinCall() {
        if (CheckSelfPermission()){
            val option=ChannelMediaOptions()
            option.channelProfile= Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType=Constants.CLIENT_ROLE_BROADCASTER
            setUpLocalVideo()
            localSurfaceView!!.visibility= VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token,cannelName,uid,option)
        }else{
            Toast.makeText(this,"permission not granted",Toast.LENGTH_SHORT).show()
        }

    }

    private val mRtcEventHandler: IRtcEngineEventHandler =
        object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                showMessage("Remote User joined $uid ")
                runOnUiThread { setUpRemoteVideo(uid) }

            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                isJoined = true
                showMessage("Joined Channel $channel")
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                showMessage("User Offline")
                runOnUiThread {
                    remoteSurfaceView!!.visibility=GONE
                }

            }

        }

    private fun setUpRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid
            )
        )
    }

    private fun setUpLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)

        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0
            )
        )
    }
}