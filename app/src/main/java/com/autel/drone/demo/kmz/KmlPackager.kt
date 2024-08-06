package com.autel.mission.kml.packager

import com.autel.data.bean.entity.BaseMissionModel
import com.autel.data.bean.entity.MissionFlightModel
import com.autel.data.bean.entity.WaypointMissionModel
import com.autel.data.bean.entity.WaypointModel
import com.autel.data.bean.enums.*
import com.autel.drone.demo.AltitudeType
import com.autel.drone.demo.DroneHeadingControl
import com.autel.drone.demo.MissionFinishActionType
import com.autel.drone.demo.MissionType
import com.autel.drone.demo.WaypointModel
import com.autel.drone.demo.kmz.utils.KMLLog
import com.autel.drone.demo.kmz.utils.MissionTypeValue
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.*
import com.autel.drone.sdk.vmodelx.module.mission.enums.MissionLostConnectActionType
import com.autel.mission.kml.KMLLog
import com.autel.mission.kml.utils.ParserUtils
import com.autel.mission.kml.value.MissionTypeValue
import com.autel.msdk.lib.domain.model.map.data.AutelLatLng

/**
 * 封装 template.kml
 */
class KmlPackager : BasePackager() {
    companion object{
        var TAG = "KmlPackager-KML"
    }

    override fun pack(flightModel: MissionFlightModel): Kml {
        packMissionCfg(flightModel)
        transformToKml(flightModel)
        return kmlBean
    }

    override fun transformToKml(missionFlightModel: MissionFlightModel) {
        document.folders = mutableListOf()

        var type = missionFlightModel.missionType
        when (type) {
            MissionType.WAYPOINT.value -> {
                var templateType = "waypoint"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }
            MissionType.SWARM_WAYPOINT.value -> {
                var templateType = "waypointFormation"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }

            MissionType.RECTANGLE.value -> {
                var templateType = "rectangular"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }

            MissionType.POLYGON.value -> {
                var templateType = "mapping2d"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }

            MissionType.OBLIQUE_PHOTOGRAPHY.value -> {
                var templateType = "mapping3d"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }

            MissionType.BELT_FLIGHT.value -> {
                var templateType = "mappingStrip"
                getFolder(missionFlightModel, templateType, type)?.let { document.folders.add(it) }
            }
        }
    }

