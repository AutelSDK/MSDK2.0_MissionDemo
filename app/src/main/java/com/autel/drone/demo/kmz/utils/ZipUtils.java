package com.autel.drone.demo.kmz.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Java utils 实现的Zip工具
 */
public class ZipUtils {
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte
    public static String zipName;


    /**
     * 批量压缩文件（夹）
     *
     * @param resFileList 要压缩的文件（夹）列表
     * @param zipFile     生成的压缩文件
     * @throws IOException 当压缩过程出错时抛出
     */
    public static void zipFiles(Collection<File> resFileList, File zipFile)
            throws IOException {
        if (zipFile.exists())
            zipFile.delete();
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(zipFile), BUFF_SIZE));
        for (File resFile : resFileList) {
            zipFile(resFile, zipOut, "");
        }
        zipOut.close();
    }

    public static void zipFile(File resFileList, File zipFile)
            throws IOException {
        if (zipFile.exists())
            zipFile.delete();
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(zipFile), BUFF_SIZE));
        zipFile(resFileList, zipOut, "");
        zipOut.close();
    }

    /**
     * 批量压缩文件（夹）
     *
     * @param resFileList 要压缩的文件（夹）列表
     * @param zipFile     生成的压缩文件
     * @param comment     压缩文件的注释
     * @throws IOException 当压缩过程出错时抛出
     */
    public static void zipFiles(Collection<File> resFileList, File zipFile,
                                String comment) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(zipFile), BUFF_SIZE));
        for (File resFile : resFileList) {
            zipFile(resFile, zipOut, "");
        }
        zipOut.setComment(comment);
        zipOut.close();
    }

    /**
     * 压缩文件
     *
     * @param resFile  需要压缩的文件（夹）
     * @param zipOut   压缩的目的文件
     * @param rootPath 压缩的文件路径
     * @throws FileNotFoundException 找不到文件时抛出
     * @throws IOException           当压缩过程出错时抛出
     */
    public static void zipFile(File resFile, ZipOutputStream zipOut,
                               String rootPath) throws IOException {

        rootPath = rootPath + (rootPath.trim().length() == 0 ? "" : File.separator)
                + resFile.getName();

        rootPath = new String(rootPath.getBytes(), StandardCharsets.UTF_8);
        if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles();
            for (File file : fileList) {
                zipFile(file, zipOut, rootPath);
            }
        } else {
            byte buffer[] = new byte[BUFF_SIZE];
            BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(resFile), BUFF_SIZE);
            zipOut.putNextEntry(new ZipEntry(rootPath));
            int realLength;
            while ((realLength = in.read(buffer)) != -1) {
                zipOut.write(buffer, 0, realLength);
            }
            in.close();
            zipOut.flush();
            zipOut.closeEntry();
            // resFile.delete();
        }
    }

    /**
     * upZipFile:(解压缩文件)
     * 将zipFile文件解压到folderPath目录下)
     */
    public static void upZipFile(File zipFile, String folderPath) throws IOException {

        ZipFile zFile = new ZipFile(zipFile);
        Enumeration zList = zFile.entries();
        ZipEntry ze = null;
        byte[] buf = new byte[1024];
        while (zList.hasMoreElements()) {
            ze = (ZipEntry) zList.nextElement();
            Log.d("-KML", "folderPath = " + folderPath);
            if (ze.isDirectory()) {
                String dirstr = folderPath + ze.getName();
                dirstr = new String(dirstr.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                File f = new File(dirstr);
                f.mkdir();
                continue;
            }
            String mDirPath = ze.getName();
            if (mDirPath.contains("/") || mDirPath.contains("\\")) {
                mDirPath = mDirPath.replace("\\", "/");
                String unDir = mDirPath.substring(0, mDirPath.indexOf("/"));
                if (!TextUtils.isEmpty(unDir)) {
                    String dirStr = folderPath + "/" + unDir;
                    dirStr = new String(dirStr.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                    File f = new File(dirStr);
                    f.mkdir();
                }
            }
            Log.d("-KML", "ze.getName() = " + mDirPath);
            OutputStream os = new BufferedOutputStream(
                    new FileOutputStream(getRealFileName(folderPath, mDirPath)));
            zipName = ze.getName();
            InputStream is = new BufferedInputStream(zFile.getInputStream(ze));
            int readLen = 0;
            while ((readLen = is.read(buf, 0, 1024)) != -1) {
                os.write(buf, 0, readLen);
            }
            is.close();
            os.close();
        }
        zFile.close();
    }

    /**
     * getRealFileName:(给定根目录，返回一个相对路径所对应的实际文件名)
     *
     * @param baseDir     指定根目录
     * @param absFileName 相对路径名，来自于ZipEntry中的那么
     * @return java.io.File 实际的文件
     */
    private static File getRealFileName(String baseDir, String absFileName) {

        String[] dirs = absFileName.split("/");
        File ret = new File(baseDir);
        ret.mkdirs();
        String substr = null;
        if (dirs.length > 0) {
            for (int i = 0; i < dirs.length - 1; i++) {
                substr = dirs[i];
                ret = new File(ret, substr);

            }
            if (!ret.exists()) {
                ret.mkdirs();
            }
            substr = dirs[dirs.length - 1];
            ret = new File(ret, substr);
            return ret;
        }

        return ret;

    }

    /**
     * 通过Zlib压缩数据
     *
     * @param data
     * @return
     */
    public static byte[] compress(String data) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DeflaterOutputStream zos = new DeflaterOutputStream(bos);
            zos.write(data.getBytes());
            zos.close();
            bytes = bos.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bytes;
    }

    /**
     * 解压数据
     *
     * @param data
     * @return
     */
    public static String deCompress(byte[] data) {
        Inflater decompresser = new Inflater();
        decompresser.setInput(data, 0, data.length);
        // 对byte[]进行解压，同时可以要解压的数据包中的某一段数据，就好像从zip中解压出某一个文件一样。
        byte[] result = new byte[4096 * 1024];
        int resultLength = 0;
        try {
            resultLength = decompresser.inflate(result); // 返回的是解压后的的数据包大小，
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        decompresser.end();
        return new String(result).substring(0, resultLength);
    }
}