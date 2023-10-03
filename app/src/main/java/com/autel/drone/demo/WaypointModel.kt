package com.autel.drone.demo


/**
 * @Date 2022/09/21 14:51
 * 航点数据模型
 */

data class WaypointModel(

    /**
     * 航点动作
     */
    var missionActionType: Int = MissionActionType.FLY_OVER.value,

    /**
     * 悬停时间
     */
    var hoverTime: Int = 0,

    /** 航点坐标经纬度、相对高度、绝对海拔*/
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,

    /**
     * 坐标类型
     */
    var coordinateType: Int = CoordinateType.UNKOWN.value,

    /**
     *飞行高度
     */
    var height: Float = 60f,

    /**
     * 飞行速度
     */
    var speed: Double = 5.0,

    /**
     * 盘旋方向，只针对航点动作为盘旋时有效
     */
    var hoverDirection: Int = MissionHoverDirection.CLOCKWISE.value,

    /**
     * 盘旋半径，只针对航点动作为盘旋时有效
     */
    var hoverRadius: Float = 0f,

    /**
     * 盘旋圈数，只针对航点动作为盘旋时有效
     */
    var hoverCylinderNumber: Float = 0f,

    /**
     * 任务动作集合
     */
    var missingActions: MutableList<MissionActionModel> = arrayListOf(),


    //关联兴趣点的 uuid
    var interestUuid: String? = null,

    /**
     * 转弯模式为圆弧转弯下的半径 协调半径
     */
    var turnRadius: Double = 0.0,

    /**
     * 最大协调半径
     */
    var maxTurnRadius: Float = 0f,

    /**
     * 高度优先级。1-高，0-中，-1-低
     */
    var heightPriority: Int = 0,

    /** 飞行时朝向 */
    var droneHeadingControl: Int = DroneHeadingControl.FOLLOW_ROUTE_DIRECTION.value,

    /** 偏航角(机头朝向) */
    var droneYaw: Float = 0f,

)