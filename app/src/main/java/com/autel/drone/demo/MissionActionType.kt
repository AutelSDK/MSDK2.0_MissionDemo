package com.autel.drone.demo

/**
 * @description 航点动作类作数据模型
 */
enum class MissionActionType(val value: Int) {

    /** 飞越  */
    FLY_OVER(0),

    /** 悬停  */
    HOVER(1),

    /** 协调转弯  */
    ARC(2),

    /** 未知  */
    UNKNOWN(-1)
}