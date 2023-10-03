package com.autel.drone.demo

import java.util.*

/**
 *  任务动作
 */
data class MissionActionModel(

    var uuid: String = UUID.randomUUID().toString(),
    /** 相机动作 */
    var cameraAction: Int = CameraActionEnum.UNKNOWN.value,
    /** 云台俯仰角 */
    var gimbalPitch: Float = 0f,
    /** 云台Roll角 */
    var cameraRoll: Float = 0f,
    /** 偏航角(机头朝向) */
    var droneYaw: Float = 0f,
    /** 相机参数，定时拍照的间隔 */
    var cameraInterval: Int = 2,
    /** 相机参数，定距拍照的距离间隔 */
    var cameraDistance: Float = 5.0f, //相机反应时间1s，此值和速度相关
    /** 动作时长(拍照时长、录像时长) */
    var actionTimeLen: Int = 10,
    /** 相机焦距 */
    var zoom: Int = 0,
    /** 飞行时朝向 */
    var droneHeadingControl: Int = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value,
)