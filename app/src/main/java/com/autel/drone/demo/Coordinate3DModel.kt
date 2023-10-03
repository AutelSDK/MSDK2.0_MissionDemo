package com.autel.drone.demo

import java.util.*

/**
 * @description 经纬度数据模型
 */


data class Coordinate3DModel(
    /**
     * key 不自增
     */
    var uuid: String = UUID.randomUUID().toString(),

    /**
     * 关联到其他表格，需要用到坐标点这个表的信息（表名+UUID+Coordinate3DEnum 枚举）
     */
    var foreign_key_id: String? = null,


    /**
     * 当前点的经纬度index，倾斜摄影专用，用来区分是那一条航线上传的,倾斜摄影专用
     */
    var index: Int = 0,

    /**
     * 纬度
     */
    var latitude: Double = 0.0,

    /**
     * 精度
     */
    var longitude: Double = 0.0,

    /**
     * 海拔高度
     */
    var altitude: Double = 0.0,

    /** 相对高度 */
    var height: Double = 0.0,

    /**
     * 飞行距离，从飞机当前位置飞行到这个点的距离，不保存数据库
     */
    var flyLength: Double = 0.0,

    /**
     * 飞行时间，从飞机当前位置飞行到这个点的所需要的时间，不保存数据库
     */
    var flyTime: Double = 0.0,

    /**
     * 斜射高度
     */
    var tiltHieght: Double = 60.0,

    /**
     *飞行方向，针对倾斜摄影，不存数据库
     */
    var direction: Double = 0.0,


    /**
     * 标识当前的点时什么类型，兴趣点？，普通顶点或者返航点，起飞点等等
     */
    var type: Int = 0,

    /**地图类型
    UNKOWN(0), //未知
    WGS84(1), //地球坐标系，国际通用坐标系
    GCJ02(2), //火星坐标系，WGS84坐标系加密后的坐标系；Google国内地图、高德、QQ地图 使用
    BD09(3) //百度坐标系，GCJ02坐标系加密后的坐标系
     */
    var coordinateType: Int = 0,

)