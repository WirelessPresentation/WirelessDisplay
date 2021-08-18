package com.bjnet.airplaydemo.activity;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.ScreenRenderService;
import com.bjnet.airplaydemo.base.DeviceName;
import com.bjnet.airplaydemo.base.PermissionListener;
import com.bjnet.airplaydemo.event.FinishEvent;
import com.bjnet.airplaydemo.util.SharedPreferenceHelper;
import com.bjnet.airplaydemo.util.ViewHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class LoginActivity extends FragmentActivity {

    public static final String TAG = "ScreenRender";

    private NetworkReceiver receiver = new NetworkReceiver();
    private static final String ETHNAME = "eth0";

    private View mBar;
    private View mLoadingView;
    private TextView device;
    private TextView ip;
    TextView pinCode;
    ImageView settings;
    private int mShortAnimationDuration;
    private int deviceNumber;
    ConstraintLayout bigP;
    DeviceName deviceName;
    private SharedPreferences sharedPreferences;

    private static boolean wifiConnected = false;
    private static boolean mobileConnected = false;
    private static boolean ethernetConnected = false;

    public int screenWidth1,screenHeight1;
    private float widthsize;
    private float heightsize;

    LinearLayout linearLayout;
    ViewHelper viewHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.device), Context.MODE_PRIVATE);

        int count = SharedPreferenceHelper.getInstance().getMaxCastCount();
        List<String> maxcastcount_list = Arrays.asList(getResources().getStringArray(R.array.max_cast_count_type));
        CastManager.getMgr().setMaxCastCount(Integer.parseInt(maxcastcount_list.get(count)));

        Log.i("CastManager", "MaxCastCount: "+CastManager.getMgr().getMaxCastCount());
        Log.i("CastManager", "IsSurfaceView: "+ SharedPreferenceHelper.getInstance().getIsSurfaceView());
        initView();


        viewHelper = new ViewHelper(this, linearLayout);
        deviceName = new DeviceName(getApplicationContext());
        deviceNumber = deviceName.getNumber();
        startRenderService();
    }



    private void startRenderService() {
        Intent startSvc = new Intent(this, ScreenRenderService.class);
        this.startService(startSvc);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();

        View decoeView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decoeView.setSystemUiVisibility((uiOptions));
        Resources resources = getResources();
        initLayout();
        if (CastManager.getMgr().isEnablePin()) {
            refreshPin();
        } else {
            pinCode.setText(getString(R.string.no_pincode));
        }

        mBar.setVisibility(View.GONE);

        if (deviceName.getDeviceName() != null) {
            device.setText(deviceName.getDeviceName());
        } else {
            device.setText("BJAirplayDemo_" + String.valueOf(deviceNumber));
        }
        checkNetworkConnection();

        mShortAnimationDuration = 3000;

        mBar.setAlpha(0f);
        mBar.setVisibility(View.VISIBLE);

        mBar.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        String[] per = {"android.permission.WRITE_EXTERNAL_STORAGE"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRuntimePermission(per, new PermissionListener() {
                @Override
                public void onGranted() {
                    Log.d(TAG, "onGranted: ");
                }

                @Override
                public void onDenied(List<String> deniedPermission) {
                    Log.d(TAG, "onDenied: ");
                }
            });
        }
    }



    public void settings(View v) {

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void checkNetworkConnection() {
        // BEGIN_INCLUDE(connect)
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            Log.i(TAG, "checkNetworkConnection: activeInfo type:" + activeInfo.getType());
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            ethernetConnected = activeInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
            if (wifiConnected) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                Log.i(TAG, "checkNetworkConnection: wifiInfo:" + wifiInfo);
                if (wifiInfo.getIpAddress() == 0) {
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                    if (NetworkInfo.State.CONNECTED == networkInfo.getState()) {
                        ip.setText(getLocalIp());
                    }
                } else {
                    String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
                    ip.setText(ipAddress);
                }
                //Log.i(TAG, getString(R.string.wifi_connection));
            } else if (ethernetConnected) {
                ip.setText(getLocalIp());
            }
        } else {
            Log.e(TAG, "checkNetworkConnection: no activeInfo");
            ip.setText(getString(R.string.no_network));
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Intent startSvc = new Intent(this, ScreenRenderService.class);
        this.stopService(startSvc);
        viewHelper.onDestroy();
        Log.i(TAG, "MainActivity: onDestroy:");
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onRefreshPinEvent(com.bjnet.airplaydemo.event.RefreshPinEvent event) {
        refreshPin();
    }

    private void refreshPin() {

        Log.i(TAG, "refreshPin: pincode:" + CastManager.getMgr().getPincode());

        if (CastManager.getMgr().isEnablePin()) {
            pinCode.setText(CastManager.getMgr().getPincode());
        } else {
            pinCode.setText(getString(R.string.no_pincode));
        }
    }

    private static String getLocalIp() {
        String ip = "0.0.0.0";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address && (intf.getDisplayName().equals(ETHNAME))) {
                        ip = inetAddress.getHostAddress();
                        Log.i(TAG, "getLocalIp: ip:" + ip + " name:" + intf.getDisplayName());
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return ip;
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    protected void maxSoundVolume() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkConnection();
        }
    }

    private long oldtime =0;
    @Override
    public void onBackPressed() {

        if (viewHelper.hasViews()) {
            viewHelper.clear();
        } else {
            long newtime = System.currentTimeMillis();
            if (oldtime == 0|| newtime - oldtime > 1000 ){
                oldtime = newtime;
                Toast.makeText(LoginActivity.this,"再次点击返回键切换到桌面",Toast.LENGTH_SHORT).show();
            }else {
                super.onBackPressed();
            }
        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFinishEvent(FinishEvent event) {//关闭应用
        Log.i("111111111111", "onFinishEvent: ");
        final AlertDialog.Builder finishDialog =
                new AlertDialog.Builder(LoginActivity.this);
        finishDialog.setTitle(getString(R.string.restart));
        finishDialog.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitAPP();
                    }
                });
        finishDialog.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        // 显示
        finishDialog.show();

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void exitAPP() {
        CastManager.getMgr().getAirplayModule().disableDiscover();

        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
        for (ActivityManager.AppTask appTask : appTaskList) {
            appTask.finishAndRemoveTask();
        }
        System.exit(0);
    }


    private PermissionListener mListener;

    public void requestRuntimePermission(String[] permissions, PermissionListener listener){

        List<String> permissionList = new ArrayList<>();

        mListener = listener;

        for (String permission : permissions){
            if (ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                permissionList.add(permission);
            }
        }

        if (!permissionList.isEmpty()){
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]),1);
        } else {
            listener.onGranted();
            //dosomething();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    List<String> deniedPermissions = new ArrayList<>();
                    for (int i = 0; i < grantResults.length; i++){
                        int grantResult = grantResults[i];
                        String deniedPermission = permissions[i];
                        if (grantResult != PackageManager.PERMISSION_GRANTED){
                            deniedPermissions.add(deniedPermission);
                        }
                    }

                    if (deniedPermissions.isEmpty()){
                        mListener.onGranted();
                    } else {
                        mListener.onDenied(deniedPermissions);
                    }
                }
                break;
            default:
                break;
        }
    }

    private ImageView iv_device,iv_pincode,iv_ip;
    private LinearLayout ll_tittle;
    private final float testSizeValue =  (float) 3/4;
    private void initView() {
        bigP = findViewById(R.id.big_p);
        mBar = findViewById(R.id.info_bar);
        mLoadingView = findViewById(R.id.loading);
        device = findViewById(R.id.device);
        ip = findViewById(R.id.ip);
        settings = findViewById(R.id.setting);
        pinCode = findViewById(R.id.pincode);

        linearLayout = findViewById(R.id.parent);
        ll_tittle = findViewById(R.id.ll_tittle);
        iv_device = findViewById(R.id.iv_device);
        iv_pincode= findViewById(R.id.iv_pincode);
        iv_ip= findViewById(R.id.iv_ip);

    }
    private void initLayout() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        screenWidth1=dm.widthPixels;
        screenHeight1=dm.heightPixels;

        widthsize = (float) screenWidth1 / 1080;
        heightsize = (float) screenHeight1 / 720;

        ConstraintLayout.LayoutParams ll_tittlelp = (ConstraintLayout.LayoutParams) ll_tittle.getLayoutParams();
        ll_tittlelp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        ll_tittlelp.height = (int)(70 * heightsize);
        ll_tittle.setLayoutParams(ll_tittlelp);


        LinearLayout.LayoutParams iv_devicelp = (LinearLayout.LayoutParams) iv_device.getLayoutParams();
        iv_devicelp.width = (int)(80 * widthsize);
        iv_devicelp.height = (int)(26 * heightsize);
        iv_device.setLayoutParams(iv_devicelp);


        LinearLayout.LayoutParams iv_pincodelp = (LinearLayout.LayoutParams) iv_pincode.getLayoutParams();
        iv_pincodelp.width = (int)(80 * widthsize);
        iv_pincodelp.height = (int)(26 * heightsize);
        iv_pincode.setLayoutParams(iv_pincodelp);


        LinearLayout.LayoutParams iv_iplp = (LinearLayout.LayoutParams) iv_ip.getLayoutParams();
        iv_iplp.width = (int)(80 * widthsize);
        iv_iplp.height = (int)(26 * heightsize);
        iv_ip.setLayoutParams(iv_iplp);

        ConstraintLayout.LayoutParams settingslp = (ConstraintLayout.LayoutParams) settings.getLayoutParams();
        settingslp.width = (int)(30 * widthsize);
        settingslp.height = (int)(30 * heightsize);
        settingslp.setMarginEnd((int)(30 * widthsize));
        settings.setLayoutParams(settingslp);







    }
}
