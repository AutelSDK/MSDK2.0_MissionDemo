package com.autel.mission.kml.custom;

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.gimbal.enums.GimbalOrientationEnum;

import java.util.List;

public class UavParameters {
    double uav_height_ground; // 无人机离地高度
    List<Double> uav_init_pos; // 初始位置,例: [ 104.377439, 30.285762999999999, 0.0 ],
    double uav_speed_high; // 最高速度
    double uav_speed_low; // 最低速度
    int angle;// 角度
    // 0:DOWN, 1:DOWN_45, 2:FORWARD
    /**
     * enum class GimbalOrientationEnum(var value: Float) {
     *
     * 	// 朝下控制
     * 	DOWN(-(Math.PI / 2).toFloat()),
     *
     * 	// 45度朝下
     * 	DOWN_45(-(Math.PI / 4).toFloat()),
     *
     * 	// 云台归中
     * 	FORWARD(0f);
     * }
     */
    GimbalOrientationEnum gimbalOrientationEnum = GimbalOrientationEnum.FORWARD;
    Boolean bLaserRangingSwitch;

    public double getUav_height_ground() {
        return uav_height_ground;
    }

    public void setUav_height_ground(double uav_height_ground) {
        this.uav_height_ground = uav_height_ground;
    }

    public List<Double> getUav_init_pos() {
        return uav_init_pos;
    }

    public void setUav_init_pos(List<Double> uav_init_pos) {
        this.uav_init_pos = uav_init_pos;
    }

    public double getUav_speed_high() {
        return uav_speed_high;
    }

    public void setUav_speed_high(double uav_speed_high) {
        this.uav_speed_high = uav_speed_high;
    }

    public double getUav_speed_low() {
        return uav_speed_low;
    }

    public void setUav_speed_low(double uav_speed_low) {
        this.uav_speed_low = uav_speed_low;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public GimbalOrientationEnum getGimbalOrientationEnum() {
        return gimbalOrientationEnum;
    }

    public void setGimbalOrientationEnum(GimbalOrientationEnum gimbalOrientationEnum) {
        this.gimbalOrientationEnum = gimbalOrientationEnum;
    }

    public Boolean getbLaserRangingSwitch() {
        return bLaserRangingSwitch;
    }

    public void setbLaserRangingSwitch(Boolean bLaserRangingSwitch) {
        this.bLaserRangingSwitch = bLaserRangingSwitch;
    }
}
