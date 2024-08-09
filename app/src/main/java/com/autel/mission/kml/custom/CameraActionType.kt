package com.autel.mission.kml.custom

/**
 * @Author create by AUTEL
 * @Date 2023/11/28
 * 相机动作枚举 camera action enum.
 */
enum class CameraActionType(val value: Int) {
    NONE(0),

    TAKE_PHOTO(1), //拍照 take photo

    START_RECORD(2), //开始录像 start video record

    TIME_SHOOT(3), //定时拍照

    DISTANCE_SHOOT(4), //定距拍照

    ORIENTED_SHOOT(5), //定向拍照

    STOP_RECORD(6), //停止录像

    STOP_SHOOT(7), //停止拍照

    GIMBAL_PITCH(8), //云台俯仰角

    GIMBAL_YAW(9), //云台偏航角

    ZOOM(10), //相机变焦

    HOVER(11), //悬停

    DRONE_YAW(12), //飞机偏航角

    CREATE_DIR(13); //创建文件夹

    companion object {

        fun find(value: Int): CameraActionType {
            return when (value) {
                0 -> NONE
                1 -> TAKE_PHOTO
                2 -> START_RECORD
                3 -> TIME_SHOOT
                4 -> DISTANCE_SHOOT
                5 -> ORIENTED_SHOOT
                6 -> STOP_RECORD
                7 -> STOP_SHOOT
                8 -> GIMBAL_PITCH
                9 -> GIMBAL_YAW
                10 -> ZOOM
                11 -> HOVER
                12 -> DRONE_YAW
                13 -> CREATE_DIR
                else -> {
                    NONE
                }
            }
        }
    }
}