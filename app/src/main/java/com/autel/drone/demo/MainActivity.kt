package com.autel.drone.demo

import android.os.Bundle
import android.util.Log

import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autel.drone.sdk.libbase.common.dsp.FileConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IMissionManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointGUIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointStatusReportNtfyBean
import com.autel.mission.demo.R

class MainActivity : AppCompatActivity() {

    private var missionManager: IMissionManager? = null

    //mission unique ID: generate mission file use timestamp as guid
    private var guid: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //get MissionManager
        missionManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getWayPointMissionManager()
        //Mission execute status report
        missionManager?.addWaypointMissionExecuteStateListener(object :CommonCallbacks.KeyListener<MissionWaypointStatusReportNtfyBean>{
            override fun onValueChange(oldValue: MissionWaypointStatusReportNtfyBean?, newValue: MissionWaypointStatusReportNtfyBean) {
                Log.d("Test", "$newValue")
            }
        })

        findViewById<Button>(R.id.upload).setOnClickListener {
            checkDroneConnect()

            // mock test data and get pathResultMission by algorithm
            val pathResultMission = AirLineCreator.getWaypointMissionPath()

            // generate mission file object
            val missionInfoJNI = AirLineWriter.writeMissionFile(pathResultMission!!)

            // set for mission file save path
            FileConstants.init(this)

            //save missionInfoJNI object to file, and upload to drone
            missionManager?.uploadMissionFile(missionInfoJNI, object: CommonCallbacks.CompletionCallbackWithProgressAndParam<Long> {
                override fun onProgressUpdate(progress: Double) {}
                override fun onSuccess(t: Long?) { guid = t }
                override fun onFailure(error: IAutelCode, msg: String?) {}
            })
        }

        //启动任务
        findViewById<Button>(R.id.start).setOnClickListener {
            checkDroneConnect()
            guid?.let { missionManager?.startMission(MissionWaypointGUIDBean(it.toInt()),
                    object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                        override fun onFailure(error: IAutelCode, msg: String?) {}
                        override fun onSuccess(t: Void?) {}
                    })
            }
        }

        findViewById<Button>(R.id.pause).setOnClickListener {
            checkDroneConnect()
            missionManager?.pauseMission(object: CommonCallbacks.CompletionCallbackWithParam<Void>{
                override fun onFailure(error: IAutelCode, msg: String?) {}
                override fun onSuccess(t: Void?) {}
            })
        }

        findViewById<Button>(R.id.resume).setOnClickListener {
            checkDroneConnect()
            guid?.let {
                missionManager?.resumeMission(MissionWaypointGUIDBean(it.toInt()), object: CommonCallbacks.CompletionCallbackWithParam<Void>{
                    override fun onFailure(error: IAutelCode, msg: String?) {}
                    override fun onSuccess(t: Void?) {}
                })
            }
        }

        findViewById<Button>(R.id.exit).setOnClickListener {
            checkDroneConnect()
            missionManager?.exitMission(object : CommonCallbacks.CompletionCallbackWithParam<Void>{
                override fun onFailure(error: IAutelCode, msg: String?) {}
                override fun onSuccess(t: Void?) {}
            })
        }
    }

    private fun checkDroneConnect(){
        if (DeviceManager.getDeviceManager().getFirstDroneDevice()?.isConnected() != true){
            Toast.makeText(this, "Drone is disconnected", Toast.LENGTH_LONG).show()
        }
    }
}