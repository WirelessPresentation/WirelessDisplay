package com.bjnet.airplaydemo.base;

import android.content.Context;
import android.content.SharedPreferences;

import com.bjnet.airplaydemo.R;

import java.util.Random;

public class DeviceName {
    Context context;

    public DeviceName(Context c) {
        context = c;
    }

    public void setDeviceName(String deviceName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences
                (context.getResources().getString(R.string.device), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getResources().getString(R.string.custom_name), deviceName);
        editor.commit();
    }

    public String getDeviceName() {
        SharedPreferences sharedPreferences = context.getSharedPreferences
                (context.getResources().getString(R.string.device), Context.MODE_PRIVATE);

        return sharedPreferences.getString(context.getResources().getString(R.string.custom_name), null);
    }

    public int getNumber() {
        int deviceNumber;
        SharedPreferences sharedPreferences = context.getSharedPreferences
                (context.getResources().getString(R.string.device), Context.MODE_PRIVATE);
        if (sharedPreferences.getInt(context.getResources().getString(R.string.device_name), 999999) == 999999) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Random random = new Random();
            deviceNumber = random.nextInt(999999);
            editor.putInt(context.getResources().getString(R.string.device_name), deviceNumber);
            editor.commit();
        } else {
            deviceNumber = sharedPreferences.getInt(context.getResources().getString(R.string.device_name), 999999);
        }
        return deviceNumber;
    }
}
