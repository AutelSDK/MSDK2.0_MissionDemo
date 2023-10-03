package com.autel.drone.demo

/**
 * @Date 2022/09/21 15:05
 * 盘旋方向类型
 */
enum class MissionHoverDirection(val value: Int) {

    /**
     * 顺时针
     */
    CLOCKWISE(0),

    /**
     * 逆时针
     */
    ANTICLOCKWISE(1),

    /**
     * 未知
     */
    UNKOWN(-1)


}