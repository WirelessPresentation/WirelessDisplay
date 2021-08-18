package com.bjnet.airplaydemo.base;

/**
 * Created by Administrator on 2016/10/10.
 */
public class MediaConfHelper {
    private boolean isEnableChannelStat;
    private boolean isEnableChannelDropAudio;
    private static MediaConfHelper instance = null;

    public MediaConfHelper() {
        isEnableChannelStat = true;
        isEnableChannelDropAudio = true;
    }

    public void setIsEnableChannelStat(boolean isEnableChannelStat) {
        this.isEnableChannelStat = isEnableChannelStat;
    }

    public void setIsEnableChannelDropAudio(boolean isEnableChannelDropAudio) {
        this.isEnableChannelDropAudio = isEnableChannelDropAudio;
    }

    public boolean isEnableChannelStat() {
        return isEnableChannelStat;
    }

    public boolean isEnableChannelDropAudio() {
        return isEnableChannelDropAudio;
    }

    public static MediaConfHelper getInstance() {
        if (instance == null){
            synchronized (MediaConfHelper.class){
                if (instance == null){
                    instance = new MediaConfHelper();
                }
            }
        }
        return instance;
    }
}
