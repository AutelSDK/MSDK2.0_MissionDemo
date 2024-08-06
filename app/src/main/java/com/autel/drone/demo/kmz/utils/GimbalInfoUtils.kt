package com.autel.drone.demo.kmz.utils

import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.module.camera.ICameraSupport2
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *@Author autel
 *@Date 2024/4/22
 *
 */
object GimbalInfoUtils {
    private var gimbalMap = mutableMapOf<GimbalTypeEnum, ICameraSupport2>()
   suspend fun updateGimbal(gimbalTypeEnum: GimbalTypeEnum) {
       if (gimbalMap[gimbalTypeEnum] != null) return
        val camera = suspendCoroutine<ICameraSupport2?> {continuation->
            DeviceManager.getDeviceManager().getCameraSupport(gimbalTypeEnum) {
                continuation.resume(it)
            }
        }
       camera?.let {
           gimbalMap[gimbalTypeEnum] = it
       }
   }
    fun getGimbalSupport(gimbalType: GimbalTypeEnum) : ICameraSupport2? {
        return gimbalMap[gimbalType]
    }
    fun getPriorityLensType(list: List<LensTypeEnum>) : LensTypeEnum? {
        listOf(LensTypeEnum.Visible, LensTypeEnum.Zoom, LensTypeEnum.WideAngle, LensTypeEnum.NightVision, LensTypeEnum.TeleZoom, LensTypeEnum.Thermal).forEach {
            if (list.contains(it)) {
                return it
            }
        }
        return list.firstOrNull()
    }

}