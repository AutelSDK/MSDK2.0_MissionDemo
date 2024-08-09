package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 *
 * 航点高度类型
 */
enum class AltitudeType(val value: Int) {
    /**
     * 相对高度
     */
    RELATIVE(0),

    /**
     * 海拔高度
     */
    ABSOLUTE(1)
}