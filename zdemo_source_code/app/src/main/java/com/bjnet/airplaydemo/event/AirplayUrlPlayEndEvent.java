package com.bjnet.airplaydemo.event;

public class AirplayUrlPlayEndEvent {
    public AirplayUrlPlayEndEvent(int channelID, int error) {
        this.channelID = channelID;
        this.error = error;
    }

    private int channelID;
    private int error; //0:completion other:error

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "AirplayUrlPlayEndEvent{" +
                "channelID=" + channelID +
                ", error=" + error +
                '}';
    }
}
