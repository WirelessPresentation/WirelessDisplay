package com.bjnet.airplaydemo.playerEngine;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.event.AirplayUrlPlayEndEvent;
import com.bjnet.airplaydemo.event.VideoSizeEvent;
import com.bjnet.cbox.module.MediaChannel;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlayerEngine extends PlayerEngineSuper {

    private static final String TAG = "MediaPlayer";

    private MediaPlayer player;

    public MediaPlayerEngine(MediaChannel channel, String url){
        this.channel = channel;
        this.url = url;
        this.player = null;
        this.playerState = PlayerState.PLAYER_STATE_UNINIT;
        this.ctrlPlayerState = PlayerState.PLAYER_STATE_STARTED;
        this.checkPrepareTimer = new Timer();
    }

    public MediaPlayerEngine(MediaChannel channel, String url, Surface holder){
        this.channel = channel;
        this.url = url;
        this.player = null;
        this.playerState = PlayerState.PLAYER_STATE_UNINIT;
        this.ctrlPlayerState = PlayerState.PLAYER_STATE_STARTED;
        this.checkPrepareTimer = new Timer();
        setSurface(holder);
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        synchronized (playerState){
            if (playerState == PlayerState.PLAYER_STATE_UNINIT || playerState == PlayerState.PLAYER_STATE_RELEASED) {

            }else {
                if (this.player != null){
                    this.player.setSurface(surface);
                    synchronized (playerState) {
                        if(playerState == PlayerState.PLAYER_STATE_INIT){
                            this.player.prepareAsync();
                            setPlayerState(PlayerState.PLAYER_STATE_PREPARING);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean open() {
        preparePlayer();
        return true;
    }

    private void preparePlayer(){
        synchronized (playerState) {
            this.player = new MediaPlayer();
            try {
                player.setDataSource(this.url);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();

                player.release();
                setPlayerState(PlayerState.PLAYER_STATE_RELEASED);
                this.player = null;
                return;
            }

            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (surface != null){
                this.player.setSurface(this.surface);
            }
            setPlayerState(PlayerState.PLAYER_STATE_INIT);
        }

        this.player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "MediaPlayer url channel:" +
                        "" + channel.getChannelId() + " onPrepared ctrlPlayerState:" + ctrlPlayerState.toString());
                ViewSize size = getFitSize(DemoApplication.getContext(),player);
                synchronized (playerState) {
                    if(PlayerState.PLAYER_STATE_PREPARING == playerState){
                        setPlayerState(PlayerState.PLAYER_STATE_PREPARED);
                        Log.i(TAG, "MediaPlayer OnPrepared: fix w:"+size.width+" h: "+size.height);
                        //surfaceHodler.setFixedSize(size.width,size.height);
                        VideoSizeEvent event = new VideoSizeEvent(channel.getChannelId(),size.width,size.height);
                        DemoApplication.APP.getEventBus().post(event);
                        player.start();
                        setPlayerState(PlayerState.PLAYER_STATE_STARTED);
                    }
                }
                if(ctrlPlayerState == PlayerState.PLAYER_STATE_PAUSED){
                    pause();
                }

                if (pending_seek_to_msec > 0){
                    player.seekTo(pending_seek_to_msec);
                    pending_seek_to_msec = -1;
                }
            }
        });

        this.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i(TAG, "MediaPlayer url channel:" +
                        "" + channel.getChannelId() + " onError what:" + what);
                setPlayerState(PlayerState.PLAYER_STATE_ERROR);
                if(state != MediaChannel.MCState.MC_DEAD){
                    DemoApplication.APP.getEventBus().post(new AirplayUrlPlayEndEvent(channel.getChannelId(),what));
                }
                return false;
            }
        });

        this.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "MediaPlayer audio url channel:" +
                        "" + channel.getChannelId() + " onCompletion what:" + mp);
