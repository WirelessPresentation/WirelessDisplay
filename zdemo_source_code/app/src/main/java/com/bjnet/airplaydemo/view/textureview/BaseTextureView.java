package com.bjnet.airplaydemo.view.textureview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.base.MediaChannelCtx;
import com.bjnet.airplaydemo.event.VideoSizeEvent;
import com.bjnet.airplaydemo.view.BaseView;
import com.bjnet.cbox.module.MediaChannel;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseTextureView extends BaseView implements TextureView.SurfaceTextureListener {
    public BaseTextureView(@NonNull Context context) {
        super(context);
        init();
    }

    public BaseTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(getResources().getColor(R.color.dev_back));
        setClipChildren(true);
    }

    @Override
    public abstract void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);

    @Override
    public abstract void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int c);

    @Override
    public abstract boolean onSurfaceTextureDestroyed(SurfaceTexture surface);

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public int getChannelId() {
        return channel.getChannelId();
    }

    @Override
    public MediaChannel getChannel() {
        return channel;
    }

    MediaChannel channel;
    FrameLayout.LayoutParams textureParams;
    TextureView textureView;

    @Override
    public void onCreate(MediaChannel mediaChannel) {
        Log.i("BaseTextureView", "onCreate: ");
        channel = mediaChannel;

        CastManager.getMgr().updateChannelFlag(getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_CREATED_MASK);
        DemoApplication.APP.getEventBus().register(this);

        MediaChannelCtx ctx = CastManager.getMgr().getChannelCtxById(getChannelId());
        if (ctx == null){
            onDestroy();
            return;
        }else{
            if ((ctx.getChannelMask() & MediaChannelCtx.CHANNEL_CLOSED_MASK) == MediaChannelCtx.CHANNEL_CLOSED_MASK){
                Log.i(DemoApplication.TAG, "onCreate: channel has closed now channel:"+ctx.toString());
                onDestroy();
                return;
            }
        }
        textureView = new TextureView(getContext());
        textureParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        textureParams.gravity = Gravity.CENTER;
        textureView.setLayoutParams(textureParams);
        textureView.setSurfaceTextureListener(this);
        addView(textureView);
    }

    public void onDestroy() {
        DemoApplication.APP.getEventBus().unregister(this);
        CastManager.getMgr().updateChannelFlag(getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_DESTROYED_MASK);
        destroyDrawingCache();
    }

//    @Override
//    public void onChannelSizeChange(VideoSizeEvent event) {
//        if (channel.getChannelId() == event.getChannelID() && textureView != null) {
//            setVideoViewSize(event.getWidth(), event.getHeight());
//        }
//    }


    int video_width;
    int video_height;

    protected void setVideoViewSize(int width, int height) {
        video_width = width;
        video_height = height;
        int[] size = getScaleSize(width, height);
        textureParams.width = size[0];
        textureParams.height = size[1];
        textureView.setLayoutParams(textureParams);
    }

    int maxWidth;
    int maxHeight;
    double maxWidthHeightBi;

    protected int[] getScaleSize(int width, int height) {
        int displayWidth = 0;
        int displayHeight = 0;
        maxWidthHeightBi = ((float)maxWidth) / ((float)maxHeight);
        double widthHeightBi = ((float)width) / ((float)height);
        if (widthHeightBi > maxWidthHeightBi) {
            //以宽为设置为max等比例缩放高才不会超出
            displayWidth = maxWidth;
            displayHeight = (int) (displayWidth / widthHeightBi);


            if (displayHeight > maxHeight) {
                displayHeight = maxHeight;
                displayWidth = (int) (displayHeight * widthHeightBi);
            }
        } else {
            //以高为设置为max等比例缩放宽才不会超出
            displayHeight = maxHeight;
            displayWidth = (int) (displayHeight * widthHeightBi);
            if (displayWidth > maxWidth) {
                displayWidth = maxWidth;
                displayHeight = (int) (displayWidth / widthHeightBi);
            }
        }
        int[] size = {displayWidth, displayHeight};
        return size;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        maxWidth = params.width;
        maxHeight = params.height;
        initVideoUI();
        Log.i(DemoApplication.TAG, "setLayoutParams:"+"maxWidth"+maxWidth+"maxHeight"+maxHeight);
    }

    protected void initVideoUI() {
        if (video_width == 0) {
            textureParams.width = maxWidth;
            textureParams.height = maxHeight;
            textureView.setLayoutParams(textureParams);
        } else {
            setVideoViewSize(video_width, video_height);
        }
    }

    protected int count = 1;

    public void setDisplayViewCount(int count) {
        this.count = count;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChannelSizeChange(VideoSizeEvent event) {
        Log.i(DemoApplication.TAG, "onChannelSizeChange: "+"width"+event.getWidth()+"height"+event.getHeight());
        if (channel.getChannelId() == event.getChannelID() && textureView != null) {
            setVideoViewSize(event.getWidth(), event.getHeight());
        }
    }
}
