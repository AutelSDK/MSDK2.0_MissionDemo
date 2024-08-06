package com.autel.mission.kml.packager

import com.autel.data.bean.entity.BaseMissionModel
import com.autel.data.bean.entity.MissionCameraActionModel
import com.autel.data.bean.entity.MissionFlightModel
import com.autel.data.bean.entity.QueueParamModel
import com.autel.data.bean.entity.WaypointModel
import com.autel.data.bean.enums.CameraActionType
import com.autel.data.bean.enums.DroneHeadingControl
import com.autel.drone.demo.AutelLatLng
import com.autel.drone.demo.DroneHeadingControl
import com.autel.drone.demo.WaypointModel
import com.autel.drone.demo.kmz.utils.CameraActionType
import com.autel.drone.demo.kmz.utils.KMLLog
import com.autel.drone.demo.kmz.utils.KmlConstant
import com.autel.drone.demo.kmz.utils.MissionTypeValue
import com.autel.drone.demo.kmz.utils.ParserUtils
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Action
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.ActionActuatorFuncParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.ActionGroup
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.ActionTrigger
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Document
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Folder
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.FormationInfo
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.GlobalWaypointHeadingParam
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.HomePoint
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.InterestingPoints
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Kml
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.MissionConfig
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Placemark
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Point
import com.autel.internal.mission.MsnAction
import com.autel.mission.kml.utils.AssistPhotoUtils
import com.autel.msdk.lib.domain.model.map.data.AutelLatLng
import com.autel.swarmjni.FormationType
import java.io.File
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.tan

abstract class BasePackager {
    companion object{
        var TAG = "BasePackager-KML"
    }

    var kmlBean: Kml = Kml()
    var document: Document = Document()
    var missionConfig: MissionConfig = MissionConfig()

    var resListener: OnResourceListener? = null

    init {

        kmlBean.xmlns = KmlConstant.XMLNS
        kmlBean.wpml = KmlConstant.WPML

        document.author = "Autel"
        document.version = "1.0"

        kmlBean.document = document
        kmlBean.document.missionConfig =missionConfig

    }

    abstract fun pack(flightModel: MissionFlightModel):Kml

    abstract fun transformToKml(mode: MissionFlightModel)

    abstract fun packMissionCfg(mode: MissionFlightModel)

    fun addResourceListener(listener: OnResourceListener){
        resListener = listener
    }

