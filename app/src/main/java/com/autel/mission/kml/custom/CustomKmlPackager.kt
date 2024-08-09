package com.autel.mission.kml.custom

import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.ActionGroup
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.DroneInfo
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Folder
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.GlobalWaypointHeadingParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.HomePoint
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Kml
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.PayloadInfo
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.PayloadParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Placemark
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.WaylineCoordinateSysParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.WaypointHeadingParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.WaypointTurnParam

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 *
 * 封装 template.kml
 */
class CustomKmlPackager : CustomBasePackager() {
    companion object{
        var TAG = "CustomKmlPackager-KML"
    }

    override fun pack(model: MissionPath): Kml {
        packMissionCfg(model)
        transformToKml(model)
        return kmlBean
    }

    override fun transformToKml(model: MissionPath) {
        val folderList: MutableList<Folder> = mutableListOf()
        document.folders = folderList
        val templateType = "waypoint"
        getFolder(model, templateType,).let { folderList.add(it) }
    }

    override fun packMissionCfg(model: MissionPath){

        //起飞安全高度
        missionConfig.takeOffSecurityHeight = model.parameters.uav_height_ground.toFloat()
        //全局速度
        missionConfig.globalTransitionalSpeed = model.parameters.uav_speed_high.toString()

        document.createTime = System.currentTimeMillis()
        document.updateTime =  System.currentTimeMillis()

        missionConfig.flyToWaylineMode = "safely"

        val finishActionType =  MissionFinishActionType.GO_HOME.value
        if (finishActionType == MissionFinishActionType.GO_HOME.value) {
            missionConfig.finishAction = "goHome"
        } else {
            missionConfig.finishAction = "noAction"
        }

        val lostConnectAction = MissionLostConnectActionType.GO_ON_MISSION.value
        if (lostConnectAction == MissionLostConnectActionType.GO_ON_MISSION.value) {
            missionConfig.executeRCLostAction = "goContinue"
            missionConfig.exitOnRCLost = "goContinue"
        } else {
            missionConfig.exitOnRCLost = "executeLostAction"
            missionConfig.executeRCLostAction = "goBack"
        }

        val droneInfo = DroneInfo()
        droneInfo.droneEnumValue = 67
        droneInfo.droneSubEnumValue = 0
        missionConfig.droneInfo = droneInfo

        val payloadInfo = PayloadInfo()
        payloadInfo.payloadEnumValue = convetGimbalType("XL801")
        payloadInfo.payloadPositionIndex = 0
        payloadInfo.payloadSubEnumValue = 0
        missionConfig.payloadInfo = payloadInfo

        missionConfig.interestingPoints = emptyList()

        val homePoint = HomePoint()
        homePoint.apply {
            homePoint.altitude = 0.0
            homePoint.height = 0.0
            homePoint.latitude = model.parameters.uav_init_pos[1]
            homePoint.longitude = model.parameters.uav_init_pos[0]
            homePoint.id = "1"
        }
        missionConfig.homePoint = homePoint
    }

    override fun getFolder(model: MissionPath, templateType: String): Folder {
        val folder = Folder()

        folder.templateType = templateType
        folder.templateId = "0"

        //避障模式：关闭
        folder.obstacleMode = ObstacleAvoidanceType.INVALID.value

        //使用绝对高度
        val altitudeType = AltitudeType.ABSOLUTE.value
        folder.waylineCoordinateSysParam = getCoordinateSysParam(altitudeType)

        val payloadParam = PayloadParam()
        payloadParam.imageFormat = convertLensToString(0)
        folder.payloadParam = payloadParam

        when (templateType) {
            MissionTypeValue.WAYPOINT -> {

                val droneHeadingControl = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value
                val droneYaw =  0.0f
                folder.globalWaypointHeadingParam = getGlobalHeadingMode(droneHeadingControl, droneYaw)

                folder.globalHeight =  60F
                folder.height =  model.parameters.uav_height_ground.toFloat()
                folder.autoFlightSpeed = model.parameters.uav_speed_high.toFloat()

                folder.gimbalYaw =  0
                folder.gimbalPitchMode = "usePointSetting" /*, "manual"*/

                folder.placemarkList = getWaypointPlacemarks(model)

                folder.placemarkList.forEach {placemark ->
                    placemark.actionGroup?.forEach {
                        if (it.actionGroupEndIndex == -1) {
                            it.actionGroupEndIndex = folder.placemarkList.size - 1
                        }
                    }
                }
            }

            else -> {
            }
        }
        return folder
    }


