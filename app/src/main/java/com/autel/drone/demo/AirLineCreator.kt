package com.autel.drone.demo

import android.text.TextUtils

import com.autel.drone.sdk.algor.AlgorithmManager
import com.autel.drone.sdk.algor.bean.AutelCoordinate3D

import com.autel.sdk.mission.wp.CameraActionJNI
import com.autel.sdk.mission.wp.PathMission
import com.autel.sdk.mission.wp.PathPoint
import com.autel.sdk.mission.wp.PathResultMission

/**
 * 航线规划转换类，主要是对[AlgorithmManager]相关方法的封装，将ui数据转换为算法输入数据
 *
 * Created at 2022/9/29
 */
class AirLineCreator {

    companion object {

        fun mockData() : WaypointMissionModel{
            var waypointMissionModel = WaypointMissionModel()

            var waypointList = mutableListOf<WaypointModel>()
            waypointMissionModel.waypointList = waypointList
            //航点坐标集合:目前最多500
            var i = 0
            while (i in 0..100){
                i++
                var latLng = AutelLatLng()
                latLng.latitude = 113.0003 * i+ 0.005
                latLng.longitude = 24.0003 * i+ 0.005
                latLng.altitude = 60.0
                var waypoint = createWaypoint(latLng, waypointMissionModel)
                waypointList.add(waypoint)
            }
            return waypointMissionModel
        }

        protected fun createWaypoint(latLng: AutelLatLng, missionModel: WaypointMissionModel): WaypointModel {
            val waypointModel = WaypointModel()
            waypointModel.latitude = latLng.latitude
            waypointModel.longitude = latLng.longitude
            waypointModel.altitude = latLng.altitude.toDouble()
            waypointModel.height = missionModel.routeHeight!!
            waypointModel.speed = missionModel.routeSpeed?.toDouble()!!
            waypointModel.droneHeadingControl = missionModel.droneHeadingControl!!
            waypointModel.droneYaw = missionModel.droneYaw!!
            val actionList = arrayListOf<MissionActionModel>()
            //创建航点时，默认添加一个航段动作，跟随航线
            val missionActionModel = MissionActionModel()
            missionActionModel.cameraAction = missionModel.cameraActionType!!
            missionActionModel.cameraDistance = missionModel.cameraDistance?.toFloat()!!
            missionActionModel.cameraInterval =missionModel.cameraInterval!!
            missionActionModel.gimbalPitch = missionModel.gimbalPitch!!
            actionList.add(missionActionModel)
            waypointModel.missingActions = actionList
            return waypointModel
        }

        /**
         * 航点任务规划算法
         *
         * 将用户界面输入调用规划算法生成飞行轨迹
         * 逻辑同步于ModelB的PathPlaningUtils#getWaypointMissionPath方法
         */
        fun getWaypointMissionPath(): PathResultMission? {

            var waypointMissionModel = mockData()
            val interestingPoints = mutableListOf<Coordinate3DModel>()

            val waypointModel = waypointMissionModel.waypointList!![0]

            val mission = PathMission()
            mission.default_R_flag = 0
            mission.HomeLLA = getHomePoint(waypointModel)
            mission.WPNum = waypointMissionModel.waypointList!!.size.toShort()
            val wPInfoList = arrayOfNulls<PathPoint>(waypointMissionModel.waypointList!!.size)
            mission.WP_Info_strc = wPInfoList

            waypointMissionModel.waypointList!!.forEachIndexed { index, waypoint ->
                val cameraActionSize: Int = waypoint.missingActions.size

                val point = PathPoint()
                //1 代表停下的点，2 代表协调转弯
                point.WPTypeUsr = 1
                point.WPLLAUsr = doubleArrayOf(
                    waypoint.latitude,
                    waypoint.longitude,
                    waypoint.height.toDouble()
                )
                point.RadUsr = waypoint.turnRadius
                point.VelRefUsr = waypoint.speed

                point.Heading_Mode = waypoint.droneHeadingControl.toShort()
                point.POI_Valid = -1

                var interestUuid = waypoint.interestUuid
                var interestPoint: Coordinate3DModel? = null

                if(!TextUtils.isEmpty(interestUuid)) {
                    interestingPoints.forEach {
                        if (it.uuid == interestUuid) {
                            interestPoint = it
                        }
                    }
                }

                if (interestPoint != null) {
                    point.POI_Valid = 1
                    point.POIUsr = doubleArrayOf(
                        interestPoint!!.latitude,
                        interestPoint!!.longitude,
                        interestPoint!!.altitude
                    )
                }
                point.ActionNum = cameraActionSize.toShort()
                if (cameraActionSize > 0) {
                    val actionJNIList = arrayOfNulls<CameraActionJNI>(cameraActionSize)
                    var idx = 0
                    for (j in 0 until cameraActionSize) {
                        val actionItem = waypoint.missingActions[j]
                        val actionJNI = CameraActionJNI()
                        val actionType: Int = actionItem.cameraAction

                        actionJNI.Action_Type = actionType
                        actionJNI.Gimbal_Pitch = actionItem.gimbalPitch
                        actionJNI.Gimbal_Roll = actionItem.cameraRoll
                        actionJNI.Action_Yaw_Ref = actionItem.droneYaw
                        actionJNI.Shoot_Time_Interval = actionItem.cameraInterval
                        actionJNI.Shoot_Dis_Interval = actionItem.cameraDistance
                        actionJNI.Action_Time = actionItem.actionTimeLen
                        actionJNI.Zoom_Rate = actionItem.zoom
                        actionJNI.reserved = intArrayOf(0, 0)
                        if (actionType <= 10) {
                            //因为航线动作只可以有一个，所以用这种方式强行将航线动作放在最后
                            actionJNIList[cameraActionSize - 1] = actionJNI
                        } else {
                            actionJNIList[idx++] = actionJNI
                        }
                    }
                    point.MSN_ActionInfo = actionJNIList
                }
                wPInfoList[index] = point
            }
            val result = AlgorithmManager.getInstance().getWaypointPath(mission)
            return result
        }

        private fun getHomePoint(waypointModel: WaypointModel): DoubleArray {
            val homPoint = DoubleArray(3)
            homPoint[0] = waypointModel.latitude
            homPoint[1] = waypointModel.longitude
            homPoint[2] = waypointModel.height.toDouble()
            return homPoint
        }

        private fun getDronePoint(droneLocation: AutelCoordinate3D?): DoubleArray? {
            if (droneLocation == null) {
                return null
            }
            val drone = DoubleArray(3)
            drone[0] = droneLocation.latitude
            drone[1] = droneLocation.longitude
            drone[2] = droneLocation.altitude
            return drone
        }

    }
}




/**
 * 航段动作，小于10的都是航段动作
 */
private fun WaypointModel.getSegmentAction(): MissionActionModel? {
    for (missionAction in missingActions) {
        if (missionAction.cameraAction < CameraActionEnum.LEG_NONE.value) {
            return missionAction
        }
    }
    return null
}


private fun WaypointModel.getDroneHead(): Int {
    val segmentAction = getSegmentAction()
    return segmentAction?.droneHeadingControl ?: DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value
}