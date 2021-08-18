package com.bjnet.airplaydemo;

import android.app.Application;
import android.content.Context;

import com.bjnet.airplaydemo.event.CreateChannelFailedEvent;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Created by supermanwg on 2016/10/5.
 */
public class DemoApplication extends Application {
    public static final String TAG = "DemoCastClient";

    public static DemoApplication APP;
    UncaughtExceptionHandler defaultHandler;
    org.greenrobot.eventbus.EventBus eventBus;

    public static final int MOUSEISSHOW = 1;
    public static final int MOUSEBITMAP = 2;
    public static final int MOUSEISMOVE = 3;

    public org.greenrobot.eventbus.EventBus getEventBus(){
        return eventBus;
    }

    private static Context context;
    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        APP = this;
        eventBus = org.greenrobot.eventbus.EventBus.getDefault();
        Utils.init(this);

        defaultHandler = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ex.printStackTrace();
                LogUtils.iTag(TAG, "uncaughtException: "+ex.getLocalizedMessage());
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);

        context = getApplicationContext();
        eventBus.register(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        eventBus.unregister(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LogUtils.w( "onTrimMemory: ");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onCreateChannelFailedEvent(CreateChannelFailedEvent event) {
        String info = event.getReason();
        ToastUtils.showShort(info);
    }
}
