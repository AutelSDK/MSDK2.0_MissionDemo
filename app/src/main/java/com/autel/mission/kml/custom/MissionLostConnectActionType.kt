package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 * @description 任务失联动作
 */

enum class MissionLostConnectActionType(val value: Int) {
    /** 飞机执行的动作无效 */
    INVALID(0),

    /**  返航 */
    RETURN_HOME(1),

    /** 继续任务 */
    GO_ON_MISSION(2),

    UNKNOWN(-1),
}