package com.autel.drone.demo.kmz.utils;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileOperator {
    public static final String TAG = "ZipFileOperator-KML";


    /**
     * 压缩文件和文件夹
     * @param srcFilePath 要压缩的文件或文件夹
     * @param outFilePath 压缩完成的Zip路径
     */
    public static boolean zip(String srcFilePath, String outFilePath) {
        ZipOutputStream outZip = null;
        boolean success = false;
        try {
            outZip = new ZipOutputStream(new FileOutputStream(outFilePath));
            File file = new File(srcFilePath);
            ZipUtils.zipFile(file, outZip, "");
            outZip.finish();
            outZip.flush();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "zip folder fail =" + e);
            success =  false;
        } finally {
            if(outZip != null){
                try {
                    outZip.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            fileSync(outFilePath);
        }
        return success;
    }


    /**
     * 强制文件落盘
     */
    public static void fileSync(String path) {
        try {
            KMLLog.Companion.i(TAG, "zip folder fileSync="+path);
            FileOutputStream fileOutputStream = new FileOutputStream(path,true);
            FileDescriptor fileDescriptor = fileOutputStream.getFD();
            fileOutputStream.flush();
            fileDescriptor.sync();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "zip fileSync =" + e);
        }
    }
}