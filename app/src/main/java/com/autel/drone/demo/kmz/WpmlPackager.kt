package com.autel.mission.kml.packager

import com.autel.data.bean.entity.*
import com.autel.data.bean.enums.*
import com.autel.drone.demo.AltitudeType
import com.autel.drone.demo.DroneHeadingControl
import com.autel.drone.demo.MissionConstant
import com.autel.drone.demo.MissionFinishActionType
import com.autel.drone.demo.MissionType
import com.autel.drone.demo.kmz.utils.CameraActionType
import com.autel.drone.demo.kmz.utils.GimbalInfoUtils
import com.autel.drone.demo.kmz.utils.KMLLog
import com.autel.drone.demo.kmz.utils.MissionTypeValue
import com.autel.drone.sdk.algor.AlgorithmManager2
import com.autel.drone.sdk.algor.bean.PolygonMission
import com.autel.drone.sdk.vmodelx.dronestate.CameraBaseData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.*
import com.autel.drone.sdk.vmodelx.module.mission.enums.MissionLostConnectActionType
import com.autel.drone.sdk.vmodelx.module.mission.enums.ObstacleAvoidanceType
import com.autel.internal.mission.MsnInfoUsr
import com.autel.internal.mission.WPInfoStru
import com.autel.mission.kml.utils.AssistPhotoUtils
import com.autel.msdk.lib.domain.model.map.data.AutelLatLng

/**
 * 封装 waylines.wpml
 */
class WpmlPackager: BasePackager(){

    companion object{
        var TAG = "WpmlPackager-KML"
    }

    override fun pack(flightModel: MissionFlightModel): Kml {
        packMissionCfg(flightModel)
        transformToKml(flightModel)
        return kmlBean
    }

    /**
     * MsnInfoUsr 为多机信息, 单机可用上面接口
     */
    fun pack(flightModel: MissionFlightModel, msnInfoUsr: MsnInfoUsr) : Kml {
        packMissionCfg(flightModel)
        transformToKml(flightModel, msnInfoUsr)
        return kmlBean
    }

    private fun transformToKml(flightModel: MissionFlightModel, msnInfoUsr: MsnInfoUsr) {
        var folderList: MutableList<Folder> = mutableListOf()
        document.folders = folderList
        val folder = Folder()
        folder.templateId = "0"
        folder.waylineId = "0"
        folder.obstacleMode = flightModel.obstacleAvoidance
        //高度模式
        if(flightModel.altitudeType == AltitudeType.ABSOLUTE.value){
            folder.executeHeightMode = "EGM96"
        } else {
            folder.executeHeightMode = "relativeToStartPoint"
        }
        val waypoints = flightModel.taskList.firstOrNull()?.waypointMission?.waypointList ?: emptyList<WaypointModel>()
        folder.placemarkList = convertPlacemarks(msnInfoUsr.wpInfoStrc.toList(), waypoints)
        folder.placemarkList.forEach {
            it.actionGroup?.forEach { group->
                group.actions?.forEach { action->
                    if (action.actionActuatorFunc == "takePhoto") {
                        action.actionActuatorFuncParam.payloadLensIndex = convertLensToString(flightModel.lensTypeValue)
                    }
                }
                if (group.actionGroupEndIndex == -1) {
                    group.actionGroupEndIndex = folder.placemarkList.size - 1
                }
            }
        }

        folderList.add(folder)
    }

    override fun transformToKml(flightModel: MissionFlightModel){
        val folderList: MutableList<Folder> = mutableListOf()
        document.folders = folderList

        val type = flightModel.missionType

        when (type) {
            MissionType.WAYPOINT.value -> {
                var templateType = "waypoint"
                getFolder(flightModel, templateType, type).let { folderList.add(it) }
            }
            MissionType.POLYGON.value, MissionType.RECTANGLE.value -> {
                var templateType = "mapping2d"
                getFolder(flightModel, templateType, type).let { folderList.add(it) }
            }
        }
    }

