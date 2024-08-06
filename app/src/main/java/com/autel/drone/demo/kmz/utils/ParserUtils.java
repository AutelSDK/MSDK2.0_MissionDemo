package com.autel.drone.demo.kmz.utils;


import com.autel.data.bean.entity.Coordinate3DModel;
import com.autel.msdk.lib.domain.model.map.data.AutelLatLng;

import kotlin.text.Regex;

public class ParserUtils {

    public static  final String TAG ="ParserUtils-KML";
    public static final Double EARTH_RADIUS = 6378137.0;

    /***
     * 坐标组解析
     * @param coordinates
     * @return
     */
    public static String[] parserCoordinates(String coordinates){
        //排除 \f:换页符 \n:换行符 \r:回车符 \t:制表符
        Regex reg = new Regex("\f|\n|\r|\t");
        String modified = reg.replace(coordinates.trim(), "");
        //用多空格split
        return modified.split("\\s+");
    }

    /***
     * 坐标点解析
     * @param coordinates
     * @return
     */
    public static Coordinate3DModel parserPoint(String coordinates) {
        String[] pointStr = coordinates.trim().split(",");
        if (pointStr == null || pointStr.length < 2) {
            return null;
        }

        try {
            Coordinate3DModel point = new Coordinate3DModel();
            point.setLongitude(Double.parseDouble(pointStr[0]));
            point.setLatitude(Double.parseDouble(pointStr[1]));
            if (pointStr.length > 2) {
                point.setAltitude(Double.parseDouble(pointStr[2]));
            }
            return point;
        } catch (Exception e) {
            KMLLog.Companion.e(TAG, "parserPoint fail=" + e);
            e.printStackTrace();
        }
        return null;
    }

    public static String parserCameraAction(int type) {
        switch (type) {
            case 1: case 11:
                return "takePhoto";
            case 2: case 12:
                return "startRecord";
            case 3: case 13:
                return "timeTakePhoto";
            case 4:
                return "distanceTakePhoto";
            case 6:
                return "stopRecord";
            case 7:
                return "stopTakePhoto";
            case 10: case 14:
                return "noAction";
        }
        return "noAction";
    }

    //获取两个点之间的距离
    /** 地球半径，单位m*/
    public static double getDistance(AutelLatLng point1, AutelLatLng point2) {
        double latitude1 = Math.toRadians(point1.getLatitude());
        double latitude2 = Math.toRadians(point2.getLatitude());
        double longitude1 = Math.toRadians(point1.getLongitude());
        double longitude2 = Math.toRadians(point2.getLongitude());
        double a = latitude1 - latitude2;
        double b = longitude1 - longitude2;
        //计算两点间距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2.0) + Math.cos(latitude1) * Math.cos(latitude2) * Math.pow(Math.sin(b / 2), 2.0)));
         s *= EARTH_RADIUS;
        return s;
    }

}
