package com.bjnet.airplaydemo.view.surfaceview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.bjnet.airplaydemo.event.VideoRotateEvent;
import com.bjnet.airplaydemo.imp.AirplayMirrorChannel;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AirplaySurfaceVideoView extends BaseSurfaceView {

    public AirplaySurfaceVideoView(Context context) {
        super(context);
    }

    public AirplaySurfaceVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AirplaySurfaceVideoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        getChannel().setSurface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        setVideoViewSize(width, height);
        sizeChange();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        getChannel().setSurface(null);
    }

    public AirplayMirrorChannel getChannel() {
        return (AirplayMirrorChannel) channel;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRotateChannel(VideoRotateEvent event) {
        if (event.getChannelID() == getChannelId()) {
            sizeChange();
        }
    }

    int rotationVideoWidth = 0;
    int rotationVideoHeight = 0;

    public void sizeChange() {
//        int videoRotate = getChannel().getVideoRotate();
//        textureView.setRotation(videoRotate);
//        if (videoRotate % 180 == 0) {//竖屏
//            textureView.setScaleX(1);
//            textureView.setScaleY(1);
//        } else {
//            rotationVideoWidth = video_width;
//            rotationVideoHeight = video_height;
//            int size[] = getScaleSize(video_height, video_width);
//            float scaleX = ((float) size[0]) / ((float) textureParams.height);
//            textureView.setScaleX(scaleX);
//            textureView.setScaleY(scaleX);
//        }
    }

    @Override
    public void setDisplayViewCount(int count) {
        super.setDisplayViewCount(count);
        if (count == 1) {
            textureView.setScaleX(1);
            textureView.setScaleY(1);
        } else if (count == 2) {
            sizeChange();
        }
    }
}