    override fun packMissionCfg(missionFlightModel: MissionFlightModel) {
        missionConfig.flyToWaylineMode = "safely"
        if (missionFlightModel.finishActionType == MissionFinishActionType.GO_HOME.value) {
            missionConfig.finishAction = "goHome"
        } else {
            missionConfig.finishAction = "noAction"
        }

        if (missionFlightModel.lostConnectAction == MissionLostConnectActionType.GO_ON_MISSION.value) {
            missionConfig.exitOnRCLost = "goContinue"
        } else {
            missionConfig.exitOnRCLost = "executeLostAction"
            missionConfig.executeRCLostAction = "goBack"
        }
        missionConfig.takeOffSecurityHeight = missionFlightModel.safeTakeoffHeight

        //Autel 自定义新增
        missionConfig.obstacleMode = getObstacleMode(missionFlightModel)
        missionConfig.globalTransitionalSpeed = waypointMission(missionFlightModel)?.routeSpeed.toString()

        var droneInfo = DroneInfo()
        droneInfo.droneEnumValue = 67
        droneInfo.droneSubEnumValue = 0
        missionConfig.droneInfo = droneInfo

        var payloadInfo = PayloadInfo()
        payloadInfo.payloadEnumValue = convetGimbalType(missionFlightModel.gimbalType)
        payloadInfo.payloadPositionIndex = 0
        payloadInfo.payloadSubEnumValue = 0
        missionConfig.payloadInfo = payloadInfo

        //Slam精准复拍
        missionConfig.isUseSlamPosition = isUseSlamPosition(missionFlightModel)
        missionConfig.slamResourcesPath = getUSlamPath(missionFlightModel)

        KMLLog.i(TAG, "SlamInfo- missionConfig hasSlam =${ missionConfig.isUseSlamPosition}, ${missionConfig.slamResourcesPath}")
    }