    override fun packMissionCfg(missionFlightModel: MissionFlightModel){
        //Autel指挥中心兼容字段
        missionConfig.altitudeType = missionFlightModel.altitudeType
        missionConfig.name = missionFlightModel.summaryTaskInfoModel?.name
        missionConfig.estimateFlyTime = missionFlightModel.totalTime
        missionConfig.estimateFlyLength = missionFlightModel.totalDistance.toFloat() / 1000
        //Autel end
        missionConfig.takeOffSecurityHeight = missionFlightModel.safeTakeoffHeight
        document.createTime = missionFlightModel.summaryTaskInfoModel?.createTime
        document.updateTime = missionFlightModel.summaryTaskInfoModel?.updateTime
        missionConfig.flyToWaylineMode = "safely"
        if (missionFlightModel.finishActionType == MissionFinishActionType.GO_HOME.value) {
            missionConfig.finishAction = "goHome"
        } else {
            missionConfig.finishAction = "noAction"
        }

        if (missionFlightModel.lostConnectAction == MissionLostConnectActionType.GO_ON_MISSION.value) {
            missionConfig.executeRCLostAction = "goContinue"
            missionConfig.exitOnRCLost = "goContinue"
        } else {
            missionConfig.exitOnRCLost = "executeLostAction"
            missionConfig.executeRCLostAction = "goBack"
        }

        var droneInfo = DroneInfo()
        droneInfo.droneEnumValue = 67
        droneInfo.droneSubEnumValue = 0
        missionConfig.droneInfo = droneInfo

        var payloadInfo = PayloadInfo()
        payloadInfo.payloadEnumValue = convetGimbalType(missionFlightModel.gimbalType)
        payloadInfo.payloadPositionIndex = 0
        payloadInfo.payloadSubEnumValue = 0
        missionConfig.payloadInfo = payloadInfo

        missionFlightModel.taskList.firstOrNull()?.waypointMission?.let {
            missionConfig.globalTransitionalSpeed = it.routeSpeed.toString()
        }
        missionFlightModel.taskList.firstOrNull()?.mappingMission?.let {
            missionConfig.globalTransitionalSpeed = it.speed.toString()
        }

        missionConfig.interestingPoints = getInterestingPoints(missionFlightModel)
        missionConfig.homePoint = getHomePoint(missionFlightModel)

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

        folder.obstacleMode = missionFlightModel.obstacleAvoidance

        folder.templateType = templateType
        folder.templateId = "0"
        folder.waylineCoordinateSysParam = getCoordinateSysParam(missionFlightModel)
        val payloadParam = PayloadParam()
        payloadParam.imageFormat = convertLensToString(missionFlightModel.lensTypeValue)
        folder.payloadParam = payloadParam

        when (templateType) {
            MissionTypeValue.WAYPOINT, MissionTypeValue.WAYPOINTFORMATION -> {
                folder.globalWaypointHeadingParam = getHeadingMode(waypointMission)

                folder.globalHeight = waypointMission?.routeHeight ?: 60F
                folder.height = waypointMission?.routeHeight ?: 60F
                folder.autoFlightSpeed = waypointMission?.routeSpeed

                folder.gimbalYaw = waypointMission?.droneYaw?.toInt() ?: 0
                folder.gimbalPitchMode = waypointMission?.gimbalPitchMode ?: "manual"

                baseMissionModel?.let {
                    folder.placemarkList = getWaypointPlacemarks(it)
                }

                folder.placemarkList.forEach {placemark ->
                    if(placemark.interestingPointId.isNotEmpty()) {
                        val interest = missionFlightModel.interestingPoints.firstOrNull { it.uuid == placemark.interestingPointId }
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

            else -> {
                folder.globalWaypointHeadingParam = getHeadingMode(null)
                folder.height = mappingMission?.height
                folder.autoFlightSpeed = mappingMission?.speed
                baseMissionModel?.let {
                    folder.waylineCoordinateSysParam.globalShootHeight = it.mappingMission?.height ?: 0f
                    folder.placemarkList = getMappinPlacemarks(it)
                }
            }
        }
        return folder
    }

    override fun getMappinPlacemarks(baseMissionModel: BaseMissionModel): MutableList<Placemark> {
        val mappingMission = baseMissionModel.mappingMission ?: return arrayListOf()
        val placemark = Placemark()
        placemark.polygon = Polygon()
        val outerBoundaryIs = OuterBoundaryIs()
        outerBoundaryIs.linearRing = LinearRing()
        placemark.polygon.outerBoundaryIs = outerBoundaryIs
        var coordinates = ""
        mappingMission.vertexList.forEach { coordinates = coordinates + it.longitude + "," + it.latitude + "," + it.altitude + "\n\t   "}
        outerBoundaryIs.linearRing.coordinates = coordinates

        placemark.ellipsoidHeight = mappingMission.height - mappingMission.relativeHeight
        placemark.height = mappingMission.height - mappingMission.relativeHeight

        placemark.overlap = Overlap()
        placemark.overlap.orthoCameraOverlapH = mappingMission.courseRate.toInt()
        placemark.overlap.orthoCameraOverlapW = mappingMission.sideRate.toInt()
        placemark.overlap.inclinedLidarOverlapH = mappingMission.courseRateForTilt.toInt()
        placemark.overlap.inclinedLidarOverlapW = mappingMission.sideRateForTilt.toInt()
        placemark.direction = mappingMission.courseAngle.toInt()
        placemark.inclinedGimbalPitch = mappingMission.pitchAngleForTilt.toInt()
        placemark.inclinedFlightSpeed = mappingMission.speedForTilt
        placemark.relativeHeight = mappingMission.relativeHeight
        placemark.heightForTilt = mappingMission.heightForTilt
        placemark.dualGrid = if(mappingMission.doubleGrid) 1 else 0
        placemark.elevationOptimizeEnable = if (mappingMission.elevationOpt) 1 else 0
        placemark.coordinatedTurn = if (mappingMission.coordinatedTurn) 1 else 0
        placemark.gimbalPitchAngle = mappingMission.pitchAngle
        placemark.enlargeSwitch = mappingMission.enlargeSwitch
        placemark.enlargeValue = mappingMission.enlargeValue
        return arrayListOf(placemark)
    }
    override fun getWaypointPlacemarks(baseMissionModel: BaseMissionModel): MutableList<Placemark> {
        var placemarks: MutableList<Placemark> = mutableListOf()
        val pointsList = baseMissionModel.waypointMission?.waypointList ?: emptyList()

        baseMissionModel.waypointMission?.waypointList?.forEachIndexed { pointIdx, it ->
            var placemark = Placemark()
            placemarks.add(placemark)

            placemark.index = pointIdx
            placemark.waypointType = 0
            placemark.height = it.height
            placemark.point = getCoordinatesPoint(it)
            placemark.waypointSpeed = it.speed
            placemark.interestingPointId = it.interestUuid ?: ""

            //Autel兼容：指挥中心
            placemark.droneHeadingControlValue = it.droneHeadingControl
            //end Autel

            placemark.waypointHeadingParam = getHeadingMode(it)

            val waypointTurn = WaypointTurnParam()
            waypointTurn.waypointTurnMode = "toPointAndStopWithDiscontinuityCurvature"
            waypointTurn.waypointTurnDampingDist = it.turnRadius.toFloat()
            if (it.turnRadius > 0) {
                waypointTurn.waypointTurnMode = "coordinateTurn"
            }
            placemark.waypointTurnParam = waypointTurn
            if (it.turnRadius > 0 && pointIdx > 0 && pointIdx < pointsList.size - 1) {
                val pre = AutelLatLng(pointsList[pointIdx - 1].latitude,pointsList[pointIdx - 1].longitude)
                val center = AutelLatLng(it.latitude,it.longitude)
                val next = AutelLatLng(pointsList[pointIdx + 1].latitude,pointsList[pointIdx + 1].longitude)
                val distance = convertRadiusToDistance(listOf(pre, center, next), it.turnRadius.toFloat())
                placemark.waypointTurnParam.waypointTurnDampingDist = distance
            }

            placemark.useGlobalHeight = if(it.isHeightFollowRoute) "1" else "0"
            placemark.useGlobalSpeed = if(it.isSpeedFollowRoute) "1" else "0"
            placemark.useGlobalHeadingParam = if(it.isYawAngleFollowRoute) "1" else "0"
            placemark.gimbalPitchAngle = it.gimbalPitch

            if (baseMissionModel.missionType == MissionType.SWARM_WAYPOINT.value) {
                placemark.formationInfo = getFormationInfo(it.queueParamModel)
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


            //全局航段轨迹是否尽量贴合直线 : 0：航段轨迹全程为曲线 1：航段轨迹尽量贴合两点连线
            //placemark.useStraightLine = 1
            //相机动作
            val groups = arrayListOf<ActionGroup>()
            placemark.actionGroup = groups
            getGroups(groups, placemarks, it.cameraActions, pointIdx)
        }
        return placemarks
    }

    //全局偏航角模式
    private fun getHeadingMode(pointModel: WaypointMissionModel?): GlobalWaypointHeadingParam {
        val globalParam = GlobalWaypointHeadingParam()
        pointModel?.let {
            globalParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(pointModel.droneHeadingControl)
            if (pointModel.droneHeadingControl == DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value) {
                globalParam.waypointHeadingAngle = 0f
            } else {
                globalParam.waypointHeadingAngle = if (pointModel.droneYaw > 180) {
                    pointModel.droneYaw - 360
                } else {
                    pointModel.droneYaw
                }
            }
        } ?: run {
            globalParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(0)
            globalParam.waypointHeadingAngle = 0f
        }
        return globalParam
    }

    //航点偏航角模式
    private fun getHeadingMode(pointModel: WaypointModel): WaypointHeadingParam {
        val headingParam = WaypointHeadingParam()
        headingParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(pointModel.droneHeadingControl)
        if (pointModel.droneHeadingControl == DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value) {
            headingParam.waypointHeadingAngle = 0f
        } else {
            headingParam.waypointHeadingAngle = if (pointModel.droneYaw > 180) {
                pointModel.droneYaw - 360
            } else {
                pointModel.droneYaw
            }
        }
        return headingParam
    }

    //坐标类型，高度模式
    private fun getCoordinateSysParam(missionFlightModel: MissionFlightModel): WaylineCoordinateSysParam {
        val coordinateSysParam = WaylineCoordinateSysParam()
        coordinateSysParam.coordinateMode = "WGS84"
        if (missionFlightModel.altitudeType == AltitudeType.RELATIVE.value) {
            coordinateSysParam.heightMode = "relativeToStartPoint"
        } else {
            coordinateSysParam.heightMode = "EGM96"
        }
        return coordinateSysParam
    }
}