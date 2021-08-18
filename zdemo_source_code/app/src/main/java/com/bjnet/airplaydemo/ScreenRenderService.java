package com.bjnet.airplaydemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.bjnet.airplaydemo.base.DeviceName;
import com.bjnet.airplaydemo.event.RefreshPinEvent;


public class ScreenRenderService extends Service {
    // TODO: Rename actions, choose action names that describe tasks that this

    @Override
    public void onCreate() {
        super.onCreate();

        DeviceName deviceClass = new DeviceName(getApplicationContext());
        int deviceNumber = deviceClass.getNumber();
        final String deviceName;
        if (deviceClass.getDeviceName() != null) {
            deviceName = deviceClass.getDeviceName();
        } else {
            deviceName = "BJAirplayDemo_" + String.valueOf(deviceNumber);
        }
        //WifiStateManager.getInstance().init();
        new Thread(new Runnable() {
            @Override
            public void run() {
                CastManager.getMgr().prepareBJLicenseMoudle(deviceName);
            }
        }).start();

        SharedPreferences sharedPreferences = getSharedPreferences
                (getResources().getString(R.string.device), Context.MODE_PRIVATE);
        CastManager.getMgr().setEnablePin(sharedPreferences.getBoolean(getResources().getString(R.string.sta_pin), false));

        CastManager.getMgr().setPlayer(sharedPreferences.getInt("player", 1));
        DemoApplication.APP.getEventBus().post(new RefreshPinEvent());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(DemoApplication.TAG, "ScreenRenderService onStartCommand");
        DemoApplication.APP.getEventBus().post(new RefreshPinEvent());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(DemoApplication.TAG, "ScreenRenderService onDestroy");
        CastManager.getMgr().unInitAirplayModule();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