    override fun getFolder(missionFlightModel: MissionFlightModel, templateType: String, missionType: Int): Folder {
        var folder = Folder()

        var baseMissionModel = missionFlightModel.taskList.firstOrNull()
        var waypointMission = baseMissionModel?.waypointMission
        var mappingMission = baseMissionModel?.mappingMission

        folder.templateId = "0"
        folder.waylineId = "0"

        //高度模式
        if(missionFlightModel.altitudeType == AltitudeType.ABSOLUTE.value){
            folder.executeHeightMode = "WGS84"
        } else {
            folder.executeHeightMode = "relativeToStartPoint"
        }

        folder.autoFlightSpeed = waypointMission?.routeSpeed

        when (templateType) {
            MissionTypeValue.WAYPOINT -> {
                folder.autoFlightSpeed = waypointMission?.routeSpeed
                baseMissionModel?.let {
                    folder.placemarkList = getWaypointPlacemarks(it)
                }

                folder.placemarkList.forEachIndexed { index, placemark ->
                    baseMissionModel?.waypointMission?.waypointList?.getOrNull(index)?.let {wm->
                        val interest = missionFlightModel.interestingPoints.firstOrNull { it.uuid == wm.interestUuid }
                        interest?.let {
                            placemark.waypointHeadingParam.waypointPoiPoint = convertPoi(arrayListOf<Double>(it.latitude, it.longitude, it.height).toDoubleArray())
                        }

                    }
                    placemark.actionGroup?.forEach {
                        if (it.actionGroupEndIndex == -1) {
                            it.actionGroupEndIndex = folder.placemarkList.size - 1
                        }
                    }

                }
            }
           MissionTypeValue.RECTANGLE, MissionTypeValue.MAPPING2D -> {
                folder.height = mappingMission?.height
                folder.autoFlightSpeed = mappingMission?.speed
                baseMissionModel?.let {
                    folder.placemarkList = getPolygonPlacemarks(missionFlightModel)
                    folder.placemarkList.firstOrNull()?.actionGroup?.firstOrNull()?.let {
                        val group = it.clone()
                        folder.startActionGroup = group
                    }
                    folder.placemarkList.forEach {
                        it.actionGroup?.forEach { group->
                            group.actions?.forEach { action->
                                if (action.actionActuatorFunc == "takePhoto") {
                                    action.actionActuatorFuncParam.payloadLensIndex = convertLensToString(missionFlightModel.lensTypeValue)
                                }
                            }
                            if (group.actionGroupEndIndex == -1) {
                                group.actionGroupEndIndex = folder.placemarkList.size - 1
                            }
                        }
                    }
                }
            }
            else -> {
                folder.height = mappingMission?.height
                folder.autoFlightSpeed = mappingMission?.speed
                baseMissionModel?.let { folder.placemarkList = getMappingPlacemarks(it) }
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


    /**
     * 测绘任务
      */
    private fun getPolygonPlacemarks(flightModel: MissionFlightModel) : List<Placemark> {
       val mappingMission = flightModel.taskList.firstOrNull()?.mappingMission ?: return emptyList()
       val gimbal = GimbalInfoUtils.getGimbalSupport(GimbalTypeEnum.find(flightModel.gimbalType))
       val lensType = GimbalInfoUtils.getPriorityLensType(AssistPhotoUtils.getLensTypes(flightModel.lensTypeValue)) ?: LensTypeEnum.Zoom
       var fovH = gimbal?.getHFov(lensType) ?: MissionConstant.DEFAULT_FOVH
       var fovV = gimbal?.getVFov(lensType) ?: MissionConstant.DEFAULT_FOVV

       val m = PolygonMission()
       m.vertexs = getVertexList(mappingMission)
       m.droneLLA = doubleArrayOf()
       m.homeLLA = doubleArrayOf()
       m.userDefineAngle = if (mappingMission.autoDefineAngle) 0 else 1 //0:自动，1：用户自定义航向

       m.sideRate = mappingMission.sideRate / 100f
       m.courseRate = mappingMission.courseRate / 100f
       m.finishAction = flightModel.finishActionType
       m.doubleGrid = if (mappingMission.doubleGrid) 1 else 0
       m.altOptim = if (mappingMission.elevationOpt) 1 else 0
       m.photoAngle = mappingMission.takePhotoAngle
       m.rEnable = if (mappingMission.coordinatedTurn) 1 else 0
       m.speed = mappingMission.speed
       m.height = mappingMission.height
       m.RelativeH = mappingMission.relativeHeight
       m.courseAngle = mappingMission.courseAngle
       m.gimbalPitch = mappingMission.pitchAngle
       m.vFov = fovV
       m.hFov = fovH
       m.UserEnlargeL =
           if (mappingMission.enlargeSwitch == 1) mappingMission.enlargeValue else 0f
       val result = AlgorithmManager2.getInstance().getMappingMissionPath(m)
       if(result.errCode.toInt() == 0) {
           return convertPlacemarks(result.wpInfoStrc.toList(), emptyList())
       }
       return emptyList()
   }

    private fun convertPlacemarks(infoStrus: List<WPInfoStru>, points: List<WaypointModel>) : List<Placemark> {
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
    }

    override fun getWaypointPlacemarks(baseMissionModel: BaseMissionModel): MutableList<Placemark> {
        var placemarks: MutableList<Placemark> = mutableListOf()
        val pointsList = baseMissionModel.waypointMission?.waypointList ?: emptyList()
        baseMissionModel.waypointMission?.waypointList?.forEachIndexed { pointIdx, it ->
            var placemark = Placemark()
            placemarks.add(placemark)

            placemark.index = pointIdx
            placemark.executeHeight = it.height
            placemark.waypointSpeed = it.speed
            placemark.point = getCoordinatesPoint(it)

            //偏航角模式
            placemark.waypointHeadingParam = getHeadingMode(it.droneHeadingControl, it.droneYaw, DoubleArray(0))

            //协调半径
            placemark.waypointTurnParam = WaypointTurnParam()
            placemark.waypointTurnParam.waypointTurnMode = "toPointAndStopWithDiscontinuityCurvature"
            placemark.waypointTurnParam.waypointTurnDampingDist = it.turnRadius.toFloat()
            if (it.turnRadius > 0) {
                placemark.waypointTurnParam.waypointTurnMode = "coordinateTurn"
            }
            if (it.turnRadius > 0 && pointIdx > 0 && pointIdx < pointsList.size - 1) {
                val pre = AutelLatLng(pointsList[pointIdx - 1].latitude,pointsList[pointIdx - 1].longitude)
                val center = AutelLatLng(it.latitude,it.longitude)
                val next = AutelLatLng(pointsList[pointIdx + 1].latitude,pointsList[pointIdx + 1].longitude)
                val distance = convertRadiusToDistance(listOf(pre, center, next), it.turnRadius.toFloat())
                placemark.waypointTurnParam.waypointTurnDampingDist = distance
            }

            //end协调半径

            //全局航段轨迹是否尽量贴合直线 : 0：航段轨迹全程为曲线 1：航段轨迹尽量贴合两点连线
            //placemark.useStraightLine = 1

            //相机动作
            val groups = arrayListOf<ActionGroup>()
            placemark.actionGroup = groups
            getGroups(groups, placemarks, it.cameraActions, pointIdx)

            val isCustom = GimbalPitchMode.find(baseMissionModel.waypointMission?.gimbalPitchMode ?: "")
            if (isCustom == GimbalPitchMode.CUSTOM && pointIdx > 0) {
                val nextGroup = getTowardsNextPointGroup(it.gimbalPitch, pointIdx - 1, placemarks[pointIdx - 1].actionGroup.size)
                placemarks[pointIdx - 1].actionGroup.add(nextGroup)
            }

            //精准复拍 Slam 相关
            placemark.gpsPositionValid = it.slamInfoModel.gpsValid
            placemark.rtkPositionValid = it.slamInfoModel.rtkValid
            placemark.slamPositionInfo = SlamPositionInfo()
            val slamPosition = "${it.slamInfoModel.slamX},${it.slamInfoModel.slamY},${it.slamInfoModel.slamZ}"
            placemark.slamPositionInfo.slamPosition = slamPosition
            placemark.slamPositionInfo.slamPositionValid = it.slamInfoModel.slamValid
            placemark.slamPositionInfo.slamPositionIndex = it.slamInfoModel.slamPosIndex
            KMLLog.i(TAG, "SlamInfo-packager slamPosition = $slamPosition")
            //精准复拍 Slam 相关 end
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


    private fun waypointMission(model: MissionFlightModel): WaypointMissionModel?{
        return model.taskList.firstOrNull()?.waypointMission
    }

    //MissionConfig中自定义字段
    private fun getObstacleMode(model: MissionFlightModel): String {
        return when (model.obstacleAvoidance) {
            ObstacleAvoidanceType.INVALID.value -> "ignore"
            ObstacleAvoidanceType.HOVER.value -> "hover"
            ObstacleAvoidanceType.LEFT_RIGHT.value -> "avoid"
            else -> "ignore"
        }
    }

    private fun getVertexList(taskModel: MappingMissionModel): DoubleArray {
        val vertexPointList = taskModel.vertexList
        if (vertexPointList.isEmpty()) {
            return DoubleArray(0)
        }
        val length = vertexPointList.size
        val vertexList =
            DoubleArray(length * 3)
        var index = 0
        for (waypoint in vertexPointList) {
            vertexList[index++] = waypoint.latitude
            vertexList[index++] = waypoint.longitude
            vertexList[index++] = waypoint.height
        }
        return vertexList
    }
}