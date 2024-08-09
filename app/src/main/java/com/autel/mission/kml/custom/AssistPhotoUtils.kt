package com.autel.mission.kml.custom

import android.text.TextUtils
import android.util.Log
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum

/**
 * 航线辅助拍照 Ai识别
 */
class AssistPhotoUtils {

    companion object {

        private val AiModels = HashMap<Int, String>().apply {
            put(10, "insulator")
            put(11, "crossArmPoint")
            put(12, "wirePoint")
            put(13, "earthWirePoint")
        }

        /**
         * 按位提取支持的识别物列表，以，隔开
         */
        fun getAiObjectString(assistValue: Int): String {
            var obj = ""
            AiModels.forEach { (t, u) ->
                if ((assistValue and (1 shl t)) shr t == 1) {
                    obj += if (TextUtils.isEmpty(obj)) {
                        "$u"
                    } else {
                        ",$u"
                    }
                }
            }
            Log.d("AssistPhotoUtils", "getAiObject =${Integer.toBinaryString(assistValue)} $obj")
            return obj
        }


        fun getAssistPhotoValue(objList: List<String>): Int {
            var value = 0
            objList.forEach {
                AiModels.forEach { (t, u) ->
                    if (it.equals(u, true)) {
                        value += 1 shl t
                    }
                }
            }
            return value
        }

        fun getLensTypeValue(valueList: List<LensTypeEnum>) : Int {
            var value = 0
            valueList.forEach {
                value += 1 shl it.typeIndex()
            }
            return value
        }

        fun getLensTypes(value: Int) : List<LensTypeEnum> {
            val indexes = mutableListOf<Int>()
            var n = value
            var index = 0
            while (n != 0) {
                if (n and 1 == 1) {
                    indexes.add(index)
                }
                n = n shr 1
                index++
            }
            return indexes.mapNotNull { getLensType(it) }
        }

        fun getLensType(index: Int) : LensTypeEnum? {
            return when(index) {
                1 -> LensTypeEnum.Zoom
                2 -> LensTypeEnum.TeleZoom
                3 -> LensTypeEnum.Thermal
                4 -> LensTypeEnum.TeleThermal
                5 -> LensTypeEnum.WideAngle
                6 -> LensTypeEnum.NightVision
                7 -> LensTypeEnum.Visible
                8 -> LensTypeEnum.Telephoto
                else -> null
            }
        }

        fun isLensFollowed(value: Int) : Boolean {
            return value.and(1) == 1
        }

    }
}

fun LensTypeEnum.typeIndex() : Int {
    return when(this) {
        LensTypeEnum.Zoom -> 1
        LensTypeEnum.TeleZoom -> 2
        LensTypeEnum.Thermal -> 3
        LensTypeEnum.TeleThermal -> 4
        LensTypeEnum.WideAngle -> 5
        LensTypeEnum.NightVision -> 6
        LensTypeEnum.Visible -> 7
        LensTypeEnum.Telephoto -> 8

        else -> 0
    }
}