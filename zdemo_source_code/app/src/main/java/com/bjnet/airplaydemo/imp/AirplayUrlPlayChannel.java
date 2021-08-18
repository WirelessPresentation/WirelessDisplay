package com.bjnet.airplaydemo.imp;

import android.util.Log;
import android.view.Surface;

import com.bjnet.airplaydemo.playerEngine.PlayerEngine;
import com.bjnet.airplaydemo.playerEngine.PlayerEngineSuper;
import com.bjnet.cbox.module.ComBuffer;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaChannelInfo;

/**
 * @package: com.bjnet.screenrenderdemo
 * @data: 2018/6/14
 * @author: DELL
 */
public class AirplayUrlPlayChannel extends MediaChannel {

    private static final String TAG = "UrlPlayChannel";

    private String url;

    private PlayerEngineSuper playerEngine;

    public AirplayUrlPlayChannel(MediaChannelInfo info) {
        super(info);
        this.url = info.getUrlPath();
        playerEngine = new PlayerEngine(this,this.url).getPlayerEngine();
    }

    @Override
    public int getDuation() {
        return  playerEngine.getDuation();
    }

    @Override
    public void pause() {
        Log.i(TAG, "pause: AirplayUrlPlayChannel pause ,channel id:" + getChannelId());
        playerEngine.pause();
    }

    @Override
    public void play() {
        Log.i(TAG, "AirplayUrlPlayChannel play ,channel id:" + getChannelId());
        playerEngine.play();
    }

    @Override
    public void setMute() {
        super.setMute();
        playerEngine.setMute();
    }

    @Override
    public void setUnmute() {
        super.setUnmute();
        playerEngine.setVolume(80);
    }

    @Override
    public void seek(int sec) {
        playerEngine.seek(sec);
    }

    @Override
    public int getPts() {

        return playerEngine.getPts();
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        playerEngine.setSurface(surface);
    }

    @Override
    public boolean open() {
        //setState(MCState.MC_OPENED);
        Log.d(TAG,"AirplayUrlPlayChannel open id:"+getChannelId()+" state:"+state);
        setState(MCState.MC_OPENED);

        playerEngine.open();
        return true;
    }

    @Override
    public int getPlayerStatus() {
        return playerEngine.getPlayerStatus();
    }

    @Override
    public void close() {

        Log.d(TAG, "AirplayUrlPlayChannel close id:" + getChannelId() + " state:" + state);
        if (state == MCState.MC_DEAD){
            return;
        }

        setState(MCState.MC_DEAD);
        playerEngine.close();

        Log.d(TAG, "AirplayUrlPlayChannel close done id:" + getChannelId() + " state:" + state);
    }

    @Override
    public void onFrame(int frameLen, int w, int h) {
        assert false;
    }

    @Override
    public void onAudioFrame(ComBuffer data) {
        assert false;
    }
    @Override
    public ComBuffer reqBuffer(int size) {
        return null;
    }
}
