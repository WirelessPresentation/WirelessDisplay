package com.bjnet.airplaydemo.event;

public class VideoSizeEvent {
    private int channelID;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private int width;
    private int height;

    public VideoSizeEvent(int channelID, int width, int height) {
        this.channelID = channelID;
        this.width = width;
        this.height = height;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    @Override
    public String toString() {
        return "VideoSizeEvent{" +
                "channelID=" + channelID +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
