package com.bjnet.airplaydemo.view.textureview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

public class AirUrlTextureView extends BaseTextureView implements TextureView.SurfaceTextureListener {

    public AirUrlTextureView(@NonNull Context context) {
        super(context);
    }

    public AirUrlTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AirUrlTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        getChannel().setSurface(new Surface(surface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int c) {
//        setVideoViewSize(width, c);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        getChannel().setSurface(null);
        return false;
    }

}
