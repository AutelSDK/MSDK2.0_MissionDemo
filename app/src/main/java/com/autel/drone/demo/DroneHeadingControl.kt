package com.autel.drone.demo

/**
 * @Date 2022/09/21 9:07
 * 飞机偏航角类型
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
    UNKNOWN(-1),
}