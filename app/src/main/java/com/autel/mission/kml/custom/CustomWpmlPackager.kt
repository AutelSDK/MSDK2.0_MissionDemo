package com.autel.mission.kml.custom

import com.autel.drone.sdk.vmodelx.dronestate.CameraBaseData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.ActionGroup
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.DroneInfo
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Folder
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Kml
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.PayloadInfo
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Placemark
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.WaypointHeadingParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.WaypointTurnParam

/**
 * 封装 waylines.wpml
 */
class CustomWpmlPackager: CustomBasePackager(){

    companion object{
        var TAG = "CustomWpmlPackager-KML"
    }

    override fun pack(model: MissionPath): Kml {
        packMissionCfg(model)
        transformToKml(model)
        return kmlBean
    }

    override fun transformToKml(model: MissionPath){
        val folderList: MutableList<Folder> = mutableListOf()
        document.folders = folderList
        val templateType = "waypoint"
        getFolder(model, templateType).let { folderList.add(it) }
    }

    override fun packMissionCfg(model: MissionPath) {
        //起飞安全高度
        missionConfig.takeOffSecurityHeight = model.parameters.uav_height_ground.toFloat()
        //全局速度
        missionConfig.globalTransitionalSpeed = model.parameters.uav_speed_high.toString()

        missionConfig.flyToWaylineMode = "safely"

        //任务执行完成动作：返航
        val finishActionType = MissionFinishActionType.GO_HOME.value
        if (finishActionType == MissionFinishActionType.GO_HOME.value) {
            missionConfig.finishAction = "goHome"
        } else {
            missionConfig.finishAction = "noAction"
        }

        //执行任务失联动作：继续任务
        val lostConnectAction = MissionLostConnectActionType.GO_ON_MISSION.value
        if (lostConnectAction == MissionLostConnectActionType.GO_ON_MISSION.value) {
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
        payloadInfo.payloadEnumValue = 0 //convetGimbalType(missionFlightModel.gimbalType)
        payloadInfo.payloadPositionIndex = 0
        payloadInfo.payloadSubEnumValue = 0
        missionConfig.payloadInfo = payloadInfo
    }

    override fun getFolder(model: MissionPath, templateType: String): Folder {
        val folder = Folder()

        folder.templateId = "0"
        folder.waylineId = "0"

        //高度模式: 使用绝对高度
        val altitudeType = AltitudeType.ABSOLUTE.value
        if(altitudeType == AltitudeType.ABSOLUTE.value){
            folder.executeHeightMode = "WGS84"
        } else {
            folder.executeHeightMode = "relativeToStartPoint"
        }

        //全局飞行速度：取得最大飞行速度
        folder.autoFlightSpeed = model.parameters.uav_speed_high.toFloat()

        when (templateType) {
            MissionTypeValue.WAYPOINT -> {
                folder.placemarkList = getWaypointPlacemarks(model)
                folder.placemarkList.forEachIndexed { _, placemark ->
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

    /**获取默认的可见光镜头缓存数据**/
    private fun getDefaultVisibleLensCameraData(droneDevice: IAutelDroneDevice): CameraBaseData? {
        val lensType = getDefaultVisibleLensType(droneDevice)
        val cameraData = droneDevice.getDeviceStateData().gimbalDataMap[droneDevice.getGimbalDeviceType()]?.cameraData
        if (lensType == LensTypeEnum.WideAngle) {
            return cameraData?.wideAngleCameraData
        } else if (lensType == LensTypeEnum.Zoom) {
            return cameraData?.zoomCameraData
        } else if (lensType == LensTypeEnum.TeleZoom) {
            return cameraData?.teleZoomCameraData
        } else if (lensType == LensTypeEnum.Visible) {
            return null
        }
        return null
    }

    /**获取默认的可见光镜头类型**/
    private fun getDefaultVisibleLensType(droneDevice: IAutelDroneDevice): LensTypeEnum? {
        val visibleList = listOf<LensTypeEnum>(LensTypeEnum.WideAngle, LensTypeEnum.Zoom, LensTypeEnum.Visible, LensTypeEnum.TeleZoom)
        val lensList = droneDevice.getCameraAbilitySetManger().getLensList(droneDevice.getGimbalDeviceType())
        visibleList.forEach {
            if (lensList?.contains(it) == true) {
                return it
            }
        }
        return null
    }



    /*private fun convertPlacemarks(infoStrus: List<WPInfoStru>, points: List<WaypointModel>) : List<Placemark> {
        val placemarks = arrayListOf<Placemark>()
        infoStrus.forEachIndexed { pointIdx, info ->
            val placemark = Placemark()
            placemark.index = pointIdx
            placemark.executeHeight = info.wpLLAUsr[2].toFloat()
            placemark.waypointSpeed = info.velRefUsr.toDouble()
            placemark.point = getCoordinatesPoint(info.wpLLAUsr)

            //偏航角模式
            var yaw = 0f
            info.msnActionInfo.lastOrNull()?.let {
                if (it.actionType < 10) {
                    yaw = it.actionYawRef
                }
            }
            placemark.waypointHeadingParam = getHeadingMode(info.headingMode.toInt(), yaw, info.poiUsr)

            //协调半径
            placemark.waypointTurnParam = WaypointTurnParam()
            placemark.waypointTurnParam.waypointTurnMode = "toPointAndStopWithDiscontinuityCurvature"
            placemark.waypointTurnParam.waypointTurnDampingDist = info.radUsr
            if (info.radUsr > 0) {
                placemark.waypointTurnParam.waypointTurnMode = "coordinateTurn"
            }
            if (info.radUsr > 0 && pointIdx > 0 && pointIdx < infoStrus.size - 1) {
                val pre = AutelLatLng(infoStrus[pointIdx - 1].wpLLAUsr[0],infoStrus[pointIdx - 1].wpLLAUsr[1])
                val center = AutelLatLng(info.wpLLAUsr[0],info.wpLLAUsr[1])
                val next = AutelLatLng(infoStrus[pointIdx + 1].wpLLAUsr[0],infoStrus[pointIdx + 1].wpLLAUsr[1])
                val distance = convertRadiusToDistance(listOf(pre, center, next), info.radUsr)
                placemark.waypointTurnParam.waypointTurnDampingDist = distance
            }

            //end协调半径

            //全局航段轨迹是否尽量贴合直线 : 0：航段轨迹全程为曲线 1：航段轨迹尽量贴合两点连线
            //placemark.useStraightLine = 1

            //相机动作
            val groups= arrayListOf<ActionGroup>()
            placemark.actionGroup = groups
            var groupIndex = 0
            var curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
            groups.add(curGroup)

            info.msnActions.forEachIndexed actionLoop@{ actionIdx, actionModel ->
                KMLLog.d(TAG, "actionType: ${actionModel.actionType}")
                when (actionModel.actionType) {
                    CameraActionType.TAKE_PHOTO.value -> {
                        curGroup.actions.add(getTakePhoto(null, curGroup.actions.size))
                    }
                    CameraActionType.DRONE_YAW.value -> {
                        curGroup.actions.add(getRotateYaw(actionModel, curGroup.actions.size))
                    }
                    CameraActionType.GIMBAL_PITCH.value -> {
                        curGroup.actions.add(getGimbalPitch(actionModel, curGroup.actions.size))
                    }
                    CameraActionType.ZOOM.value -> {
                        curGroup.actions.add(getZoom(actionModel, curGroup.actions.size))
                    }
                    CameraActionType.GIMBAL_YAW.value -> {
                        curGroup.actions.add(getGimbalYaw(actionModel, curGroup.actions.size))
                    }
                    CameraActionType.START_RECORD.value -> {
                        curGroup.actions.add(getStartRecord(null, curGroup.actions.size))
                    }
                    CameraActionType.HOVER.value -> {
                        curGroup.actions.add(getHover(actionModel, curGroup.actions.size))
                    }
                    CameraActionType.STOP_SHOOT.value -> {
                        setEndTakePhoto(placemarks, pointIdx)
                    }
                    CameraActionType.STOP_RECORD.value -> {
                        curGroup.actions.add(getStopRecord(curGroup.actions.size))
                    }
                    CameraActionType.DISTANCE_SHOOT.value -> {
                        if (curGroup.actions.isNotEmpty()) {
                            groupIndex += 1
                        } else {
                            groups.remove(curGroup)
                        }
                        val disGroup = getDistanceTakePhoto(actionModel, groupIndex, pointIdx)
                        groups.add(disGroup)
                        groupIndex += 1
                        curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
                        groups.add(curGroup)
                    }
                    CameraActionType.TIME_SHOOT.value -> {
                        if (curGroup.actions.isNotEmpty()) {
                            groupIndex += 1
                        } else {
                            groups.remove(curGroup)
                        }
                        val disGroup = getTimeTakePhoto(actionModel, groupIndex, pointIdx)
                        groups.add(disGroup)
                        groupIndex += 1
                        curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
                        groups.add(curGroup)
                    }
                }
            }
            if (curGroup.actions.isEmpty()) {
                groups.remove(curGroup)
            }
            if (pointIdx > 0 && points.size > pointIdx) {
                val nextGroup = getTowardsNextPointGroup(points[pointIdx].gimbalPitch, pointIdx - 1, placemarks[pointIdx - 1].actionGroup.size)
                placemarks[pointIdx - 1].actionGroup.add(nextGroup)
            }

            placemarks.add(placemark)
        }
        return placemarks
    }*/

    override fun getWaypointPlacemarks(model: MissionPath): MutableList<Placemark> {
        val placemarks: MutableList<Placemark> = mutableListOf()
        model.uav_route?.forEachIndexed { pointIdx, it ->
            val placemark = Placemark()
            placemarks.add(placemark)

            placemark.index = pointIdx

            val height = it.getOrNull(2)?.toFloat() ?: 60F
            placemark.executeHeight = height

            val speed = it.getOrNull(3) ?: 5.0
            placemark.waypointSpeed = speed

            //航点坐标
            val lon = it.getOrNull(0)
            val lat = it.getOrNull(1)
            placemark.point = getCoordinatesPoint(lon!!, lat!!)

            //偏航角模式: 默认沿航线
            val droneHeadingControl = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value
            val droneYaw = it.getOrNull(4)?.toFloat() ?: 0.0f
            placemark.waypointHeadingParam = getHeadingMode(droneHeadingControl, droneYaw, DoubleArray(0))

            //协调半径
            placemark.waypointTurnParam = WaypointTurnParam()
            placemark.waypointTurnParam.waypointTurnMode = "toPointAndStopWithDiscontinuityCurvature"
            placemark.waypointTurnParam.waypointTurnDampingDist = 0.0f

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

    //航点偏航角模式
    private fun getHeadingMode(mode: Int, yaw: Float, poi: DoubleArray): WaypointHeadingParam {
        val waypointHeadingParam = WaypointHeadingParam()
        waypointHeadingParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(mode)
        if (mode == DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value) {
            waypointHeadingParam.waypointHeadingAngleEnable = 0
            waypointHeadingParam.waypointHeadingAngle = 0f
        } else if (mode == DroneHeadingControl.FOLLOW_INTEREST_POINT.value) {
            waypointHeadingParam.waypointPoiPoint = convertPoi(poi)
        } else {
            waypointHeadingParam.waypointHeadingAngleEnable = 1
            waypointHeadingParam.waypointHeadingAngle = if (yaw > 180) {  //wpml(-180~180)
                yaw - 360
            } else {
                yaw
            }
        }
        return waypointHeadingParam
    }
}