//                ScreenRenderApp.APP.getEventBus().post(new AirplayUrlPlayEndEvent(getChannelId(),0));
                setPlayerState(PlayerState.PLAYER_STATE_COMPLETED);
            }
        });
        this.checkPrepareTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (playerState) {
                    if (playerState == PlayerState.PLAYER_STATE_PREPARING){
                        Log.e(TAG, "url channel:" +
                                "" + channel.getChannelId() + " prepare timeout:");
                        doReleasePlayer();
                    }
                }
            }
        },15000);
    }

    @Override
    public void close() {
        synchronized (playerState) {
            doReleasePlayer();
        }
    }

    @Override
    public void pause() {
        synchronized (playerState) {
            if (player != null) {
                if (PlayerState.PLAYER_STATE_PAUSED == this.playerState
                        || PlayerState.PLAYER_STATE_STARTED == this.playerState
                        || PlayerState.PLAYER_STATE_PREPARED == this.playerState) {
                    player.pause();
                    setPlayerState(PlayerState.PLAYER_STATE_PAUSED);
                }
            }
        }
    }

    @Override
    public int getPts() {
        int pts = 0;
        synchronized (playerState) {
            if (this.player != null){
                if (this.playerState == PlayerState.PLAYER_STATE_STARTED
                        || this.playerState == PlayerState.PLAYER_STATE_PAUSED)
                {
                    pts = (int)this.player.getCurrentPosition();
                }
            }
        }
        if(0 == pts){
            pts = 1;
        }
        return pts;
    }

    @Override
    public int getDuation() {
        if (duation > 0)
            return  duation;
        if(player != null && playerState.equals(PlayerState.PLAYER_STATE_STARTED)){
            duation =  (int)player.getDuration();
            Log.v(TAG,"getDuration" + playerState + "  "+ duation);
        }
        if (duation <= 0)
            return 72000000;

        return  duation;
    }

    @Override
    public void play() {
        if (player != null){
            synchronized (playerState) {
                if (PlayerState.PLAYER_STATE_PAUSED == this.playerState
                        || PlayerState.PLAYER_STATE_STARTED == this.playerState
                        || PlayerState.PLAYER_STATE_PREPARED == this.playerState) {
                    player.start();
                    setPlayerState(PlayerState.PLAYER_STATE_STARTED);
                }
            }
        }
        this.ctrlPlayerState = PlayerState.PLAYER_STATE_STARTED;
    }

    @Override
    public void seek(int sec) {
        if (player != null){
            if (PlayerState.PLAYER_STATE_PAUSED == this.playerState
                    || PlayerState.PLAYER_STATE_STARTED == this.playerState
                    || PlayerState.PLAYER_STATE_PREPARED == this.playerState)
            {
                player.seekTo(sec * 1000);
                Log.i(TAG,"AirplayUrlPlayChannel seek:" + sec);
            }
            else
            {
                Log.i(TAG,"AirplayUrlPlayChannel seek:"+sec+" in error state:"+this.playerState.toString());
                if(PlayerState.PLAYER_STATE_PREPARING == this.playerState
                        || PlayerState.PLAYER_STATE_INIT == this.playerState){
                    pending_seek_to_msec = sec * 1000;
                }
            }
        }
    }

    @Override
    public void setVolume(int volume) {
        float v = volume/100.0f;
        if(player != null){
            player.setVolume(v,v);
        }
    }

    @Override
    public void setMute() {
        player.setVolume(0,0);
    }

    @Override
    public void setUnmute() {
        player.setVolume(1,1);
    }

    public static ViewSize getFitSize(Context context, MediaPlayer mediaPlayer)
    {
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        double fit1 = videoWidth * 1.0 / videoHeight;

        int width2 = getScreenWidth(context);
        int height2 = getScreenHeight(context);
        double fit2 = width2 * 1.0 / height2;

        double fit = 1;
        if (fit1 > fit2)
        {
            fit = width2 * 1.0 / videoWidth;
        }else{
            fit = height2 * 1.0 / videoHeight;
        }

        ViewSize viewSize = new ViewSize();
        viewSize.width = (int) (fit * videoWidth);
        viewSize.height = (int) (fit * videoHeight);

        return viewSize;
    }

    private void doReleasePlayer() {
        synchronized (playerState) {
            Log.i(TAG, "doReleasePlayer: channnel id:" + channel.getChannelId() + " state:" + playerState.toString());
            switch (this.playerState) {
                case PLAYER_STATE_UNINIT:
                case PLAYER_STATE_RELEASED:
                    break;
                case PLAYER_STATE_INIT:
                case PLAYER_STATE_PREPARING:
                case PLAYER_STATE_PREPARED: {
                    if (this.player != null) {
                        this.player.release();
                        this.player = null;
                        setPlayerState(PlayerState.PLAYER_STATE_RELEASED);
                    }
                }
                break;
                case PLAYER_STATE_STARTED:
                case PLAYER_STATE_PAUSED:
                    if (this.player != null) {
                        this.player.stop();
                        this.player.release();
                        this.player = null;
                        setPlayerState(PlayerState.PLAYER_STATE_RELEASED);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
