package com.autel.drone.demo.kmz.utils

import com.autel.drone.sdk.log.SDKLog

class KMLLog {

    companion object{
        fun i(tag: String, msg:String){
            SDKLog.i(tag, msg)
        }

        fun e(tag: String, msg:String){
            SDKLog.i(tag, msg)
        }

        fun d(tag: String, msg:String){
            SDKLog.i(tag, msg)
        }

        fun w(tag: String, msg:String){
            SDKLog.w(tag, msg)
        }
    }
}