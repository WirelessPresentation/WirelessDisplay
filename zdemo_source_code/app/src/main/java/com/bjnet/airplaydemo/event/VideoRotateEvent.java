package com.bjnet.airplaydemo.event;

/**
 * Created by supermanwg on 2018/7/23.
 */

public class VideoRotateEvent {
    private int channelID;
    private int angle;

    public VideoRotateEvent(int channelID, int rotate) {
        this.channelID = channelID;
        this.angle = rotate;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    @Override
    public String toString() {
        return "VideoRotateEvent{" +
                "channelID=" + channelID +
                ", angle=" + angle +
                '}';
    }
}
