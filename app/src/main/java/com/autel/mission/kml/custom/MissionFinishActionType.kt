package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 *
 * 任务完成后相机动作 Aircraft action after finish mission
 */
enum class MissionFinishActionType(val value: Int) {
    /** 返航 */
    GO_HOME(0),

    /** 悬停 */
    HOVER(1),

    /** 降落 */
    LAND(2),

    /** 停在最后一个航点位置 */
    LAST(3),

    /** 未知 */
    UNKNOWN(-1),
}