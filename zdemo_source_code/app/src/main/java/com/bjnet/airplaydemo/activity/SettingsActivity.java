package com.bjnet.airplaydemo.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.base.DeviceName;
import com.bjnet.airplaydemo.event.CloseChannelEvent;
import com.bjnet.airplaydemo.event.FinishEvent;
import com.bjnet.airplaydemo.event.FrameModeEvent;
import com.bjnet.airplaydemo.event.MaxCastCountEvent;
import com.bjnet.airplaydemo.event.MaxFrameEvent;
import com.bjnet.airplaydemo.event.PlayModeEvent;
import com.bjnet.airplaydemo.event.ResolutionEvent;
import com.bjnet.airplaydemo.util.SharedPreferenceHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    TextView device,max_frame_text,resolution_text,max_cast_count_text,frame_mode_text,play_mode_text;
    DeviceName deviceName;
    Switch pinIf,surfaceview_if;
    boolean isNeedRestart = false;

    private int deviceNumber;
    boolean isChecked,isSurfaceview;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_settings);
        EventBus.getDefault().register(this);

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.device), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        deviceName = new DeviceName(getApplicationContext());
        device = findViewById(R.id.device);
        pinIf = findViewById(R.id.pin_if);
        surfaceview_if = findViewById(R.id.surfaceview_if);

        max_frame_text= findViewById(R.id.max_frame_text);
        resolution_text= findViewById(R.id.resolution_text);
        max_cast_count_text = findViewById(R.id.max_cast_count_text);
        frame_mode_text= findViewById(R.id.frame_mode_text);
        play_mode_text= findViewById(R.id.play_mode_text);
        deviceNumber = deviceName.getNumber();

        pinIf.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isChecked = b;
                CastManager.getMgr().setEnablePin(isChecked);

            }
        });

        surfaceview_if.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isSurfaceview = b;
                SharedPreferenceHelper.getInstance().saveIsSurfaceView(b);

            }
        });
    }

    public void deviceName(View v) {
        Intent intent = new Intent(this, CustomAct.class);
        startActivity(intent);
    }

    public void backPressed(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isNeedRestart){
            DemoApplication.APP.getEventBus().post(new FinishEvent());
        }

    }

    public void wifi(View view) {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility((uiOptions));
        }
    }

    public void aboutUs(View view) {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility((uiOptions));

        isChecked = sharedPreferences.getBoolean(getResources().getString(R.string.sta_pin), false);
        pinIf.setChecked(isChecked);

        isSurfaceview=SharedPreferenceHelper.getInstance().getIsSurfaceView();
        surfaceview_if.setChecked(isSurfaceview);


        if (deviceName.getDeviceName() != null) {
            device.setText(deviceName.getDeviceName());
        } else {
            device.setText("BJAirplayDemo_" + String.valueOf(deviceNumber));
        }
        int maxFrame = SharedPreferenceHelper.getInstance().getMaxFrame();
        List<String> max_frame_list = Arrays.asList(getResources().getStringArray(R.array.max_frame_type));
        max_frame_text.setText(max_frame_list.get(maxFrame)+"");
        int resolution = SharedPreferenceHelper.getInstance().getResolution();
        List<String> resolution_list = Arrays.asList(getResources().getStringArray(R.array.resolution_type));
        resolution_text.setText(resolution_list.get(resolution)+"");

        int count = SharedPreferenceHelper.getInstance().getMaxCastCount();
        List<String> maxcastcount_list = Arrays.asList(getResources().getStringArray(R.array.max_cast_count_type));
        max_cast_count_text.setText(maxcastcount_list.get(count)+"");

        int frameMode = SharedPreferenceHelper.getInstance().getFrameMode();
        List<String> frame_mode_list = Arrays.asList(getResources().getStringArray(R.array.frame_mode_type));
        frame_mode_text.setText(frame_mode_list.get(frameMode)+"");


        int playMode = SharedPreferenceHelper.getInstance().getPlayMode();
        List<String> play_mode_list = Arrays.asList(getResources().getStringArray(R.array.play_mode_type));
        play_mode_text.setText(play_mode_list.get(playMode)+"");

    }

    @Override
    protected void onStop() {
        editor.putBoolean(getResources().getString(R.string.sta_pin), isChecked);
        editor.commit();

        SharedPreferenceHelper.getInstance().saveIsSurfaceView(isSurfaceview);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void player(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }
    public void max_frame(View view) {
        Intent intent = new Intent(this, ChoiceActivity.class);
        intent.putExtra("activity","max_frame");
        startActivity(intent);
    }

    public void resolution(View view) {
        Intent intent = new Intent(this, ChoiceActivity.class);
        intent.putExtra("activity","resolution");
        startActivity(intent);
    }
    public void max_cast_count(View view) {
        Intent intent = new Intent(this, ChoiceActivity.class);
        intent.putExtra("activity","max_cast_count");
        startActivity(intent);
    }
    public void frame_mode(View view) {
        Intent intent = new Intent(this, ChoiceActivity.class);
        intent.putExtra("activity","frame_mode");
        startActivity(intent);
    }
    public void play_mode(View view) {
        Intent intent = new Intent(this, ChoiceActivity.class);
        intent.putExtra("activity","play_mode");
        startActivity(intent);
    }




    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMaxFrameEvent(MaxFrameEvent event) {
        int maxFrame = SharedPreferenceHelper.getInstance().getMaxFrame();
        List<String> max_frame_list = Arrays.asList(getResources().getStringArray(R.array.max_frame_type));
        max_frame_text.setText(max_frame_list.get(maxFrame)+"");
        isNeedRestart = true;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResolutionEvent(ResolutionEvent event) {
        int resolution = SharedPreferenceHelper.getInstance().getResolution();
        List<String> resolution_list = Arrays.asList(getResources().getStringArray(R.array.resolution_type));
        resolution_text.setText(resolution_list.get(resolution)+"");
        isNeedRestart = true;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMaxCastCountEvent(MaxCastCountEvent event) {
        int count = SharedPreferenceHelper.getInstance().getMaxCastCount();
        List<String> maxcastcount_list = Arrays.asList(getResources().getStringArray(R.array.max_cast_count_type));
        max_cast_count_text.setText(maxcastcount_list.get(count)+"");
        CastManager.getMgr().setMaxCastCount(Integer.parseInt(maxcastcount_list.get(count)));
        isNeedRestart = true;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFrameModeEvent(FrameModeEvent event) {
        int count = SharedPreferenceHelper.getInstance().getFrameMode();
        List<String> frame_mode_list = Arrays.asList(getResources().getStringArray(R.array.frame_mode_type));
        frame_mode_text.setText(frame_mode_list.get(count)+"");
        isNeedRestart = true;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayModeEvent(PlayModeEvent event) {
        int count = SharedPreferenceHelper.getInstance().getPlayMode();
        List<String> play_mode_list = Arrays.asList(getResources().getStringArray(R.array.play_mode_type));
        play_mode_text.setText(play_mode_list.get(count)+"");
        isNeedRestart = true;
    }



}
