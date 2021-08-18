package com.bjnet.airplaydemo.util;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.event.AirAudioEvent;
import com.bjnet.airplaydemo.event.AirSurfaceEvent;
import com.bjnet.airplaydemo.event.AirUrlEvent;
import com.bjnet.airplaydemo.event.AirplayEvent;
import com.bjnet.airplaydemo.event.CloseChannelEvent;
import com.bjnet.airplaydemo.view.BaseView;
import com.bjnet.airplaydemo.view.surfaceview.AirAudioView;
import com.bjnet.airplaydemo.view.surfaceview.AirplaySurfaceVideoView;
import com.bjnet.airplaydemo.view.textureview.AirUrlTextureView;
import com.bjnet.airplaydemo.view.textureview.AirplayTextureView;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaConst;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ViewHelper {

    private Activity activity;
    private LinearLayout linearLayout;
    private Timer timer;
    private static final int AIRPLAY_TEXTUREVIEW = 2;
    private static final int AIRPLAY_SURFACEVIEW = 3;
    private static final int AIRPLAY_AUDIO = 4;
    private static final int AIRPLAY_URL = 5;


    public ViewHelper(Activity activity, LinearLayout linearLayout) {
        this.activity = activity;
        this.linearLayout = linearLayout;
        DemoApplication.APP.getEventBus().register(this);
    }

    /**
     * 销毁
     */
    public void onDestroy() {
        DemoApplication.APP.getEventBus().unregister(this);
        for (BaseView videoView : views) {
            if (videoView != null) {
                videoView.onDestroy();
            }
        }
        views.clear();
    }

    private int screenWidth, screenHeight;
    private List<BaseView> views = new ArrayList<>();

    /**
     * 初始宽高
     */
    private void initWidthHeight() {
        if (screenHeight == 0 || screenWidth == 0) {
            screenWidth = linearLayout.getMeasuredWidth();
            screenHeight = linearLayout.getMeasuredHeight();
        }
        Log.i(DemoApplication.TAG, "width" + screenWidth + "height" + screenHeight);
    }

    /**
     * 初始化视频空间的宽高
     */
    private void initDisplayUI() {

        int count = views.size();
        if (count == 0) {
            startTimer();
        } else {
            linearLayout.setVisibility(View.VISIBLE);
            stopTimer();
            int videoViewWidth = screenWidth / count;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(videoViewWidth, screenHeight);

            for (BaseView view : views) {
                view.setLayoutParams(layoutParams);
                view.setDisplayViewCount(count);
            }
        }
    }

    private void startTimer(){
        if(timer == null){
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linearLayout.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            },500);
        }
    }

    private void stopTimer(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloseEvent(CloseChannelEvent messageEvent) {
        close(messageEvent.getChannelID());
    }

    /**
     * 关闭视频
     *
     * @param channelId
     */
    public void close(int channelId) {
        Log.e(DemoApplication.TAG, "closeChannel: " + channelId);
        for (BaseView videoView : views) {
            if (channelId == videoView.getChannelId()) {
//                CastManager.getMgr().updateChannelFlag(videoView.getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_DESTROYED_MASK);
                linearLayout.removeView(videoView);
                videoView.onDestroy();
                views.remove(videoView);
                initDisplayUI();
                break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAirplay(AirplayEvent event) {
        addChannel(event.mediaChannel, AIRPLAY_TEXTUREVIEW);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAirAudio(AirAudioEvent event) {
        addChannel(event.mediaChannel, AIRPLAY_AUDIO);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAirSurface(AirSurfaceEvent event) {
        addChannel(event.mediaChannel, AIRPLAY_SURFACEVIEW);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAirUrl(AirUrlEvent event) {
        addChannel(event.mediaChannel, AIRPLAY_URL);
    }

    public boolean hasViews() {
        return (views.size() > 0) ? true : false;
    }

    public void clear() {
        for (BaseView videoView : views) {
//                CastManager.getMgr().updateChannelFlag(videoView.getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_DESTROYED_MASK);
            linearLayout.removeView(videoView);
            videoView.onDestroy();
            views.remove(videoView);
        }
    }

    private void addChannel(MediaChannel mediaChannel, int i) {
        Log.e(DemoApplication.TAG, "addChannel: " + mediaChannel.getChannelId() + "type" + i);
        initWidthHeight();

        BaseView videoView;
        videoView = getVideoView(mediaChannel, i);

        if (videoView == null) {
            Log.e(DemoApplication.TAG, "videoView == null");
            return;
        }
        //播放器添加播放资源
        videoView.onCreate(mediaChannel);
        views.add(videoView);
        linearLayout.addView(videoView);
        initDisplayUI();
    }

    public BaseView getVideoView(MediaChannel mediaChannel, int i) {
        BaseView videoView = null;
        switch (i) {
            case AIRPLAY_TEXTUREVIEW:
                videoView = new AirplayTextureView(activity);
                break;
            case AIRPLAY_SURFACEVIEW:
                videoView = new AirplaySurfaceVideoView(activity);
                break;
            case AIRPLAY_AUDIO:
                videoView = new AirAudioView(activity);
                break;
            case AIRPLAY_URL:
                videoView = new AirUrlTextureView(activity);
                break;
        }
        return videoView;
    }
}
