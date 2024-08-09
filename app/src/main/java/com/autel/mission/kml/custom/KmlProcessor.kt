package com.autel.mission.kml.custom

import android.text.TextUtils
import android.util.Log
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.XmlUtils
import com.autel.drone.sdk.vmodelx.module.fileservice.kml.models.Kml
import com.google.gson.Gson
import java.io.File


/**
 * @Author create by AUTEL
 * @Date 2022/09/21 11:56
 *
 * 将航线数据模型转换为 KMZ 任务文件
 */
class KmlProcessor {

    companion object {
        const val TAG = "KmlProcessor-KML"

        const val KMZ_SUCCESS = 100 // Export success
        const val DIR_ERROR = 401  //make save dir fail
        const val ZIP_ERROR = 402  //zip kmz error
        const val SAVE_ERROR = 403  //save kmz error
    }

    fun loadMissionFile(){
        val bean = Gson().fromJson(MockData.test1, MissionPath::class.java)
        exportMissionAsKmz(bean, "mnt/sdcard", "testMission"){ code, path->
            Log.i(TAG, "exportMissionAsKmz code=$code, path=$path")
        }
    }

    /**
     * 导出任务为 kmz
     */
    fun exportMissionAsKmz(
        flightModel: MissionPath,
        saveDir: String,
        saveName: String,
        callBack: (Int, String) -> Unit
    ) {
        val missionName = if (TextUtils.isEmpty(saveName)) {
            "mission_${System.currentTimeMillis()}"
        } else {
            saveName.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "")
        }

        Log.i(TAG, "exportMissionAsKmz saveDir=$saveDir")

        val tmpDir = saveDir + File.separator + System.currentTimeMillis()
        val dir = FileOperator.makeSaveDir(tmpDir)

        if (TextUtils.isEmpty(dir)) {
            Log.e(TAG, "make save dir fail.")
            callBack.invoke(DIR_ERROR, "make dir error")
            return
        }

        val xmlUtils = XmlUtils<Kml>("Autel")

        val kmlBean = CustomKmlPackager().pack(flightModel)
        val kmlStr = xmlUtils.objectToXml(kmlBean)

        val wpmlBean = CustomWpmlPackager().pack(flightModel)
        val wpmlStr = xmlUtils.objectToXml(wpmlBean)

        val success = FileOperator.saveKmlFile(kmlStr, dir, FileOperator.TEMPLATE_FILE_NAME)
        val success1 = FileOperator.saveKmlFile(wpmlStr, dir, FileOperator.WPML_FILE_NAME)

        if (success && success1) {
            val outFile = saveDir + File.separator + "$missionName.kmz"
            val result = ZipFileOperator.zip(dir, outFile)
            FileOperator.deleteDir(tmpDir)
            if (result) {
                callBack.invoke(KMZ_SUCCESS, outFile)
                Log.i(TAG, "kmz success.")
            } else {
                callBack.invoke(ZIP_ERROR, "zip error")
                Log.e(TAG, "zip error.")
            }
        } else {
            callBack.invoke(SAVE_ERROR, "save kml/wpml error")
            Log.e(TAG, "save kml/wpml fail.")
        }
    }
}