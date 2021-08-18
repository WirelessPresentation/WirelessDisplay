package com.bjnet.airplaydemo.imp;

import android.util.Log;

import com.bjnet.cbox.module.ComBuffer;
import com.bjnet.cbox.module.ComBufferPool;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaChannelInfo;

/**
 * @package: com.bjnet.screenrenderdemo
 * @data: 2018/6/14
 * @author: DELL
 */
public class AirplayPicViewChannel extends MediaChannel {
    private static final String TAG = "PicViewChannel";
    private ComBufferPool bufferPool = new ComBufferPool();

    public AirplayPicViewChannel(MediaChannelInfo info) {
        super(info);
    }

    @Override
    public void showPicture(String pic) {
        Log.i(TAG,"showPicture: " + pic);
    }

    @Override
    public void showPicture(ComBuffer data){
        Log.i(TAG,"showPicture from buffer");
    }

    @Override
    public boolean open() {
        setState(MCState.MC_OPENED);
        return true;
    }

    @Override
    public void close() {
        setState(MCState.MC_DEAD);
    }

    @Override
    public void onFrame(int frameLen, int w, int h) {

    }

    @Override
    public void onAudioFrame(ComBuffer data) {

    }

    @Override
    public ComBuffer reqBuffer(int size) {
        return this.bufferPool.reqBuffer(size);
    }

}
