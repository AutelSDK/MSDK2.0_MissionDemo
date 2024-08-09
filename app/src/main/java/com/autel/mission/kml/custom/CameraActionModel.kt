package com.autel.mission.kml.custom

import java.util.UUID

/**
 * @Author create by AUTEL
 * @Date 2023/11/28
 * 相机动作及参数模型
 */

data class CameraActionModel(

    var uuid: String = UUID.randomUUID().toString(),

    /** 动作 */
    var type: Int = CameraActionType.NONE.value,

    /**动作值*/
    var actionValue: Float = 0f,

    var fileName: String = "",

    /**拍摄镜头*/
    var lensValue: Int = 0,

    /**辅助拍照选项：识别模型编号*/
    var assistValue: Int = 0,

    //原始任务录制图片保存地址
    var originalPath: String? = null,

    //裁剪后任务录制保存地址
    var cropPicPath: String? = null,

    //裁剪参数
    var centerX:Int = 0,
    var centerY:Int = 0,
    var width:Int = 0,
    var height:Int = 0,

    var subActions : MutableList<CameraActionModel> = arrayListOf()

)
