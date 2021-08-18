package com.bjnet.airplaydemo.playerEngine;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaConst;

import java.util.Timer;

enum PlayerState{
    PLAYER_STATE_UNINIT("UNINIT"),
    PLAYER_STATE_INIT("INIT"),
    PLAYER_STATE_PREPARING("PREPARING"),
    PLAYER_STATE_PREPARED("PREPARED"),
    PLAYER_STATE_STARTED("STARTED"),
    PLAYER_STATE_PAUSED("PAUSED"),
    PLAYER_STATE_RELEASED("RELEASED"),
    PLAYER_STATE_COMPLETED("COMPLETED"),
    PLAYER_STATE_ERROR("ERROR");

    PlayerState(String desp){
        descp = desp;
    }
    private String descp;

    @Override
    public String toString() {
        return "{" +
                descp +
                '}';
    }
}

public abstract class PlayerEngineSuper {

    private static final String TAG = "PlayerEngineSuper";

    public PlayerState playerState;
    public PlayerState ctrlPlayerState;
    public Timer checkPrepareTimer;
    protected MediaChannel.MCState state;

    public Handler handler;
    public MediaChannel channel;

    public static final int DlnaEvent_SetState = 0;
    public static final int DlnaEvent_RefrenshPos = 1;
    public static final int DlnaEvent_SetDuation = 2;

    public String url;

    public int duation = -1;
    public int pending_seek_to_msec = -1;
    public HandlerThread handlerThread;
    public Surface surface;

    public abstract void setSurface(Surface surface);

    public abstract boolean open();

    public abstract void close();

    public abstract void pause();

    public abstract int getPts();

    public abstract int getDuation();

    public abstract void play();

    public abstract void seek(int msec);

    public abstract void setVolume(int volume);

    public abstract void setMute();

    public abstract void setUnmute();

    public void setPlayerState(PlayerState playerState) {
        if (this.playerState != playerState){
            Log.i(TAG, " playState from "+this.playerState.toString()+" to "+playerState.toString());
            this.playerState = playerState;
        }
    }

    public int getPlayerStatus() {
        int status = MediaConst.PLAYER_STATUS_LOADING;
        switch (playerState){
            case PLAYER_STATE_INIT:
            case PLAYER_STATE_UNINIT:
            case PLAYER_STATE_PREPARING:
                status =  MediaConst.PLAYER_STATUS_LOADING;
                break;
            case PLAYER_STATE_PREPARED:
            case PLAYER_STATE_STARTED:
                status =  MediaConst.PLAYER_STATUS_PLAYING;
                break;
            case PLAYER_STATE_PAUSED:
                status =  MediaConst.PLAYER_STATUS_PAUSED;
                break;
            case PLAYER_STATE_RELEASED:
            case PLAYER_STATE_COMPLETED:
                status =  MediaConst.PLAYER_STATUS_ENDED;
                break;
            case PLAYER_STATE_ERROR:
                status =  MediaConst.PLAYER_STATUS_FAILED;
                break;
            default:
                status =  MediaConst.PLAYER_STATUS_LOADING;
                break;
        }

        Log.i(TAG, "getPlayerStatus: "+status);
        return status;
    }

    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }

    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }

    public static class ViewSize
    {
        public int width = 0;
        public int height = 0;
    }

}
