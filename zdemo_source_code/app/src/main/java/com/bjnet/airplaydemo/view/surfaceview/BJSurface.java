package com.bjnet.airplaydemo.view.surfaceview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.SurfaceView;

public class BJSurface extends SurfaceView {
    public BJSurface(Context context) {
        super(context);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
}
