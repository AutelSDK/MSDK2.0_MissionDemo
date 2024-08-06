package com.autel.drone.demo

import android.util.Log
import com.autel.internal.mission.v2.MissionInfoJNI
import com.autel.internal.mission.v2.PoiPointJNI
import com.autel.internal.mission.v2.WaypointInfoJNI
import com.autel.sdk.mission.wp.v2.CameraActionJNI
import com.autel.sdk.mission.wp.v2.MissionConfig
import com.autel.sdk.mission.wp.v2.PathResultLine
import com.autel.sdk.mission.wp.v2.PathResultMission
import java.math.BigDecimal
import java.util.*


/**
 * 航线规划结果转换类，[AirLineCreator]规划的结果，转换成生成二进制需要的结果
 *
 * 逻辑同步于AirLineCreator#writeMissionFile
 */
class AirLineWriter {

    companion object {

        /**
         * 航点任务
         * 将规划的结果转换成生成aut文件的入参
         */
        fun writeMissionFile(
            result: PathResultMission,
        ): MissionInfoJNI{
            val cfg = createMissionConfigFromMissionModel()

            //航点任务
            /* 默认相机动作:
            LEG_TAKE_PHOTO(1),  //航段动作-拍照
            LEG_RECORD(2), //航段动作-录像
            LEG_TIMELAPSE(3),  //航段动作-定时拍
            LEG_DISTANCE(4),  //航段动作-定距拍照
            LEG_DISTANCE_SCAN(5), //航段动作-定距扫拍
            LEG_STOP_RECORD(6),  //航段动作-结束录像
            LEG_STOP_PHOTO(7),  //航段动作-停止拍照
            LEG_NONE(10)*/   //航段动作-无
            // 默认相机动作
            cfg.defaultAction = 2
            //云台俯仰角
            cfg.gimbalPitch = 0f //俯范围：-30~90（单位：度）

            Log.i("missionCfg", "cfg =$cfg")
            return writeMissionFile(cfg, result)
        }

        /**
         * 生成任务文件，任务以文件的方式上传下载
         *
         * @param filePath 生成的文件路径
         * @param cfg
         * @param m
         * @return 返回结果: 0:正常;-1:路径不能为空;-2:创建文件目录失败;-3:创建文件目录异常;-4:任务航线不能为空
         */
        private fun writeMissionFile(cfg: MissionConfig, m: PathResultMission): MissionInfoJNI {
            val info: MissionInfoJNI = createInfo(cfg)
            info.Waypoint_Num = m.lineSize()
            info.Mission_Time = (m.T_ttl_fly * 100).toInt()
            info.Mission_Length = (m.L_ttl_fly * 100).toInt()
            info.Waypoints = arrayOfNulls(info.Waypoint_Num)
            fillWpList(info.Waypoints, m.FP_Info_strc, 0, m.FP_Info_strc.size, 0)
            return info
        }

        private fun fillWpList(
            target: Array<WaypointInfoJNI>,
            src: Array<PathResultLine>,
            offset: Int,
            size: Int,
            index: Int
        ): Int {
            for (i in 0 until size) {
                val waypointInfoJNI = WaypointInfoJNI()
                waypointInfoJNI.Waypoint_Type = src[i].WPTypeExe.toInt()
                waypointInfoJNI.Prev_Latitude = doubleToInt(src[i].WPPrevLLAExe[0])
                waypointInfoJNI.Prev_Longitude = doubleToInt(src[i].WPPrevLLAExe[1])
                waypointInfoJNI.Prev_Altitude = (src[i].WPPrevLLAExe[2] * 1000).toInt()
                waypointInfoJNI.Cur_Latitude = doubleToInt(src[i].WPCurrLLAExe[0])
                waypointInfoJNI.Cur_Longitude = doubleToInt(src[i].WPCurrLLAExe[1])
                waypointInfoJNI.Cur_Altitude = (src[i].WPCurrLLAExe[2] * 1000).toInt()

                waypointInfoJNI.Center_Latitude = doubleToInt(src[i].WPCentLLAExe[0])
                waypointInfoJNI.Center_Longitude = doubleToInt(src[i].WPCentLLAExe[1])
                waypointInfoJNI.Center_Altitude = (src[i].WPCentLLAExe[2] * 1000).toInt()

                waypointInfoJNI.Velocity_Ref = src[i].VelRef_FP.toFloat()
                waypointInfoJNI.Velocity_Ref_Next = src[i].VelRefNxt_FP.toFloat()
                waypointInfoJNI.Altitude_Priority = src[i].AltPrio_FP.toInt()
                waypointInfoJNI.Heading_Mode = src[i].Heading_Mode_FP.toInt()
                waypointInfoJNI.Action_Num = src[i].ActionNum_FP.toInt()
                waypointInfoJNI.POI_Valid = src[i].POI_Valid_FP.toInt()
                waypointInfoJNI.setEmgAct_FP(src[i].EmgAct_FP)
                waypointInfoJNI.setIndex(index)
                waypointInfoJNI.FP_Time = (src[i].T_curr * 100).toInt()
                waypointInfoJNI.FP_Length = (src[i].FP_length * 100).toInt()
                if (src[i].POI_FP != null) {
                    val poiPoint = PoiPointJNI()
                    poiPoint.Latitude = doubleToInt(src[i].POI_FP[0])
                    poiPoint.Longitude = doubleToInt(src[i].POI_FP[1])
                    poiPoint.Altitude = (src[i].POI_FP[2] * 1000).toInt()
                    waypointInfoJNI.POI = poiPoint
                }
                val ccSize = src[i].MSN_ActionInfo.size
                val actionJNIList = arrayOfNulls<CameraActionJNI>(ccSize)
                for (k in 0 until ccSize) {
                    val resultCameraAction = src[i].MSN_ActionInfo[k]
                    val actionJNI = CameraActionJNI()
                    actionJNI.Action_Type = resultCameraAction.Action_Type.toInt()
                    actionJNI.Gimbal_Pitch = resultCameraAction.Gimbal_Pitch
                    actionJNI.Gimbal_Roll = resultCameraAction.Gimbal_Roll
                    actionJNI.Action_Yaw_Ref = resultCameraAction.Action_Yaw_Ref
                    actionJNI.Shoot_Time_Interval = resultCameraAction.Shoot_Time_Interval.toInt()
                    actionJNI.Shoot_Dis_Interval = resultCameraAction.Shoot_Dis_Interval * 1000
                    actionJNI.Action_Time = resultCameraAction.Action_Time.toInt()
                    actionJNI.Zoom_Rate = resultCameraAction.Zoom_Rate.toInt()
                    actionJNI.reserved = intArrayOf(0)
                    actionJNIList[k] = actionJNI
                }
                waypointInfoJNI.Actions = actionJNIList
                target[offset + i] = waypointInfoJNI
            }
            return offset + size
        }

        private fun doubleToInt(d: Double): Long {
            val bd = BigDecimal(d).setScale(7, BigDecimal.ROUND_HALF_UP)
            val bd1 = BigDecimal(10E6)
            return bd.multiply(bd1).toLong()
        }

        /**
         * 从PathPlaningUtils#writeMissionFile同步过来
         */
        private fun createMissionConfigFromMissionModel(): MissionConfig {
            val cfg = MissionConfig()

            cfg.id = 0 //当前任务下标，默认0
            cfg.altitudeType = 0 //航点高度类型: 相对高度：0, 海拔高度：1
            cfg.finishAction = 0 //任务完成动作: 返航：0， 悬停：1
            cfg.lossAction = 1 //任务失联动作: 返航：1，继续任务： 2
            cfg.vFov = 30.0f //相机视场角: 目前固定值 30
            cfg.obstacleMode = 0 //任务避障模式: 关闭：0 悬停：1 绕障：2
            cfg.type = 0 //任务类型：航点任务：0

            return cfg
        }


        /**
         * 代码同步于AirLineCreator#createInfo
         */
        private fun createInfo(cfg: MissionConfig): MissionInfoJNI {
            val missionInfoJNI = MissionInfoJNI()
            missionInfoJNI.Mission_ID = cfg.id
            missionInfoJNI.Altitude_type = cfg.altitudeType
            missionInfoJNI.Mission_type = cfg.type
            missionInfoJNI.Finish_Action = cfg.finishAction
            missionInfoJNI.RC_Lost_Action = cfg.lossAction
            missionInfoJNI.Obstacle_Mode = cfg.obstacleMode
            missionInfoJNI.VFOV_Mapping = cfg.vFov
            missionInfoJNI.Gride_Enable_Mapping = if (cfg.doubleGrid) 1 else 0
            missionInfoJNI.Yaw_Ref_Mapping = cfg.yawRef
            missionInfoJNI.Overlap_Mapping = (cfg.courseRate * 100).toInt()
            missionInfoJNI.Gimbal_Pitch_Mapping = cfg.gimbalPitch
            //0bit 是否打开高程优化 1bit 是否打开侧向避障
            if (cfg.altOptim) {
                missionInfoJNI.reserved[0] = missionInfoJNI.reserved[0] or 1
            } else {
                missionInfoJNI.reserved[0] = missionInfoJNI.reserved[0] and 1.inv()
            }
            if (cfg.sideObstacle == 1) {
                missionInfoJNI.reserved[0] = missionInfoJNI.reserved[0] or 2
            } else {
                missionInfoJNI.reserved[0] = missionInfoJNI.reserved[0] and 2.inv()
            }
            missionInfoJNI.Min_OA_Dist = cfg.Min_OA_Dist
            missionInfoJNI.POI_Num = cfg.poiNum
            missionInfoJNI.Action_Default.Gimbal_Pitch = cfg.gimbalPitch
            missionInfoJNI.Action_Default.Action_Yaw_Ref = cfg.yawRef
            missionInfoJNI.Action_Default.Action_Type = 0
            missionInfoJNI.Action_Default.Shoot_Dis_Interval = 2000f
            missionInfoJNI.GUID = (System.currentTimeMillis() / 1000)
            return missionInfoJNI
        }
    }
}