    open fun getFolder(missionFlightModel: MissionFlightModel, templateType: String, missionType: Int): Folder {
        val folder = Folder()
        val baseMissionModel = missionFlightModel.taskList.firstOrNull()
        val waypointMission = baseMissionModel?.waypointMission
        val mappingMission = baseMissionModel?.mappingMission

        folder.obstacleMode = missionFlightModel.obstacleAvoidance
        folder.templateType = templateType
        folder.templateId = "0"
        waypointMission?.let {
            val globalParam = GlobalWaypointHeadingParam()
            globalParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(it.droneHeadingControl)
            globalParam.waypointHeadingAngle = it.droneYaw
            folder.globalWaypointHeadingParam = globalParam
        }
        mappingMission?.let {
            val globalParam = GlobalWaypointHeadingParam()
            globalParam.waypointHeadingMode = DroneHeadingControl.convertToKmlString(0)
            globalParam.waypointHeadingAngle = 0f
            folder.globalWaypointHeadingParam = globalParam
        }
        when (templateType) {
            MissionTypeValue.WAYPOINT -> {
                folder.gimbalPitch = (waypointMission?.gimbalPitch ?: 0).toFloat()
                folder.globalHeight = waypointMission?.routeHeight ?: 60F
                folder.height = waypointMission?.routeHeight
                folder.autoFlightSpeed = waypointMission?.routeSpeed
                folder.gimbalYaw = waypointMission?.droneYaw?.toInt() ?: 0
                baseMissionModel?.let { folder.placemarkList = getWaypointPlacemarks(it) }
                folder.placemarkList.forEach {placemark ->
                    if(placemark.interestingPointId.isNotEmpty()) {
                        placemark.interestingPointId = missionFlightModel.interestingPoints.indexOfFirst { it.uuid == placemark.interestingPointId }.toString()
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

    open fun getMappingPlacemarks(baseMissionModel: BaseMissionModel): MutableList<Placemark> {
        val placemarks: MutableList<Placemark> = mutableListOf()
        baseMissionModel.mappingMission?.vertexList?.forEach {
            var placemark = Placemark()

            placemark.height = it.height.toFloat()
            val waypointModel = WaypointModel()
            waypointModel.altitude = it.altitude
            waypointModel.latitude = it.latitude
            waypointModel.longitude = it.longitude
            placemark.point = getCoordinatesPoint(waypointModel)
            /*placemark.waypointSpeed = it.speed
            placemark.interestingPointId = it.interestingPointIndex.toString()
            placemark.droneHeadingControlValue = it.droneHeadingControl

            placemark.useGlobalHeight = if (it.isHeightFollowRoute) 1 else 0
            placemark.useGlobalSpeed = if (it.isSpeedFollowRoute) 1 else 0
            placemark.useGlobalHeadingParam = if (it.isYawAngleFollowRoute) 1 else 0*/

            placemarks.add(placemark)
        }
        return placemarks
    }


    open fun getWaypointPlacemarks(baseMissionModel: BaseMissionModel): MutableList<Placemark> {
        var placemarks: MutableList<Placemark> = mutableListOf()
        return placemarks
    }


    /**
     * 编队信息解析
     */
    fun getFormationInfo(model: QueueParamModel): FormationInfo {
        val info = FormationInfo()
        if (model.type == FormationType.Type_C.value) {
            info.formationType = "C"
            info.formationRadius = model.width.toFloat()
            info.formationEndAngle = model.endAngle.toFloat()
            info.formationStartAngle = model.startAngle.toFloat()
        } else if (model.type == FormationType.Type_U.value) {
            info.formationType = "U"
            info.formationWidth = model.width.toFloat()
            info.formationLength = model.distance.toFloat()
        } else if (model.type == FormationType.Type_1.value) {
            info.formationType = "horizontalOne"
            info.formationDistanceBetweenAircraft = model.distance.toFloat()
        } else if (model.type == FormationType.Type_1V.value) {
            info.formationType = "verticalOne"
            info.formationDistanceBetweenAircraft = model.distance.toFloat()
        }
        return info
    }

    fun getCoordinatesPoint(waypointModel: WaypointModel): Point {
        val point = Point()
        point.coordinates = waypointModel.longitude.toString() + "," + waypointModel.latitude.toString()
        return point
    }

    fun getCoordinatesPoint(lla: DoubleArray): Point {
        val point = Point()
        if (lla.size >= 2) {
            point.coordinates = lla[1].toString() + "," + lla[0].toString()
        } else {
            point.coordinates = "0,0"
        }
        return point
    }

    fun getHomePoint(missionFlightModel: MissionFlightModel): HomePoint {
        var homePoint = HomePoint()
        missionFlightModel.homePoint?.HomePoint?.let {
            homePoint.altitude = it.altitude
            homePoint.height = it.height
            homePoint.latitude = it.latitude
            homePoint.longitude = it.longitude
            homePoint.id = it.uuid
        }
        return homePoint
    }

    fun getInterestingPoints(missionFlightModel: MissionFlightModel): List<InterestingPoints> {
        var interestingPoints: MutableList<InterestingPoints> = mutableListOf()
        missionFlightModel.interestingPoints?.forEach {
            var interestingPoint = InterestingPoints()
            interestingPoint.altitude = it.altitude
            interestingPoint.height = it.height
            interestingPoint.latitude = it.latitude
            interestingPoint.longitude = it.longitude
            interestingPoints.add(interestingPoint)
        }
        return interestingPoints
    }

    /////////////////new//////////
    fun getActionGroup(groupId: Int, index: Int, endIndex: Int): ActionGroup {
        val actionGroup = ActionGroup()
        actionGroup.actionGroupId = groupId
        actionGroup.actionGroupStartIndex = index
        actionGroup.actionGroupEndIndex = endIndex
        actionGroup.actionGroupMode = "sequence"
        //Action Trigger
        actionGroup.actionTrigger = ActionTrigger()
        actionGroup.actionTrigger.actionTriggerType = "reachPoint"

        actionGroup.actions = mutableListOf<Action>()
        return actionGroup
    }

    //飞机偏航角 Action
    fun getRotateYaw(actionModel: MissionCameraActionModel, actionId: Int = 0): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "rotateYaw"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.aircraftHeading = if (actionModel.actionValue > 180) {
            actionModel.actionValue - 360
        } else {
            actionModel.actionValue
        }
        action.actionActuatorFuncParam.aircraftPathMode = "counterClockwise"
        return action
    }

    //云台俯仰角 Action（云台偏航角飞机不支持）
    fun getGimbalPitch(actionModel: MissionCameraActionModel, actionId: Int = 1): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "gimbalRotate"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.gimbalRotateMode = "absoluteAngle"

        action.actionActuatorFuncParam.gimbalPitchRotateEnable = 1
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = actionModel.actionValue

        action.actionActuatorFuncParam.gimbalRollRotateEnable = 0
        action.actionActuatorFuncParam.gimbalRotateTimeEnable = 0
        return action
    }

    //云台俯仰角 Action（云台偏航角飞机不支持）
    fun getGimbalYaw(actionModel: MissionCameraActionModel, actionId: Int = 1): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "gimbalRotate"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.gimbalRotateMode = "absoluteAngle"

        action.actionActuatorFuncParam.gimbalPitchRotateEnable = 0
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = 0f
        action.actionActuatorFuncParam.gimbalYawRotateEnable = 1
        action.actionActuatorFuncParam.gimbalYawRotateAngle = if (actionModel.actionValue > 180) {
            actionModel.actionValue - 360
        } else {
            actionModel.actionValue
        }

        action.actionActuatorFuncParam.gimbalRollRotateEnable = 0
        action.actionActuatorFuncParam.gimbalRotateTimeEnable = 0
        return action
    }

    //云台俯仰角 Action（云台偏航角飞机不支持）
    fun getGimbalYaw(actionModel: MsnAction, actionId: Int = 1): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "gimbalRotate"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.gimbalRotateMode = "absoluteAngle"

        action.actionActuatorFuncParam.gimbalPitchRotateEnable = 0
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = 0f
        action.actionActuatorFuncParam.gimbalYawRotateEnable = 1
        action.actionActuatorFuncParam.gimbalYawRotateAngle = actionModel.actionValue

        action.actionActuatorFuncParam.gimbalRollRotateEnable = 0
        action.actionActuatorFuncParam.gimbalRotateTimeEnable = 0
        return action
    }

    //变焦
    fun getZoom(actionModel: MissionCameraActionModel, actionId: Int = 3): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "zoom"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.focalLength = convertZoomToEquivalentFocalLength(actionModel.actionValue)
        return action
    }


