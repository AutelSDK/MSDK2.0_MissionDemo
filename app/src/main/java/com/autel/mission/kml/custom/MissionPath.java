package com.autel.mission.kml.custom;

import java.util.List;

/**
 * 任务路径详情
 * 例：
 * {
 *    "parameters" : {
 *       "uav_height_ground" : 100.0,
 *       "uav_init_pos" : [ 104.377439, 30.xxxxxxx, 0.0 ],
 *       "uav_speed_height" : 7.0,
 *       "uav_speed_low" : 3.5
 *    },
 *    "uav_route" : [
 *       [ 104.48033142089844, 30.xxxxxxx, 0.0, 7.0, 7.0 , 7.0  ],
 *       [ 104.45047760009766, 30.xxxxxxx, 0.0, 7.0, 7.0 , 7.0  ],
 *       [ 104.45001220703125, 30.xxxxxxx, 0.0, 7.0, 7.0 , 7.0  ]
 *    ]
 * }
 */
public class MissionPath {
    private UavParameters parameters;   // 无人机飞行参数
    private List<List<Double>> uav_route;   // 无人机路径：经、纬、高、当前点的速度

    public UavParameters getParameters() {
        return parameters;
    }

    public void setParameters(UavParameters parameters) {
        this.parameters = parameters;
    }

    public List<List<Double>> getUav_route() {
        return uav_route;
    }

    public void setUav_route(List<List<Double>> uav_route) {
        this.uav_route = uav_route;
    }
}
