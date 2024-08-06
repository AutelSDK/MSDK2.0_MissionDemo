package com.autel.drone.demo.kmz

import android.text.TextUtils
import com.autel.data.bean.entity.MissionFlightModel
import com.autel.drone.demo.MissionType
import com.autel.drone.demo.kmz.utils.FileOperator
import com.autel.drone.demo.kmz.utils.GimbalInfoUtils
import com.autel.drone.demo.kmz.utils.KMLLog
import com.autel.drone.demo.kmz.utils.KmlCode
import com.autel.drone.demo.kmz.utils.ZipFileOperator
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.XmlUtils
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Kml
import com.autel.internal.mission.MsnInfoUsr
import com.autel.mission.kml.packager.BasePackager
import com.autel.mission.kml.packager.KmlPackager
import com.autel.mission.kml.packager.WpmlPackager
import java.io.File

/***
 * This demo only how to make a kmz mission file
 * MissionFlightModel is only waypoint airline business model,just replace it by your data model
 */

class MissionToKmlProcessor {

    companion object {
        const val TAG = "MissionToKmlProcessor-KML"
        //KMZ打包预览原图，而不是裁剪图片，因为增加了裁剪框参数
        const val UPLOAD_ORIGINAL_PIC = true
    }

    /**
     * 任务生成 kmz 文件上传
     */
    suspend fun saveMissionAsKmz(
        flightModel: MissionFlightModel,
        msnInfoUsr: MsnInfoUsr,
        saveDir: String,
        callBack: (Int, String) -> Unit
    ) {

        KMLLog.i(TAG, "saveMissionAsKmz saveDir=$saveDir")

        var tmpDir = saveDir + "tmp_${System.currentTimeMillis()}"

        var dir = FileOperator.makeSaveDir(tmpDir)
        var resDir = FileOperator.makeResSaveDir(dir)

        KMLLog.i(TAG, "saveMissionAsKmz dir=$dir")
        KMLLog.i(TAG, "saveMissionAsKmz resDir=$resDir")

        if (TextUtils.isEmpty(dir)) {
            KMLLog.e(TAG, "make save dir fail.")
            callBack.invoke(KmlCode.DIR_ERROR, "make save dir fail")
            return
        }


        val xmlUtils = XmlUtils<Kml>("Autel")

        val kmlBean = KmlPackager().pack(flightModel)
        val kmlStr = xmlUtils.objectToXml(kmlBean)

        val wpmlPackager = WpmlPackager()

        //KMZ resource for AccurateRetake, 精准复拍资源
        wpmlPackager.addResourceListener(object: BasePackager.OnResourceListener{
            override fun onResource(original: String?, crop: String?) {
                if(UPLOAD_ORIGINAL_PIC){
                    original?.let { FileOperator.copyFile(original, resDir) }
                } else {
                    crop?.let { FileOperator.copyFile(crop, resDir) }
                }
            }
        })

        val wpmlBean = if (flightModel.droneCount > 1 || flightModel.missionType != MissionType.WAYPOINT.value) {
            wpmlPackager.pack(flightModel, msnInfoUsr)
        } else {
            wpmlPackager.pack(flightModel)
        }
        val wpmlStr = xmlUtils.objectToXml(wpmlBean)

        val success = FileOperator.saveKmlFile(kmlStr, dir, FileOperator.TEMPLATE_FILE_NAME)
        val success1 = FileOperator.saveKmlFile(wpmlStr, dir, FileOperator.WPML_FILE_NAME)
        //delay(500)
        if (success && success1) {
            val outFile = saveDir + "${System.currentTimeMillis()}.kmz"
            val result = ZipFileOperator.zip(dir, outFile)
            FileOperator.deleteDir(tmpDir)
            if (result) {
                callBack.invoke(KmlCode.KMZ_SUCCESS, outFile)
                KMLLog.i(TAG, "kmz success.")
            } else {
                callBack.invoke(KmlCode.ZIP_ERROR, "zip error")
                KMLLog.e(TAG, "zip error.")
            }
        } else {
            callBack.invoke(KmlCode.SAVE_ERROR, "save kml fail")
            KMLLog.e(TAG, "save kml fail.")
        }
    }

