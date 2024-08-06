package com.autel.drone.demo

/**
 * @date 2022/8/5.
 * @author maowei
 * @description 航线类型   0航点， 1矩形， 2多边形， 3倾斜， 4任务录制，5航带，6多边形仿地，7单体环绕
 */
enum class MissionType(val value: Int) {

    /** 航点任务 */
    WAYPOINT(0),

    /** 矩形任务 */
    RECTANGLE(1),

    /** 多边形任务 */
    POLYGON(2),

    /** 倾斜摄影 */
    OBLIQUE_PHOTOGRAPHY(3),

    /** 测绘任务，任务录制 */
    TASK_RECORD(4),

    /**航带飞行**/
    BELT_FLIGHT(5),

    /** 多边形仿地 */
    POLYGON_GROUND (6),

    //单体环绕
    SINGLE_SURROUND(7),

    /** 自定义 home点  for test  */
    HOME_POINT(8),

    MARK_LINE(9),

    SWARM(10),
    /**蜂群航点编队*/
    SWARM_WAYPOINT(11),

    /** 未知 */
    UNKNOWN(-1)
}