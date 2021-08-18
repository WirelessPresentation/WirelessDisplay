package com.bjnet.airplaydemo.base;

import com.bjnet.cbox.module.MediaChannel;

public class MediaChannelCtx {
    //mask:
    public static final int CHANNEL_OPENED_MASK = 0x01;
    public static final int CHANNEL_CLOSED_MASK = 0x02;
    public static final int CHANNEL_ACTIVITY_CREATED_MASK = 0x04;
    public static final int CHANNEL_ACTIVITY_DESTROYED_MASK = 0x08;

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public MediaChannel getChannel() {
        return channel;
    }

    public void setChannel(MediaChannel channel) {
        this.channel = channel;
    }

    public int getChannelMask(){
        return  channelMask;
    }

    public void setChannelMask(int channelMask) {
        this.channelMask = channelMask;
    }

    public MediaChannelCtx(MediaChannel channel) {
        this.channelID = channel.getChannelId();
        this.channel = channel;
        this.setChannelMask(CHANNEL_OPENED_MASK);
    }

    private int channelID;
    private MediaChannel channel;
    private int channelMask;
}