    fun getRotateYaw(actionModel: MsnAction, actionId: Int = 0): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "rotateYaw"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.aircraftHeading = if (actionModel.actionValue > 180) {
            actionModel.actionValue - 360
        } else {
            actionModel.actionValue
        }
        action.actionActuatorFuncParam.aircraftPathMode = "counterClockwise"
        return action
    }

    //云台俯仰角 Action（云台偏航角飞机不支持）
    fun getGimbalPitch(actionModel: MsnAction, actionId: Int = 1): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "gimbalRotate"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.gimbalRotateMode = "absoluteAngle"

        action.actionActuatorFuncParam.gimbalPitchRotateEnable = 1
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = actionModel.actionValue

        action.actionActuatorFuncParam.gimbalRollRotateEnable = 0
        action.actionActuatorFuncParam.gimbalRotateTimeEnable = 0
        return action
    }

    //变焦
    fun getZoom(actionModel: MsnAction, actionId: Int = 3): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "zoom"

        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.focalLength = convertZoomToEquivalentFocalLength(actionModel.actionValue)
        //变焦倍数，float类型，单位：倍， 范围[0.f, 160.f]
        action.actionActuatorFuncParam.zoomMultiple = actionModel.actionValue / 100.0f
        return action
    }

    //拍照 Action
    fun getTakePhoto(actionModel: MissionCameraActionModel?, actionId: Int = 4): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "takePhoto"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.payloadPositionIndex = 0
        actionModel?.let {
            action.actionActuatorFuncParam.useGlobalPayloadLensIndex = it.lensValue.and(1)
            action.actionActuatorFuncParam.payloadLensIndex = convertLensToString(it.lensValue)
        }
        return action
    }

    //Ai辅助拍照 Action, only for special scene
    fun getOrientTakePhoto(actionModel: MissionCameraActionModel, actionId: Int = 0): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "orientedShoot"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()

        action.actionActuatorFuncParam.actionUUID = actionId.toString()
        action.actionActuatorFuncParam.gimbalPitchRotateEnable = 1
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = actionModel.subActions.firstOrNull { it.type == CameraActionType.GIMBAL_PITCH.value }?.actionValue ?: 0f
        action.actionActuatorFuncParam.gimbalYawRotateAngle = actionModel.subActions.firstOrNull { it.type == CameraActionType.GIMBAL_YAW.value }?.actionValue ?: 0f

        val droneYaw = actionModel.subActions.firstOrNull { it.type == CameraActionType.DRONE_YAW.value }?.actionValue ?: 0f
        action.actionActuatorFuncParam.aircraftHeading = if (droneYaw > 180) {
            droneYaw - 360
        } else {
            droneYaw
        }
        val zoom = actionModel.subActions.firstOrNull { it.type == CameraActionType.ZOOM.value }?.actionValue ?: 1f
        action.actionActuatorFuncParam.focalLength = convertZoomToEquivalentFocalLength(zoom)
        action.actionActuatorFuncParam.useGlobalPayloadLensIndex = actionModel.lensValue.and(1)
        action.actionActuatorFuncParam.payloadLensIndex = convertLensToString(actionModel.lensValue)
        if(actionModel.assistValue != 0) {
            action.actionActuatorFuncParam.identifyingObjects = AssistPhotoUtils.getAiObjectString(actionModel.assistValue)
        }

        getPicName(actionModel.cropPicPath)?.let {
            action.actionActuatorFuncParam.accurateFrameValid = 1
            action.actionActuatorFuncParam.orientedFilePath = it
            action.actionActuatorFuncParam.focusX = actionModel.centerX.toFloat()
            action.actionActuatorFuncParam.focusY = actionModel.centerY.toFloat()
            action.actionActuatorFuncParam.focusRegionWidth = actionModel.width.toFloat()
            action.actionActuatorFuncParam.focusRegionHeight = actionModel.height.toFloat()

        } ?: kotlin.run {
            action.actionActuatorFuncParam.accurateFrameValid = 0
        }

        action.actionActuatorFuncParam.orientedFilePath = getPicName(actionModel.originalPath)
        action.actionActuatorFuncParam.originalFilePath = getPicName(actionModel.originalPath)

        resListener?.onResource(actionModel.originalPath, actionModel.cropPicPath)

        return action
    }

    private fun getPicName(path: String?): String? {
        try {
            path?.let {
                val startPos = path.lastIndexOf(File.separator)
                if (startPos < 0) return null
                return path.substring(startPos + 1, path.length)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    //开始录像
    fun getStartRecord(actionModel: MissionCameraActionModel?, actionId: Int = 3): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "startRecord"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.payloadPositionIndex = 0
        actionModel?.let {
            action.actionActuatorFuncParam.useGlobalPayloadLensIndex = it.lensValue.and(1)
            action.actionActuatorFuncParam.payloadLensIndex = convertLensToString(it.lensValue)
        }
        return action
    }

    //悬停动作
    fun getHover(actionModel: MissionCameraActionModel, actionId: Int = 4): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "hover"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.hoverTime = actionModel.actionValue
        return action
    }

    //悬停动作
    fun getHover(actionModel: MsnAction, actionId: Int = 4): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "hover"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.hoverTime = actionModel.actionValue
        return action
    }


    //停止录像
    fun getStopRecord(actionId: Int = 5): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "stopRecord"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        return action
    }

    //无动作
    fun getNoAction(actionId: Int = 6): Action {
        val action = Action()
        action.actionId = actionId
        action.actionActuatorFunc = "noAction"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.payloadPositionIndex = 0
        return action
    }

    fun getDistanceTakePhoto(actionModel: MissionCameraActionModel, groupId: Int, pointIdx: Int) : ActionGroup {
        val actionGroup = getActionGroup(groupId, pointIdx, -1)
        actionGroup.actionTrigger.actionTriggerType = "multipleDistance"
        actionGroup.actionTrigger.actionTriggerParam = actionModel.actionValue
        actionGroup.actions.add(getTakePhoto(actionModel, 0))
        return actionGroup
    }
    fun getTimeTakePhoto(actionModel: MissionCameraActionModel, groupId: Int, pointIdx: Int) : ActionGroup {
        val actionGroup = getActionGroup(groupId, pointIdx, -1)
        actionGroup.actionTrigger.actionTriggerType = "multipleTiming"
        actionGroup.actionTrigger.actionTriggerParam = actionModel.actionValue
        actionGroup.actions.add(getTakePhoto(actionModel, 0))
        return actionGroup
    }
    fun getDistanceTakePhoto(actionModel: MsnAction, groupId: Int, pointIdx: Int) : ActionGroup {
        val actionGroup = getActionGroup(groupId, pointIdx, -1)
        actionGroup.actionTrigger.actionTriggerType = "multipleDistance"
        actionGroup.actionTrigger.actionTriggerParam = actionModel.actionValue
        actionGroup.actions.add(getTakePhoto(null, 0))
        return actionGroup
    }
    fun getTimeTakePhoto(actionModel: MsnAction, groupId: Int, pointIdx: Int) : ActionGroup {
        val actionGroup = getActionGroup(groupId, pointIdx, -1)
        actionGroup.actionTrigger.actionTriggerType = "multipleTiming"
        actionGroup.actionTrigger.actionTriggerParam = actionModel.actionValue
        actionGroup.actions.add(getTakePhoto(null,0))
        return actionGroup
    }


    fun getGroups(groups: ArrayList<ActionGroup>, placemarks: List<Placemark>, actionModels: List<MissionCameraActionModel>, pointIdx: Int){
        var groupIndex = 0
        var curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
        groups.add(curGroup)
        actionModels.forEachIndexed { actionIdx, actionModel ->
            when (actionModel.type) {
                CameraActionType.TAKE_PHOTO.value -> {
                    if (curGroup.actions.size > 0) {
                        groupIndex += 1
                        curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
                        groups.add(curGroup)
                    }
                    curGroup.actions.add(getTakePhoto(actionModel, curGroup.actions.size))
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
                    if (curGroup.actions.size > 0) {
                        groupIndex += 1
                        curGroup = getActionGroup(groupIndex, pointIdx, pointIdx)
                        groups.add(curGroup)
                    }
                    curGroup.actions.add(getStartRecord(actionModel, curGroup.actions.size))
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
                CameraActionType.ORIENTED_SHOOT.value -> {
                    curGroup.actions.add(getOrientTakePhoto(actionModel, curGroup.actions.size))
                }
            }
        }
        if (curGroup.actions.isEmpty()) {
            groups.remove(curGroup)
        }
    }
    fun setEndTakePhoto(placemarks: List<Placemark>, endIndex: Int) {
        placemarks.forEach {
            it.actionGroup.forEach {group->
                if ((group.actionTrigger.actionTriggerType == "multipleDistance" || group.actionTrigger.actionTriggerType == "multipleTiming") && group.actionGroupEndIndex == -1) {
                    group.actionGroupEndIndex = endIndex
                }
            }
        }
    }

    fun getTowardsNextPointGroup(pitchAngle: Float, pointIdx: Int, groupId: Int) : ActionGroup {
        val group = getActionGroup(groupId, pointIdx, pointIdx + 1)
        group.actionTrigger.actionTriggerType = "betweenAdjacentPoints"
        val action = Action()
        action.actionId = 0
        action.actionActuatorFunc = "gimbalEvenlyRotate"
        action.actionActuatorFuncParam = ActionActuatorFuncParam()
        action.actionActuatorFuncParam.gimbalPitchRotateAngle = pitchAngle
        group.actions.add(action)
        return group
    }

    fun convertLensToString(lens: Int) : String {
        val arr = ArrayList<String>()
        AssistPhotoUtils.getLensTypes(lens).forEach { type->
            when(type) {
                LensTypeEnum.Zoom -> arr.add("zoom")
                LensTypeEnum.Thermal -> arr.add("ir")
                LensTypeEnum.WideAngle -> arr.add("wide")
                else -> arr.add("visable")
            }
        }
        return arr.joinToString(",")
    }

    fun convertPoi(poi: DoubleArray) : String {
        if (poi.size == 3) {
            return "" + poi[0] + "," + poi[1] + "," + poi[2]
        }
        return ""
    }


    //Autel 焦距倍数转 Dji 焦距
    private fun convertZoomToEquivalentFocalLength(zoomLevel: Float): Float {
        return zoomLevel * 24f //if (zoomLevel <= 107) {
//            (zoomLevel * 24 / 107).toFloat()
//        } else if (zoomLevel <= 110) {
//            (zoomLevel * 24 / 110).toFloat()
//        } else {
//            (zoomLevel * 24 / 123).toFloat()
//        }
    }
    /////////////// new  end ///////////////
    fun convetGimbalType(gimbalType: String) : Int {
        return try {
            gimbalType.replace("XL", "").toInt()
        } catch (e: java.lang.NumberFormatException) {
            KMLLog.e("TAG", "类型无法转换：$gimbalType, 给默认值801")
            801
        }
    }

    fun convertRadiusToDistance(points:List<AutelLatLng>, radius: Float) : Float {
        if (points.size != 3) return radius
        val a = ParserUtils.getDistance(points[0], points[1])
        val b = ParserUtils.getDistance(points[2], points[1])
        val c = ParserUtils.getDistance(points[0], points[2])
        val cosA = (a * a + b * b - c * c) / max(0.00001, 2 * a * b)
        val angle = acos(cosA)
        val distance = radius / tan(angle / 2)
        KMLLog.d(TAG, "radius: $radius, dis:$distance")
        if (distance.isNaN() || distance.isInfinite()) {
            return 0f
        }
        return distance.toFloat()
    }

    interface OnResourceListener{
        fun onResource(original: String? , crop: String?)
    }

    /**
     * 是否是精准复拍Slam航线
     */
    fun isUseSlamPosition(missionFlightModel: MissionFlightModel): Int {
        val waypointList = missionFlightModel.taskList.firstOrNull()?.waypointMission?.waypointList
        val hasSlam = waypointList?.any { it.slamInfoModel.slamValid == 1 } == true
        return if (hasSlam) 1 else 0
    }

    /**
     * Slam 资源路径
     */
    fun getUSlamPath(missionFlightModel: MissionFlightModel): String? {
        val waypointList = missionFlightModel.taskList.firstOrNull()?.waypointMission?.waypointList
        return  waypointList?.firstOrNull()?.slamInfoModel?.mapId
    }

}