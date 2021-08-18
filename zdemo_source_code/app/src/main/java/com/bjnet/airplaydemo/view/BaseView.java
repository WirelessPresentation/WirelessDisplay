package com.bjnet.airplaydemo.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.bjnet.cbox.module.MediaChannel;

public abstract class BaseView extends FrameLayout {
    public BaseView(@NonNull Context context) {
        super(context);
    }

    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract MediaChannel getChannel();

    public abstract int getChannelId();

    public abstract void onCreate(MediaChannel mediaChannel);

    public abstract void setDisplayViewCount(int count);

    public abstract void onDestroy();
}
