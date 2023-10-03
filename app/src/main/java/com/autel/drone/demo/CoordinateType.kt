package com.autel.drone.demo

/**
 * @Date 2022/09/21 15:00
 * 坐标类型
 */
enum class CoordinateType(val value: Int) {

    /**
     * 未知
     */
    UNKOWN(0),

    /**
     * WGS84坐标系    地球坐标系，国际通用坐标系
     */
    WGS84(1),

    /**
     * GCJ02坐标系    火星坐标系，WGS84坐标系加密后的坐标系；Google国内地图、高德、QQ地图 使用
     */
    GCJ02(2),

    /**
     *  BD09坐标系    百度坐标系，GCJ02坐标系加密后的坐标系
     */
    BD09(3)
}