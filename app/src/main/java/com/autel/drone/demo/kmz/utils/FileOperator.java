package com.autel.drone.demo.kmz.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

public class FileOperator {
    public static final String TAG = "FileOperator-KML";

    //wpmz目录
    public static final String KML_DIR = "wpmz";
    //资源文件目录
    public static final String KML_RES_DIR = "res/image/";

    //原始资源文件目录
    public static final String KML_ORIG_DIR = "res/source";

    //航点可执行任务文件扩展名
    public static final String WPML_FILE = "wpml";
    //航点模板文件扩展名
    public static final String KML_FILE = "kml";

    //模板文件保存名
    public static final String TEMPLATE_FILE_NAME = "template.kml";
    //执行文件保存名
    public static final String WPML_FILE_NAME = "waylines.wpml";

    public static String readFileContent(File file) throws FileNotFoundException {
        FileInputStream input = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(input);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(inputStreamReader);
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

    public static void saveFile(String str) {
        FileWriter fw = null;
        try {
            fw = new FileWriter("/mnt/sdcard/json" + System.currentTimeMillis() + ".txt");
            fw.write(str);
            fw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean saveKmlFile(String kmlStr, String dirPath, String name) {
        boolean success = false;
        FileWriter fw = null;
        try {
            fw = new FileWriter(dirPath + File.separator + name);
            fw.write(kmlStr);
            fw.flush();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "makeKMLFile error1:" + e);
            success =  false;
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    KMLLog.Companion.e(TAG, "makeKMLFile error2:" + e);
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    /**
     * 获取 kml 或者 wpml 文件
     * @param dirPath  查找文件路径
     * @param FileType 文件类型
     * @return file
     */
    public static File getParseFile(String dirPath, String FileType) {
        KMLLog.Companion.d(TAG, "getParseFile=" + dirPath);
        try {
            File file = new File(dirPath);
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                KMLLog.Companion.e(TAG, "getParseFile is empty Dir");
                return null;
            }

            for (File f : files) {
                if (f.isDirectory()) {
                    if (KML_DIR.equals(f.getName())) {
                        File[] kmlFiles = f.listFiles();
                        for (File kmlFile : kmlFiles) {
                            if (kmlFile.isFile() && kmlFile.getName().endsWith(FileType)) {
                                return kmlFile;
                            }
                        }
                    } else {
                        return getParseFile(f.getAbsolutePath(), FileType);
                    }
                }
            }
        } catch (Exception e) {
            KMLLog.Companion.e(TAG, "getParseFile error=" + e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 生成保存文件目录
     */
    public static String makeSaveDir(String saveDirPath) {
        try {
            String dir = saveDirPath + File.separator + KML_DIR;
            File dirTmp = new File(dir);
            boolean success = true;
            if (dirTmp.exists()) {
                deleteDirFile(dirTmp);
            } else {
                success = dirTmp.mkdirs();
            }
            if (success) {
                return dirTmp.getAbsolutePath();
            } else {
                KMLLog.Companion.e(TAG, "make dir error1=" + dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "make dir error2=" + e);
        }
        return null;
    }

    /**
     * 生成资源文件保存目录
     */
    public static String makeResSaveDir(String wpmzDirPath) {
        try {
            String dir = wpmzDirPath + File.separator + KML_RES_DIR;
            File dirTmp = new File(dir);
            boolean success = true;
            if (dirTmp.exists()) {
                deleteDirFile(dirTmp);
            } else {
                success = dirTmp.mkdirs();
            }
            if (success) {
                return dirTmp.getAbsolutePath();
            } else {
                KMLLog.Companion.e(TAG, "make dir error1=" + dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "make dir error2=" + e);
        }
        return null;
    }

    /**
     * 生成原始资源文件保存目录
     */
    public static String makeOrigResSaveDir(String wpmzDirPath) {
        try {
            String dir = wpmzDirPath + File.separator + KML_ORIG_DIR;
            File dirTmp = new File(dir);
            boolean success = true;
            if (dirTmp.exists()) {
                deleteDirFile(dirTmp);
            } else {
                success = dirTmp.mkdirs();
            }
            if (success) {
                return dirTmp.getAbsolutePath();
            } else {
                KMLLog.Companion.e(TAG, "make dir error1=" + dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            KMLLog.Companion.e(TAG, "make dir error2=" + e);
        }
        return null;
    }


    /***
     * 删除目录下文件
     * @param file
     */
    public static void deleteDirFile(File file) {
        try {
            if (file.isFile()) {
                file.delete();
                KMLLog.Companion.d(TAG, "del1=" + file.getName());
                return;
            } else {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirFile(files[i]);
                    } else {
                        files[i].delete();
                        KMLLog.Companion.d(TAG, "del2=" + files[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 删除目录下所有文件
     * @param filePath
     */
    public static void deleteDir(String filePath) {
        try {
            File file = new File(filePath);
            if (file.isFile()) {
                file.delete();
                return;
            } else {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File tmp : files) {
                        if (tmp.isDirectory()) {
                            deleteDir(tmp.getPath());
                        } else {
                            tmp.delete();
                        }
                    }
                }
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectory(File source, File destination) {
        try {
            if (source.isDirectory()) {
                if (!destination.exists()) {
                    destination.mkdir();
                }

                String[] files = source.list();

                if (files != null) {
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(destination, file);

                        copyDirectory(srcFile, destFile);
                    }
                }
            } else {
                FileChannel sourceChannel = null;
                FileChannel destinationChannel = null;

                try {
                    sourceChannel = new FileInputStream(source).getChannel();
                    destinationChannel = new FileOutputStream(destination).getChannel();
                    destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                } finally {
                    if (sourceChannel != null) {
                        sourceChannel.close();
                    }
                    if (destinationChannel != null) {
                        destinationChannel.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(String filePath, String destDir) {
        try {
            File sourceFile = new File(filePath);
            File destinationDir = new File(destDir);

            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(new File(destinationDir, sourceFile.getName()));

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            fis.close();
            // 复制成功
        } catch (IOException e) {
            e.printStackTrace();
            // 复制失败
        }
    }
}
