package com.bjnet.airplaydemo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.bjnet.airplaydemo.base.MediaChannelCtx;
import com.bjnet.airplaydemo.event.RefreshPinEvent;
import com.bjnet.airplaydemo.event.RenameEvent;
import com.bjnet.airplaydemo.imp.AirplayModuleImp;
import com.bjnet.airplaydemo.util.SharedPreferenceHelper;
import com.bjnet.cbox.module.AirplayModule;
import com.bjnet.cbox.module.AirplayModulePara;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.licensev3.apply.ApplyLicenseRetInfo;
import com.bjnet.licensev3.apply.BJLicenseMoudle;
import com.bjnet.licensev3.apply.LicenseInfo;
import com.blankj.utilcode.util.LogUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.bjnet.airplaydemo.DemoApplication.TAG;

/**
 * Created by supermanwg on 2016/10/7.
 */
public class CastManager {
    private static int MSG_NO_SEED = 0;
//    public static final int MSG_CHANNEL_CLOSED = MSG_NO_SEED++;
//    public static final int MSG_CHANNEL_UPDATE_PICTURE = MSG_NO_SEED++;

    public static final int MSG_START_MIRROR_DISPLAY = MSG_NO_SEED++;
    public static final int MSG_SURFACE_READY = MSG_NO_SEED++;
    public static final int MSG_SURFACE_DESTROYED = MSG_NO_SEED++;
    public static final int MSG_UI_SURFACE_CREATED = MSG_NO_SEED++;
    public static final int MSG_UI_SURFACE_DESTROYED = MSG_NO_SEED++;
    //public static final int MSG_CHANNEL_CONTENT_SIZE_CHANGED = MSG_NO_SEED++;
    //public static final int MSG_CHANNEL_VIDEO_ROTATE_CHANGED = MSG_NO_SEED++;

    private ConcurrentHashMap<Integer, MediaChannelCtx> channelCtxMap;
    private String pincode = "";
    private static CastManager instance = null;
    private int maxCastCount = 1;
    private static int idSeed = 0;
    private boolean isEnablePin = false; //is enable pin code

    public boolean isFirstUpgrade() {
        return isFirstUpgrade;
    }

    public void setFirstUpgrade(boolean firstUpgrade) {
        isFirstUpgrade = firstUpgrade;
    }

    private boolean isFirstUpgrade = true;

    public AirplayModule getAirplayModule() {
        return airplayModule;
    }
    private AirplayModule airplayModule = null;

    private Handler svcHandler = null;

    public MediaChannelCtx getChannelCtxById(int channelId) {
        //return curRenderChannel;
        if (channelCtxMap.containsKey(channelId)) {
            return channelCtxMap.get(channelId);
        }
        return null;
    }

    public MediaChannel getChannelById(int channelId) {
        //return curRenderChannel;
        if (channelCtxMap.containsKey(channelId)) {
            return channelCtxMap.get(channelId).getChannel();
        }
        return null;
    }

//    private MediaChannel curRenderChannel = null;

    private CastManager() {
//        this.curRenderChannel = null;
        channelCtxMap = new ConcurrentHashMap<>();
        DemoApplication.APP.getEventBus().register(this);
    }

    public void postMessageToSvc(Message msg) {
        if (this.svcHandler != null) {
            this.svcHandler.sendMessage(msg);
        }
    }

    private int generateChannelID() {
        return idSeed++;
    }

    public void registerChannel(MediaChannel channel) {
        if (channel.getChannelId() == -1) {
            int id = generateChannelID();
            channel.setChannelId(id);
        }

        MediaChannelCtx ctx = new MediaChannelCtx(channel);
//        curRenderChannel = channel;
        synchronized (channelCtxMap) {
            this.channelCtxMap.put(channel.getChannelId(), ctx);
        }
        LogUtils.dTag(TAG, "registerChannel id = " + channel.getChannelId());
    }

    public void updateChannelFlag(int channelId, int flag) {
        LogUtils.dTag(TAG, "updateChannelFlag id = " + channelId + " flag=" + flag);
        synchronized (this.channelCtxMap) {
            if (this.channelCtxMap.containsKey(channelId)) {
                MediaChannelCtx ctx = this.channelCtxMap.get(channelId);
                if (ctx != null) {
                    int mask = ctx.getChannelMask();
                    int newMask = mask | flag;
                    ctx.setChannelMask(newMask);
                    LogUtils.dTag(TAG, "updateChannelFlag id = " + channelId + " newMask=" + newMask);
                    if ((newMask & 0x0A) == 0x0A) {
                        unregisterChannel(ctx.getChannel());
                    }
                }
            }
        }
    }

