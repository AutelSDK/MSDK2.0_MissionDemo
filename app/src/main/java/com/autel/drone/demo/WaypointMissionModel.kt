package com.autel.drone.demo

import java.util.*

/**
 * @description 航点任务数据模型
 */

data class WaypointMissionModel(

    var uuid: String = UUID.randomUUID().toString(),

    /** 整体航线的全局高度 */
    var routeHeight: Float = MissionConstant.DEFAULT_WAYPOINT_FLY_HEIGHT,
    /** 整体航线的全局速度 */
    var routeSpeed: Float = MissionConstant.DEFAULT_WAYPOINT_FLY_SPEED,
    /** 偏航角类型(沿航线方向、手动控制) */
    var droneHeadingControl: Int = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value,
    /** 航线相机动作 */
    var cameraActionType: Int = CameraActionEnum.LEG_RECORD.value,
    /** 拍照间隔 */
    var cameraInterval: Int = 2,
    /** 拍照距离 */
    var cameraDistance: Int = 5,// 和全局速度关联，拍照间隔至少1s
    /** 云台俯仰角 */
    var gimbalPitch: Float = 0f,
    /** 偏航角(机头朝向) */
    var droneYaw: Float = 0f,
    /** 云台偏航角 */
    var gimbalYaw: Float = 0f,

    /** 航点列表 */
    var waypointList: MutableList<WaypointModel>? = null,
)