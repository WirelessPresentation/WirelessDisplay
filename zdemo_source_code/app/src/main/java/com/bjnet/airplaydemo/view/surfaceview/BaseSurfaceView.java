package com.bjnet.airplaydemo.view.surfaceview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
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

public abstract class BaseSurfaceView extends BaseView implements SurfaceHolder.Callback{

    public BaseSurfaceView(@NonNull Context context) {
        super(context);
        init();
    }

    public BaseSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(getResources().getColor(R.color.dev_back));
        setClipChildren(true);
    }

    @Override
    public abstract void surfaceCreated(SurfaceHolder surfaceHolder);

    @Override
    public abstract void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2);
    @Override
    public abstract void surfaceDestroyed(SurfaceHolder surfaceHolder);

    @Override
    public MediaChannel getChannel() {
        return channel;
    }

    @Override
    public int getChannelId() {
        return channel.getChannelId();
    }

    BJSurface textureView;
    FrameLayout.LayoutParams textureParams;
    int maxWidth;
    int maxHeight;
    double maxWidthHeightBi;
    MediaChannel channel;

    @Override
    public void onCreate(MediaChannel mediaChannel) {
        Log.i("BaseSurfaceView", "onCreate: ");
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


        textureView = new BJSurface(getContext());
        textureParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        textureParams.gravity = Gravity.CENTER;
        textureView.setLayoutParams(textureParams);
        textureView.getHolder().addCallback(this);
        addView(textureView);
    }

    int video_width = 0;
    int video_height = 0;

    public void setVideoViewSize(int width, int height) {
        this.video_width = width;
        this.video_height = height;
        int[] size = getScaleSize(width, height);
        textureParams.width = size[0];
        textureParams.height = size[1];
        textureView.setLayoutParams(textureParams);
    }

    protected int[] getScaleSize(int width, int height) {
        int displayWidth = 0;
        int displayHeight = 0;
        double widthHeightBi = ((float) width) / ((float) height);
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
//        int[] size = {maxWidth, maxHeight};
        return size;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        maxWidth = params.width;
        maxHeight = params.height;
        initVideoUI();
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
//    @Override
//    public void onChannelSizeChange(VideoSizeEvent event) {
//        if (channel.getChannelId() == event.getChannelID() && textureView != null) {
//            setVideoViewSize(event.getWidth(), event.getHeight());
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChannelSizeChange(VideoSizeEvent event) {
        Log.e(DemoApplication.TAG, "onChannelSizeChange: width : " + event.getWidth() + " Height : " + event.getHeight());
        if (channel.getChannelId() == event.getChannelID() && textureView != null) {
            setVideoViewSize(event.getWidth(), event.getHeight());
        }
    }


    protected int count = 1;

    @Override
    public void setDisplayViewCount(int count) {
        this.count = count;
    }

    @Override
    public void onDestroy() {
        CastManager.getMgr().updateChannelFlag(getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_DESTROYED_MASK);
        DemoApplication.APP.getEventBus().unregister(this);
        destroyDrawingCache();
    }
}
