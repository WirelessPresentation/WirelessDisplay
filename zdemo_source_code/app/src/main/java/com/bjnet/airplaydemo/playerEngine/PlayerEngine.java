package com.bjnet.airplaydemo.playerEngine;


import android.util.Log;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.cbox.module.MediaChannel;

public class PlayerEngine {
    private static final String TAG = "PlayerEngine";

    PlayerEngineSuper ps = null;

    public PlayerEngine(MediaChannel channel, String url){

        Log.d(TAG, "PlayerEngine: Url " + url);// http://127.0.0.1:YOUTUBE

        if(url.startsWith("http://127.0.0.1")){
            ps = new IjkPlayerEngine(channel,url);
        }else {
            if(CastManager.getMgr().getPlayer() == 1){
                ps = new IjkPlayerEngine(channel,url);
            }else {
                ps = new MediaPlayerEngine(channel,url);
            }
        }
    }

    public PlayerEngineSuper getPlayerEngine() {
        return ps;
    }
}