    /**
     * 导出任务为 kmz
     */
    suspend fun exportMissionAsKmz(
        flightModel: MissionFlightModel,
        saveDir: String,
        callBack: (Int, String) -> Unit
    ) {
        var missionName = flightModel.summaryTaskInfoModel?.name
        missionName = if(TextUtils.isEmpty(missionName)){
            "mission_${System.currentTimeMillis()}"
        } else {
            missionName!!.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }

        KMLLog.i(TAG, "exportMissionAsKmz saveDir=$saveDir")

        val tmpDir = saveDir + File.separator + System.currentTimeMillis()

        val dir = FileOperator.makeSaveDir(tmpDir)
        val resDir = FileOperator.makeResSaveDir(dir)
        val origResDir = FileOperator.makeOrigResSaveDir(dir)

        KMLLog.i(TAG, "exportMissionAsKmz resDir=$resDir")
        KMLLog.i(TAG, "exportMissionAsKmz origResDir=$origResDir")

        if (TextUtils.isEmpty(dir)) {
            KMLLog.e(TAG, "make save dir fail.")
            callBack.invoke(KmlCode.DIR_ERROR, "make dir error")
            return
        }

        val gimbalType = GimbalTypeEnum.find(flightModel.gimbalType)
        GimbalInfoUtils.updateGimbal(gimbalType)
        flightModel.gimbalType = gimbalType.value
        val xmlUtils = XmlUtils<Kml>("Autel")

        val kmlBean = KmlPackager().pack(flightModel)
        val kmlStr = xmlUtils.objectToXml(kmlBean)

        val wpmlPackager = WpmlPackager()
        //精准复拍资源图片
        wpmlPackager.addResourceListener(object: BasePackager.OnResourceListener{
            override fun onResource(original: String?, crop: String?) {
                original?.let { FileOperator.copyFile(original, resDir) }
            }
        })

        val wpmlBean = wpmlPackager.pack(flightModel)
        val wpmlStr = xmlUtils.objectToXml(wpmlBean)

        val success = FileOperator.saveKmlFile(kmlStr, dir, FileOperator.TEMPLATE_FILE_NAME)
        val success1 = FileOperator.saveKmlFile(wpmlStr, dir, FileOperator.WPML_FILE_NAME)
        if (success && success1) {
            val outFile = saveDir + File.separator + "$missionName.kmz"
            val result = ZipFileOperator.zip(dir, outFile)
            FileOperator.deleteDir(tmpDir)
            if (result) {
                callBack.invoke(KmlCode.KMZ_SUCCESS, outFile)
                KMLLog.i(TAG, "kmz success.")
            } else {
                callBack.invoke(KmlCode.ZIP_ERROR, "zip error")
                KMLLog.e(TAG, "zip error.")
            }
        } else {
            callBack.invoke(KmlCode.SAVE_ERROR, "save kml error")
            KMLLog.e(TAG, "save kml fail.")
        }
    }

    suspend fun exportWPMLAsPath(
        flightModel: MissionFlightModel,
        saveDir: String
    ) : String {
        var missionName = flightModel.summaryTaskInfoModel?.name
        missionName = if(TextUtils.isEmpty(missionName)){
            "mission_${System.currentTimeMillis()}"
        } else {
            missionName!!.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }
        val wpmlFile = File(saveDir + File.separator + missionName)
        if (wpmlFile.exists()) {
            wpmlFile.delete()
        }

        val xmlUtils = XmlUtils<Kml>("Autel")

        val wpmlPackager = WpmlPackager()
        val wpmlBean = wpmlPackager.pack(flightModel)
        val wpmlStr = xmlUtils.objectToXml(wpmlBean)
        val success1 = FileOperator.saveKmlFile(wpmlStr, saveDir, missionName)
        if (success1) {
            return wpmlFile.path
        } else {
            return ""
        }
    }

    suspend fun exportWPMLAsPath(
        flightModel: MissionFlightModel,
        msnInfoUsr: MsnInfoUsr,
        saveDir: String
    ) : String {
        var missionName = flightModel.summaryTaskInfoModel?.name
        missionName = if(TextUtils.isEmpty(missionName)){
            "mission_${System.currentTimeMillis()}"
        } else {
            missionName!!.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }
        val wpmlFile = File(saveDir + File.separator + missionName)
        if (wpmlFile.exists()) {
            wpmlFile.delete()
        }

        val xmlUtils = XmlUtils<Kml>("Autel")

        val wpmlPackager = WpmlPackager()
        val wpmlBean = wpmlPackager.pack(flightModel, msnInfoUsr)
        val wpmlStr = xmlUtils.objectToXml(wpmlBean)
        val success1 = FileOperator.saveKmlFile(wpmlStr, saveDir, missionName)
        if (success1) {
            return wpmlFile.path
        } else {
            return ""
        }
    }

    suspend fun exportWPMLAsString(
        flightModel: MissionFlightModel,
        saveDir: String
    ) : String {
        var missionName = flightModel.summaryTaskInfoModel?.name
        missionName = if(TextUtils.isEmpty(missionName)){
            "mission_${System.currentTimeMillis()}"
        } else {
            missionName!!.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }
        val wpmlFile = File(saveDir + File.separator + missionName)
        if (wpmlFile.exists()) {
            wpmlFile.delete()
        }

        val xmlUtils = XmlUtils<Kml>("Autel")

        val wpmlPackager = WpmlPackager()
        val wpmlBean = wpmlPackager.pack(flightModel)
        return xmlUtils.objectToXml(wpmlBean)
    }

    suspend fun exportWPMLAsString(
        flightModel: MissionFlightModel,
        msnInfoUsr: MsnInfoUsr,
        saveDir: String
    ) : String {
        var missionName = flightModel.summaryTaskInfoModel?.name
        missionName = if(TextUtils.isEmpty(missionName)){
            "mission_${System.currentTimeMillis()}"
        } else {
            missionName!!.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }
        val wpmlFile = File(saveDir + File.separator + missionName)
        if (wpmlFile.exists()) {
            wpmlFile.delete()
        }

        val xmlUtils = XmlUtils<Kml>("Autel")

        val wpmlPackager = WpmlPackager()
        val wpmlBean = wpmlPackager.pack(flightModel, msnInfoUsr)
        return xmlUtils.objectToXml(wpmlBean)
    }
}