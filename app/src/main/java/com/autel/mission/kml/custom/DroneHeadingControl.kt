package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 9:07
 * 飞机偏航角模式
 */
enum class DroneHeadingControl(val value: Int) {

    /** 沿航线方向 */
    FOLLOW_ROUTE_DIRECTION(1),

    /** 手动控制 */
    MANUAL_CONTROL(2),

    /** 自定义 */
    CUSTOM(3),

    /** 朝兴趣点方向 */
    FOLLOW_INTEREST_POINT(4),

    /** 未知 */
    UNKNOWN(-1);

    companion object {
        fun convertToKmlString(value: Int): String {
            return when (value) {
                1 -> "followWayline"
                2 -> "manually"
                3 -> "smoothTransition"
                4 -> "towardPOI"
                else -> "followWayline"
            }
        }
    }
}