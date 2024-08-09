package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 *
 * 任务默认避障模式
 */
enum class ObstacleAvoidanceType(val value: Int) {
    /**
     * 无效模式
     */
    INVALID(0),

    /**
     * 遇到障碍物悬停
     */
    HOVER(1),

    /**
     * 遇到障碍物左右避让，避让失败时悬停
     */
    LEFT_RIGHT(2),
}