    override fun getWaypointPlacemarks(model: MissionPath): MutableList<Placemark> {
        val placemarks: MutableList<Placemark> = mutableListOf()

        model.uav_route.forEachIndexed { pointIdx, it ->
            val placemark = Placemark()
            placemarks.add(placemark)

            placemark.index = pointIdx
            placemark.waypointType = 0

            val height = it.getOrNull(2)?.toFloat() ?: 60F
            placemark.height = height

            val speed = it.getOrNull(3) ?: 5.0
            placemark.waypointSpeed = speed

            //航点坐标
            val lon = it.getOrNull(0)
            val lat = it.getOrNull(1)
            placemark.point = getCoordinatesPoint(lon!!, lat!!)

            placemark.interestingPointId = ""

            //偏航角模式: 默认沿航线
            val droneHeadingControl = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value
            val droneYaw = it.getOrNull(4)?.toFloat() ?: 0.0f
            placemark.droneHeadingControlValue = droneHeadingControl
            placemark.waypointHeadingParam = getHeadingMode(droneHeadingControl, droneYaw)

            //协调半径
            placemark.waypointTurnParam = WaypointTurnParam()
            placemark.waypointTurnParam.waypointTurnMode = "toPointAndStopWithDiscontinuityCurvature"
            placemark.waypointTurnParam.waypointTurnDampingDist = 0.0f

            placemark.useGlobalHeight = "0"
            placemark.useGlobalSpeed =  "0"
            placemark.useGlobalHeadingParam =  "0"

            val gimbalPitchAngle = it.getOrNull(5)?.toFloat() ?: 0.0f
            placemark.gimbalPitchAngle = gimbalPitchAngle

            //全局航段轨迹是否尽量贴合直线 : 0：航段轨迹全程为曲线 1：航段轨迹尽量贴合两点连线
            //placemark.useStraightLine = 1
            //相机动作
            val groups = arrayListOf<ActionGroup>()
            placemark.actionGroup = groups

            //飞机偏航角（云台偏航角）
            val cameraActions: MutableList<CameraActionModel> = arrayListOf()
            val cameraActionModel1 = CameraActionModel()
            cameraActionModel1.type =  CameraActionType.DRONE_YAW.value
            cameraActionModel1.actionValue = it.getOrNull(4)?.toFloat() ?: 0.0f
            cameraActions.add(cameraActionModel1)

            //云台俯仰角(-90 ~ 30)
            val cameraActionModel2 = CameraActionModel()
            cameraActionModel2.type =  CameraActionType.GIMBAL_PITCH.value
            val pitchAngle = it.getOrNull(5)?.toFloat() ?: 0.0f
            cameraActionModel2.actionValue = if(pitchAngle != 0.0f) {
                -pitchAngle
            } else {
                pitchAngle
            }
            cameraActions.add(cameraActionModel2)

            //相机拍照动作
            val cameraActionModel3 = CameraActionModel()
            cameraActionModel3.type =  CameraActionType.TAKE_PHOTO.value
            cameraActionModel3.actionValue = it.getOrNull(5)?.toFloat() ?: 0.0f
            cameraActions.add(cameraActionModel3)


            getGroups(groups, placemarks, cameraActions, pointIdx)
        }
        return placemarks
    }

    //全局偏航角模式
    private fun getGlobalHeadingMode(droneHeadingControl: Int, droneYaw: Float): GlobalWaypointHeadingParam {
        val globalParam = GlobalWaypointHeadingParam()

        globalParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(droneHeadingControl)
        if (droneHeadingControl == DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value) {
            globalParam.waypointHeadingAngle = 0f
        } else {
            globalParam.waypointHeadingAngle = if (droneYaw > 180) {
                droneYaw - 360
            } else {
                droneYaw
            }
        }
        return globalParam
    }

    //航点偏航角模式
    private fun getHeadingMode(droneHeadingControl: Int, droneYaw: Float): WaypointHeadingParam {
        val headingParam = WaypointHeadingParam()
        headingParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(droneHeadingControl)
        if (droneHeadingControl == DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value) {
            headingParam.waypointHeadingAngle = 0f
        } else {
            headingParam.waypointHeadingAngle = if (droneYaw > 180) {
                droneYaw - 360
            } else {
                droneYaw
            }
        }
        return headingParam
    }

    //坐标类型，高度模式
    private fun getCoordinateSysParam(altitudeType: Int): WaylineCoordinateSysParam {
        val coordinateSysParam = WaylineCoordinateSysParam()
        coordinateSysParam.coordinateMode = "WGS84"
        if (altitudeType == AltitudeType.RELATIVE.value) {
            coordinateSysParam.heightMode = "relativeToStartPoint"
        } else {
            coordinateSysParam.heightMode = "EGM96"
        }
        return coordinateSysParam
    }
}