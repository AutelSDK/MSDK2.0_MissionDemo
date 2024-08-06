package com.autel.drone.demo.kmz

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
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionKmlGUIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointStatusReportNtfyBean
import com.autel.mission.demo.R


/**
 *
 */

class KMZActivity : AppCompatActivity() {

    private var missionManager: IMissionManager? = null


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

        //mission unique ID: generate mission file use timestamp as guid
        val guid = System.currentTimeMillis()/ 1000

        //upload kmz file
        findViewById<Button>(R.id.upload).setOnClickListener {
            checkDroneConnect()
            val kmzFilePath = "testPath"
            //set for mission file save path
            FileConstants.init(this)
            missionManager?.uploadKmzMissionFile(kmzFilePath, guid.toInt(), object: CommonCallbacks.CompletionCallbackWithProgressAndParam<Long> {
                override fun onProgressUpdate(progress: Double) {}
                override fun onSuccess(guid: Long?) {  }
                override fun onFailure(error: IAutelCode, msg: String?) {}
            })
        }

        //start mission , param is MissionKmlGUIDBean
        findViewById<Button>(R.id.start).setOnClickListener {
            checkDroneConnect()
            guid.let { missionManager?.startMission(MissionKmlGUIDBean(it.toInt()),
                object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                    override fun onFailure(error: IAutelCode, msg: String?) {}
                    override fun onSuccess(t: Void?) {}
                })
            }
        }


        //pause mission
        findViewById<Button>(R.id.pause).setOnClickListener {
            checkDroneConnect()
            val isKml = true
            missionManager?.pauseMission(object: CommonCallbacks.CompletionCallbackWithParam<Void>{
                override fun onFailure(error: IAutelCode, msg: String?) {}
                override fun onSuccess(t: Void?) {}
            }, isKml)
        }


        //resume mission
        findViewById<Button>(R.id.resume).setOnClickListener {
            checkDroneConnect()
            guid.let {
                missionManager?.resumeMission(MissionKmlGUIDBean(it.toInt()), object: CommonCallbacks.CompletionCallbackWithParam<Void>{
                    override fun onFailure(error: IAutelCode, msg: String?) {}
                    override fun onSuccess(t: Void?) {}
                })
            }
        }

        //exit mission
        findViewById<Button>(R.id.exit).setOnClickListener {
            checkDroneConnect()
            val isKml = true
            missionManager?.exitMission(object : CommonCallbacks.CompletionCallbackWithParam<Void>{
                override fun onFailure(error: IAutelCode, msg: String?) {}
                override fun onSuccess(t: Void?) {}
            },isKml)
        }
    }

    private fun checkDroneConnect(){
        if (DeviceManager.getDeviceManager().getFirstDroneDevice()?.isConnected() != true){
            Toast.makeText(this, "Drone is disconnected", Toast.LENGTH_LONG).show()
        }
    }
}