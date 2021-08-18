package com.bjnet.airplaydemo.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;

public class SharedPreferenceHelper {

    public Context context;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String MAX_FRAME = "max_frame";
    private String RESOLUTION = "resolution";
    private String IS_SURFACEVIEW = "is_surfaceview";
    private String MAX_CAST_COUNT = "max_cast_count";
    private String FRAME_MODE = "frame_mode";
    private String PLAY_MODE = "PLAY_MODE";

    public static SharedPreferenceHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final SharedPreferenceHelper INSTANCE = new SharedPreferenceHelper();
    }

    private SharedPreferenceHelper() {
        context = DemoApplication.getContext();
        sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.device), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    void save32UUID(String UUID){
        editor.putString("UUID",UUID);
        editor.commit();
    }

    String get32UUID(){
        return sharedPreferences.getString("UUID",null);
    }

    public void saveSecrectKey(String secrectKey){
        editor.putString("SecrectKey",secrectKey);
        editor.commit();
    }

    public String getSecrectKey(){
        return sharedPreferences.getString("SecrectKey",null);
    }


    public void saveMaxFrame(int maxframe){
        editor.putInt(MAX_FRAME,maxframe);
        editor.commit();
    }

    public int getMaxFrame(){
        return sharedPreferences.getInt(MAX_FRAME,1);
    }

    public void saveResolution(int resolution){
        editor.putInt(RESOLUTION,resolution);
        editor.commit();
    }

    public int getResolution(){
        return sharedPreferences.getInt(RESOLUTION,0);
    }


    public void saveIsSurfaceView(boolean issurfaceview){
        editor.putBoolean(IS_SURFACEVIEW,issurfaceview);
        editor.commit();
    }

    public boolean getIsSurfaceView(){
        return sharedPreferences.getBoolean(IS_SURFACEVIEW,true);
    }

    public void saveMaxCastCount(int value){
        editor.putInt(MAX_CAST_COUNT,value);
        editor.commit();
    }

    public int getMaxCastCount(){
        return sharedPreferences.getInt(MAX_CAST_COUNT,0);
    }

    public void saveFrameMode(int value){
        editor.putInt(FRAME_MODE,value);
        editor.commit();
    }

    public int getFrameMode(){
        return sharedPreferences.getInt(FRAME_MODE,0);
    }

    public void savePlayMode(int value){
        editor.putInt(PLAY_MODE,value);
        editor.commit();
    }

    public int getPlayMode(){
        return sharedPreferences.getInt(PLAY_MODE,1);
    }
}