    public void unregisterChannel(MediaChannel channel) {
        LogUtils.dTag(TAG, "unregisterChannel id = " + channel.getChannelId());
        synchronized (channelCtxMap) {
            this.channelCtxMap.remove(channel.getChannelId());
        }
    }

    public static CastManager getMgr() {
        if (null == instance) {
            instance = new CastManager();
            //instance.refreshPin();
        }
        return instance;
    }

    public boolean isChannelAvailable() {
        int channelAva = 0;
        synchronized (channelCtxMap) {
            if (channelCtxMap.isEmpty()) {
                return true;
            }

            Set<Integer> keys = channelCtxMap.keySet();
            for (int k : keys) {
                MediaChannelCtx ctx = channelCtxMap.get(k);
                if ((ctx.getChannelMask() & MediaChannelCtx.CHANNEL_CLOSED_MASK) == 0) {
                    ++channelAva;
                }
            }
        }


        return (channelAva <= (maxCastCount-1));
    }

    public int getMaxCastCount() {
        return maxCastCount;
    }

    public void setMaxCastCount(int maxCastCount) {
        this.maxCastCount = maxCastCount;
    }

    private void prepareAirplayModule(String deviceName, String secrectKey) {
        this.airplayModule = new AirplayModule();
        this.airplayModule.setImp(new AirplayModuleImp());

        Properties paras = new Properties();
        paras.setProperty(AirplayModule.PARA_NAME_DEVICE_NAME,deviceName);

        int maxFrameid = SharedPreferenceHelper.getInstance().getMaxFrame();
        List<String> max_frame_list = Arrays.asList(DemoApplication.getContext().getResources().getStringArray(R.array.max_frame_type));
        String maxFrame = max_frame_list.get(maxFrameid);
        Log.i("CastManager", "prepareAirplayModule: maxFrame  "+maxFrame);
        paras.setProperty(AirplayModule.PARA_NAME_KEY_FRAMERATE,maxFrame);//??????

        if (SharedPreferenceHelper.getInstance().getPlayMode() == 0){//????????????url??????
            paras.setProperty("airplay_url","0");
        }else {
            paras.setProperty("airplay_url","1");
        }


        int resolutionid = SharedPreferenceHelper.getInstance().getResolution();
        Log.i("CastManager", "prepareAirplayModule: resolutionid  "+resolutionid);
        paras.setProperty(AirplayModule.PARA_NAME_KEY_RESOLUTION,Integer.toString(resolutionid));//????????? 0:1080p???1???720p???

        int frame_mode_id = SharedPreferenceHelper.getInstance().getFrameMode();
        Log.i("CastManager", "prepareAirplayModule: frame_mode_id  "+frame_mode_id);
        if (frame_mode_id ==0){
            paras.setProperty("enable_mbnaul","1"); //ENAbleMB?????? ??????????????????????????????????????????
        }

        Configuration mConfiguration = DemoApplication.getContext().getResources().getConfiguration(); //???????????????????????????
        int ori = mConfiguration.orientation; //??????????????????

        paras.setProperty(AirplayModule.PARA_NAME_KEY_ROTATION,Integer.toString(0));
        paras.setProperty(AirplayModule.PARA_NAME_SECRETE_KEY,secrectKey);
        int count = SharedPreferenceHelper.getInstance().getMaxCastCount();
        List<String> maxcastcount_list = Arrays.asList(DemoApplication.getContext().getResources().getStringArray(R.array.max_cast_count_type));
        String maxcastcount = maxcastcount_list.get(count);

        paras.setProperty("max_session_nums",maxcastcount);//?????????????????????????????????16
        paras.setProperty("enable_spsppsnaul","0");
        int errorCode = airplayModule.init(paras);
        if (errorCode == AirplayModule.BJ_AIRPLAY_ERROR_SUCCESS) {
            LogUtils.dTag(TAG, "airplayModule init success");
        } else {
            LogUtils.eTag(TAG, "airplayModule init failed : " + errorCode);
        }
    }

    private String usercode = "";//?????????????????????UserCode
    private String proGuardSalt = "";//??????????????????????????????
    private String licenseNO = "";//???????????????
    private int proGuardMode = 0;//???????????? 0???????????????1?????????

