package com.bjnet.airplaydemo.view.textureview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;

import com.bjnet.airplaydemo.event.VideoRotateEvent;
import com.bjnet.airplaydemo.imp.AirplayMirrorChannel;
import com.blankj.utilcode.util.LogUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AirplayTextureView extends BaseTextureView {
    public AirplayTextureView(@NonNull Context context) {
        super(context);
    }

    public AirplayTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AirplayTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        getChannel().setSurface(new Surface(surface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//        setVideoViewSize(width, height);
        sizeChange();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        getChannel().setSurface(null);
        return false;
    }

    int rotationVideoWidth = 0;
    int rotationVideoHeight = 0;

    public void sizeChange() {
        AirplayMirrorChannel airplayMirrorChannel = (AirplayMirrorChannel) getChannel();
        int videoRotate = airplayMirrorChannel.getVideoRotate();
        textureView.setRotation(videoRotate);
        if (videoRotate % 180 == 0) {//竖屏
            textureView.setScaleX(1);
            textureView.setScaleY(1);
        } else {
            rotationVideoWidth = video_width;
            rotationVideoHeight = video_height;
            int size[] = getScaleSize(video_height, video_width);
            float scaleX = ((float) size[0]) / ((float) textureParams.height);
            textureView.setScaleX(scaleX);
            textureView.setScaleY(scaleX);
        }
    }

    @Override
    public void setDisplayViewCount(int count) {
        super.setDisplayViewCount(count);
        if (count == 1) {
//            textureView.setScaleX(1);
//            textureView.setScaleY(1);
            sizeChange();
        } else if (count == 2) {
            sizeChange();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRotateChannel(VideoRotateEvent event) {
       LogUtils.i("onVideoRotateEvent channel:" + event);
        if (event.getChannelID() == getChannelId()) {
            sizeChange();
        }
    }
}
