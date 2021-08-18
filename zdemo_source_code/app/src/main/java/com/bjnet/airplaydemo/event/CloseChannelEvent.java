package com.bjnet.airplaydemo.event;

public class CloseChannelEvent {
    private int channelID;
    public CloseChannelEvent(int id){
        setChannelID(id);
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }
}