    public void prepareBJLicenseMoudle(String deviceName){
        BJLicenseMoudle moudle = BJLicenseMoudle.getInstance();
        String key = "BJ_TEST_KEY";

        if(SharedPreferenceHelper.getInstance().getSecrectKey() == null || SharedPreferenceHelper.getInstance().getSecrectKey().equals("")){

            String deviceId = sah1DeviceId(getMac() + proGuardSalt);//?????????deviceId??????Mac??????

//            ApplyLicenseRetInfo info  = moudle.applyLicenseWithLicenseNo(usercode,
//                    licenseNO,deviceId, proGuardMode, proGuardSalt);//????????????????????????????????????

            ApplyLicenseRetInfo info = moudle.applyLicense(usercode,
                    deviceId,
                    proGuardMode,proGuardSalt);            //userCode???mac??????"a0:bb:3e:d2:8f:ee"
            //ApplyLicenseRetInfo???????????? getRetcode??????????????? getLicenseKey????????????
            Log.i(TAG, "initLicense ???" + info.toString());
            if(info.getRetcode() == 0){
                key = info.getLicenseKey();
                SharedPreferenceHelper.getInstance().saveSecrectKey(key);
            }else{
                key = "BJ_TEST_KEY";
            }
        }else {
            key = SharedPreferenceHelper.getInstance().getSecrectKey();
        }
        LicenseInfo info = new LicenseInfo();//?????????LicenseInfo???????????????null
        int error = BJLicenseMoudle.getInstance().getLicenseInfo(usercode,info);//????????????????????????????????????
        //`LicenseType`??? License??????????????? : 1???airplay???2???bjcast???4???miracast???8???dlna???16???chrome_cast???32???bjcast_sender???licExpiryDate??????????????????licenseSum???License?????????licenseRegisted???License?????????????????????
        Log.w(TAG, "license " + error+ " type :"+info.licenseType + " Date:"+info.licExpiryDate+ " sum :"+info.licenseSum+" reg:"+info.licenseRegisted );
        prepareAirplayModule(deviceName, key);
    }

    void unInitAirplayModule(){
        if(airplayModule != null){
            airplayModule.fini();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onRenameEvent(RenameEvent e){
        LogUtils.iTag("Rename", "onRenameEvent: name:"+e.getName());
        if (airplayModule != null){
            this.airplayModule.rename(e.getName());
        }
    }

    public void refreshPin() {
        if (isEnablePin()) {
            String pincode = "";
            java.util.Random r = new java.util.Random();
            for (int i = 0; i < 4; ++i) {
                int n = (r.nextInt() % 10);
                if (n < 0) {
                    n = n * -1;
                }
                pincode += n;
            }
            //String pincode = "1234";
            int ret = CastManager.getMgr().getAirplayModule().setPassword(pincode);
            if(ret == 0){
                setPincode(pincode);
            }else{
                Log.i(TAG, "refreshPin: can not update airplay pass now");
            }
        } else {
            setPincode("");
        }


//        ScreenRenderApp.APP.getEventBus().post(new RefreshPinEvent());
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public boolean isEnablePin() {
        return isEnablePin;
    }

    public void setEnablePin(boolean enablePin) {
        if (isEnablePin != enablePin) {
            LogUtils.iTag("CONF", "setEnablePin: " + enablePin);
            isEnablePin = enablePin;
            refreshPin();
            DemoApplication.APP.getEventBus().post(new RefreshPinEvent());
        }
    }

    private int player = 0;


    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    private String getMac(){
        StringBuilder mac = new StringBuilder();
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
            byte[] macbyte= networkInterface.getHardwareAddress();
            if(macbyte!=null){
                for(int i=0;i<macbyte.length;i++) {
                    byte b=macbyte[i];
                    mac.append(String.format("%02X", b));
                    if(i!=macbyte.length-1) mac.append(":");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return mac.toString();
    }

    //SAH1??????
    private String sah1DeviceId(String src) {
        String deviceId = "";
        Log.d(TAG, "sah1DeviceId: src " + src);
        try {
            byte[] msg = src.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA1");
            deviceId = encodeHex(md.digest(msg));
        } catch (Exception e) {
            Log.e(TAG, "execute SHA1 error", e);
        }
        Log.d(TAG, "sah1DeviceId: deviceId " + deviceId);
        return deviceId;
    }
    private String encodeHex(byte[] data) {
        StringBuilder mac = new StringBuilder();
        if(data!=null){
            for (byte b : data) {
                mac.append(String.format("%02x", b));
            }
        }
        return mac.toString();
    }
}
