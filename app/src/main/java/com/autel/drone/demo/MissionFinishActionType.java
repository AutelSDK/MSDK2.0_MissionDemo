package com.autel.drone.demo;

/**
 * 任务完成动作，返航，悬停，未知
 */
public enum MissionFinishActionType {
    UNKNOWN(-1),
    /** 返航 */
    GO_HOME(0),
    /** 悬停 */
    HOVER(1),
    /** 降落 */
    LAND(2),
    /** 停在最一个航点位置 */
    LAST(3);

    private int value;

    MissionFinishActionType(int i) {
        this.value = i;
    }

    public int getValue() {
        return value;
    }

    public static MissionFinishActionType find(int value) {
        switch (value) {
            case 0:
                return GO_HOME;
            case 1:
                return HOVER;
            default:
                return UNKNOWN;
        }
    }
}
