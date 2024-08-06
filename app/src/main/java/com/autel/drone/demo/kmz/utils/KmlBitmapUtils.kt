package com.autel.drone.demo.kmz.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 精准复拍图片资源管理
 */
class KmlBitmapUtils {
    companion object {

        const val TAG = "BitmapUtils-KML"

        private const val KMZ_RES_DIR = "kmz_res"

        private const val KMZ_ORIGINAL_DIR = "kmz_original"

        /**
         * 保存裁剪后的航点 kmz 资源
         * @param context
         * @param recordId :当前 Task 的 Id，唯一即可同一个任务录制的图片保存在同一个目录
         */
        fun getInternalDir(context: Context, recordId: String): String {
            val dir = "${context.cacheDir}${File.separator}$KMZ_RES_DIR${File.separator}${recordId}${File.separator}"
            KMLLog.d(TAG, "getInternalDir =$dir")
            return dir

        }

        /**
         * 保存拍照原图资源，确保能继续编辑
         * @param context
         * @param recordId :当前 Task 的 Id，唯一即可同一个任务录制的图片保存在同一个目录
         */
        fun getExternalDir(context: Context, recordId: String): String {
            val dir = "${context.externalCacheDir}${File.separator}$KMZ_ORIGINAL_DIR${File.separator}${recordId}${File.separator}"
            KMLLog.d(TAG, "getExternalDir =$dir")
            return dir
        }

        /**
         * 删除任务时同时删除对应的资源文件
         */
        fun deleteKmzPic(context: Context?, recordId: String) {
            try {
                context?.let {
                    val resDir = getInternalDir(context, recordId)
                    FileOperator.deleteDir(resDir)
                    val origDir = getExternalDir(context, recordId)
                    FileOperator.deleteDir(origDir)
                    KMLLog.i(TAG, "deleteKmzPic success")
                } ?: run {
                    KMLLog.e(TAG, "deleteKmzPic context is null")
                }
            } catch (e: Exception) {
                KMLLog.i(TAG, "deleteKmzPic ${e}")
                e.printStackTrace()
            }
        }

        /**
         * 保存图片到指定目录
         */
        fun saveKmzRes(bitmap: Bitmap, path: String): Boolean {
            try {
                val filePic = File(path)
                if (!filePic.exists()) {
                    filePic.parentFile.mkdirs()
                    filePic.createNewFile()
                }
                val fos = FileOutputStream(filePic)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                return true
            } catch (e: IOException) {
                e.printStackTrace()
                KMLLog.i(TAG, "bitmap save error!!!!!")
            }
            return false
        }

        fun deleteFile(file: File) {
            try {
                if (null == file || !file.exists()) {
                    return
                }
                if (file.isFile) {
                    if (!file.delete()) {
                        KMLLog.e("deleteFile", " file.delete() fail")
                    }
                } else if (file.isDirectory) {
                    val files: Array<File> = file.listFiles()
                    if (null != files && files.isNotEmpty()) {
                        for (fileItem in files) {
                            deleteFile(fileItem)
                        }
                    }
                }
                if (file.isDirectory) {
                    if (!file.delete()) {
                        KMLLog.e("deleteFile", "file.delete()  fail")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}