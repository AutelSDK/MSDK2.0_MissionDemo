package com.autel.drone.demo

/**
 * @description 相机动作
 */
enum class CameraActionEnum(val value: Int) {

    /** 航段动作-拍照*/
    LEG_TAKE_PHOTO(1),

    /** 航段动作-录像 */
    LEG_RECORD(2),

    /** 航段动作-定时拍 */
    LEG_TIMELAPSE(3),

    /** 航段动作-定距拍照 */
    LEG_DISTANCE(4),

    /** 航段动作-定距扫拍 */
    LEG_DISTANCE_SCAN(5),

    /** 航段动作-结束录像 */
    LEG_STOP_RECORD(6),

    /**  航段动作-停止拍照 */
    LEG_STOP_PHOTO(7),

    /** 航段动作-无 */
    LEG_NONE(10),

    /** 航点动作-拍照 */
    POINT_TAKE_PHOTO(11),

    /** 航点动作-录像 */
    POINT_RECORD(12),

    /** 航点动作-定时拍照 */
    POINT_TIMELAPSE(13),

    /** 航点动作-无 */
    POINT_NONE(14),

    UNKNOWN(-1);

    companion object {

        /**
         * 是否是航点动作
         */
        fun isWayAction(value: Int): Boolean {
            return when (value) {
                LEG_NONE.value,
                LEG_TAKE_PHOTO.value,
                LEG_RECORD.value,
                LEG_TIMELAPSE.value,
                LEG_DISTANCE.value,
                LEG_DISTANCE_SCAN.value,
                LEG_STOP_RECORD.value,
                LEG_STOP_PHOTO.value -> true
                else -> false
            }
        }
    }

}