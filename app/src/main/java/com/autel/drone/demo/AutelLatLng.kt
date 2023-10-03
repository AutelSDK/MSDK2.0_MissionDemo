package com.autel.drone.demo

/**
 * @date 2022/7/15.
 * @author maowei
 * @description 经纬度
 */
data class AutelLatLng @JvmOverloads constructor(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
) {
    companion object {
        const val INVALID_VALUE = 0.0
    }

    /**
     * 是否非法
     */
    fun isInvalid(): Boolean {
        return !isValidLongitude() || !isValidLatitude() || (latitude == 0.0 && longitude == 0.0)
    }

    /**
     * 纬度是否合法
     */
    private fun isValidLatitude(): Boolean {
        return latitude > -90 && latitude < 90
    }

    /**
     * 经度是否合法
     */
    private fun isValidLongitude(): Boolean {
        return longitude > -180 && longitude < 180
    }

    override fun toString(): String {
        return "latitude=$latitude,longitude=$longitude,altitude=$altitude "
    }
}



