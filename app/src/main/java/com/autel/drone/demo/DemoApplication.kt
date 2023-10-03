package com.autel.drone.demo

import android.app.Application
import com.autel.drone.sdk.libbase.error.AutelError
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.interfaces.SDKManagerCallback

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SDKManager.get().init(applicationContext, true)

        println("SDKManager V=${SDKManager.get().getSDKVersion()}")
    }
}