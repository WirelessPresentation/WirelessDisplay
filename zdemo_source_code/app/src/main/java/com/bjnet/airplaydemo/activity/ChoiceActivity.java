package com.bjnet.airplaydemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.event.FrameModeEvent;
import com.bjnet.airplaydemo.event.MaxCastCountEvent;
import com.bjnet.airplaydemo.event.MaxFrameEvent;
import com.bjnet.airplaydemo.event.PlayModeEvent;
import com.bjnet.airplaydemo.event.ResolutionEvent;
import com.bjnet.airplaydemo.util.SharedPreferenceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChoiceActivity extends Activity implements RadioGroup.OnCheckedChangeListener{

    private static final String TAG = "ChoiceActivity";
    public static final String MAX_FRAME = "max_frame";
    public static final String RESOLUTION = "resolution";
    public static final String MAX_CAST_COUNT = "max_cast_count";
    public static final String FRAME_MODE = "frame_mode";
    public static final String PLAY_MODE = "play_mode";


    private TextView choice_title;
    private RadioGroup choice_group;
    private String title;
    private List<String> max_frame_list,resolution_list,max_cast_count_list,frame_mode_list,play_mode_list;
    private Resources res;
    private List<String> langua_list;
    private int i = 0;
    /***** v2 *****/
    private List<String> mode_list;
    public static final String Tag_Cast_Mode = "CastMode";

    private boolean isFirst = true;
    public int screenWidth1,screenHeight1;
    private float widthsize;
    private float heightsize;
    private ConstraintLayout cl_menu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getIntent().getExtras().getString("activity");
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        screenWidth1=dm.widthPixels;
        screenHeight1=dm.heightPixels;

        widthsize = (float) screenWidth1 / 1080;
        heightsize = (float) screenHeight1 / 720;
        switch (title){
            case MAX_FRAME:
                setContentView(R.layout.activity_choice);
                choice_title = findViewById(R.id.choice_title);
                choice_title.setText(R.string.max_frame);
                break;
            case RESOLUTION:
                setContentView(R.layout.activity_choice);
                choice_title = findViewById(R.id.choice_title);
                choice_title.setText(R.string.resolution);
                break;
            case MAX_CAST_COUNT:
                setContentView(R.layout.activity_choice);
                choice_title = findViewById(R.id.choice_title);
                choice_title.setText(R.string.max_cast_count);
                break;
            case FRAME_MODE:
                setContentView(R.layout.activity_choice);
                choice_title = findViewById(R.id.choice_title);
                choice_title.setText(R.string.frame_mode);
                break;
            case PLAY_MODE:
                setContentView(R.layout.activity_choice);
                choice_title = findViewById(R.id.choice_title);
                choice_title.setText(R.string.play_mode);
                break;

        }
        cl_menu = findViewById(R.id.cl_menu);
        ViewGroup.LayoutParams cl_menulp = (ViewGroup.LayoutParams) cl_menu.getLayoutParams();
        cl_menulp.width = (int)(500 * widthsize);
        cl_menulp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        cl_menu.setLayoutParams(cl_menulp);

        choice_group = findViewById(R.id.choice_group);
        res = getResources();
    }


    @Override
    protected void onResume() {
        super.onResume();
            if(isFirst){
                isFirst = false;
                initRadioButton(title);
            }
        choice_group.setOnCheckedChangeListener(this);
    }

    private void initRadioButton(String title){
        switch (title) {
            case MAX_FRAME:
                max_frame_list = Arrays.asList(res.getStringArray(R.array.max_frame_type));
                for(int i=0; i < max_frame_list.size(); i++)
                {
                    RadioButton tempButton = new RadioButton(this);
                    tempButton.setBackgroundResource(R.drawable.choice_item);
                    tempButton.setButtonDrawable(android.R.color.transparent);
                    tempButton.setGravity(Gravity.CENTER);
                    tempButton.setPadding((int)(115* widthsize), (int)(10 * heightsize), (int)(115* widthsize), (int)(10 * heightsize));
                    tempButton.setText(max_frame_list.get(i));
                    tempButton.setTextColor(getResources().getColor(R.color.dev_back));
                    tempButton.setTextSize(16);
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams((int)(600* widthsize),(int)(60 * heightsize));
                    layoutParams.setMargins((int)(12* widthsize),(int)(12 * heightsize),(int)(12* widthsize),(int)(12 * heightsize));
                    choice_group.addView(tempButton,layoutParams);
                }
                break;
            case RESOLUTION:
                resolution_list = Arrays.asList(res.getStringArray(R.array.resolution_type));
                for(int i=0; i < resolution_list.size(); i++)
                {
                    RadioButton tempButton = new RadioButton(this);
                    tempButton.setBackgroundResource(R.drawable.choice_item);
                    tempButton.setButtonDrawable(android.R.color.transparent);
                    tempButton.setGravity(Gravity.CENTER);
                    tempButton.setPadding((int)(115* widthsize), (int)(10 * heightsize), (int)(115* widthsize), (int)(10 * heightsize));
                    tempButton.setText(resolution_list.get(i));
                    tempButton.setTextColor(getResources().getColor(R.color.dev_back));
                    tempButton.setTextSize(16);
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams((int)(600* widthsize),(int)(60 * heightsize));
                    layoutParams.setMargins(0,(int)(12 * heightsize),0,(int)(12 * heightsize));
                    choice_group.addView(tempButton,layoutParams);
                }
                break;
            case MAX_CAST_COUNT:
                max_cast_count_list = Arrays.asList(res.getStringArray(R.array.max_cast_count_type));
                for(int i=0; i < max_cast_count_list.size(); i++)
                {
                    RadioButton tempButton = new RadioButton(this);
                    tempButton.setBackgroundResource(R.drawable.choice_item);
                    tempButton.setButtonDrawable(android.R.color.transparent);
                    tempButton.setGravity(Gravity.CENTER);
                    tempButton.setPadding((int)(115* widthsize), (int)(10 * heightsize), (int)(115* widthsize), (int)(10 * heightsize));
                    tempButton.setText(max_cast_count_list.get(i));
                    tempButton.setTextColor(getResources().getColor(R.color.dev_back));
                    tempButton.setTextSize(16);
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams((int)(600* widthsize),(int)(60 * heightsize));
                    layoutParams.setMargins(0,(int)(12 * heightsize),0,(int)(12 * heightsize));
                    choice_group.addView(tempButton,layoutParams);
                }
                break;
            case FRAME_MODE:
                frame_mode_list = Arrays.asList(res.getStringArray(R.array.frame_mode_type));
                for(int i=0; i < frame_mode_list.size(); i++)
                {
                    RadioButton tempButton = new RadioButton(this);
                    tempButton.setBackgroundResource(R.drawable.choice_item);
                    tempButton.setButtonDrawable(android.R.color.transparent);
                    tempButton.setGravity(Gravity.CENTER);
                    tempButton.setPadding((int)(115* widthsize), (int)(10 * heightsize), (int)(115* widthsize), (int)(10 * heightsize));
                    tempButton.setText(frame_mode_list.get(i));
                    tempButton.setTextColor(getResources().getColor(R.color.dev_back));
                    tempButton.setTextSize(16);
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams((int)(600* widthsize),(int)(60 * heightsize));
                    layoutParams.setMargins(0,(int)(12 * heightsize),0,(int)(12 * heightsize));
                    choice_group.addView(tempButton,layoutParams);
                }
                break;
            case PLAY_MODE:
                play_mode_list = Arrays.asList(res.getStringArray(R.array.play_mode_type));
                for(int i=0; i < play_mode_list.size(); i++)
                {
                    RadioButton tempButton = new RadioButton(this);
                    tempButton.setBackgroundResource(R.drawable.choice_item);
                    tempButton.setButtonDrawable(android.R.color.transparent);
                    tempButton.setGravity(Gravity.CENTER);
                    tempButton.setPadding((int)(115* widthsize), (int)(10 * heightsize), (int)(115* widthsize), (int)(10 * heightsize));
                    tempButton.setText(play_mode_list.get(i));
                    tempButton.setTextColor(getResources().getColor(R.color.dev_back));
                    tempButton.setTextSize(16);
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams((int)(600* widthsize),(int)(60 * heightsize));
                    layoutParams.setMargins(0,(int)(12 * heightsize),0,(int)(12 * heightsize));
                    choice_group.addView(tempButton,layoutParams);
                }
                break;


        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (title){
            case MAX_FRAME:
                RadioButton max_frame_type = findViewById(group.getCheckedRadioButtonId());
                String item_max_frame = max_frame_type.getText().toString();
                setSelect(item_max_frame);
                break;
            case RESOLUTION:
                RadioButton resolution_type = findViewById(group.getCheckedRadioButtonId());
                String item_resolution = resolution_type.getText().toString();
                setSelect(item_resolution);
                break;
            case MAX_CAST_COUNT:
                RadioButton max_cast_count_type = findViewById(group.getCheckedRadioButtonId());
                String item_max_cast_count = max_cast_count_type.getText().toString();
                setSelect(item_max_cast_count);
                break;
            case FRAME_MODE:
                RadioButton frame_mode_type = findViewById(group.getCheckedRadioButtonId());
                String frame_mode_count = frame_mode_type.getText().toString();
                setSelect(frame_mode_count);
                break;
            case PLAY_MODE:
                RadioButton play_mode_type = findViewById(group.getCheckedRadioButtonId());
                String play_mode_count = play_mode_type.getText().toString();
                setSelect(play_mode_count);
                break;

        }
    }

    private void setSelect(String item){

        switch (title){
            case MAX_FRAME:
                for (int i = 0; i < max_frame_list.size(); i++){
                    if(max_frame_list.get(i).equals(item)){
                        SharedPreferenceHelper.getInstance().saveMaxFrame(i);
                        DemoApplication.APP.getEventBus().post(new MaxFrameEvent(i));
                        finish();
                    }
                }
                break;
            case RESOLUTION:
                for (int i = 0; i < resolution_list.size(); i++){
                    if(resolution_list.get(i).equals(item)){
                        SharedPreferenceHelper.getInstance().saveResolution(i);
                        DemoApplication.APP.getEventBus().post(new ResolutionEvent(i));
                        finish();
                    }
                }
                break;
            case MAX_CAST_COUNT:
                for (int i = 0; i < max_cast_count_list.size(); i++){
                    if(max_cast_count_list.get(i).equals(item)){
                        SharedPreferenceHelper.getInstance().saveMaxCastCount(i);
                        DemoApplication.APP.getEventBus().post(new MaxCastCountEvent(i));
                        finish();
                    }
                }
                break;
            case FRAME_MODE:
                for (int i = 0; i < frame_mode_list.size(); i++){
                    if(frame_mode_list.get(i).equals(item)){
                        SharedPreferenceHelper.getInstance().saveFrameMode(i);
                        DemoApplication.APP.getEventBus().post(new FrameModeEvent(i));
                        finish();
                    }
                }
                break;
            case PLAY_MODE:
                for (int i = 0; i < play_mode_list.size(); i++){
                    if(play_mode_list.get(i).equals(item)){
                        SharedPreferenceHelper.getInstance().savePlayMode(i);
                        DemoApplication.APP.getEventBus().post(new PlayModeEvent(i));
                        finish();
                    }
                }
                break;

        }
    }
    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }
}
