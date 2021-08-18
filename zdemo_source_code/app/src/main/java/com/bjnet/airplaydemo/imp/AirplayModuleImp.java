package com.bjnet.airplaydemo.imp;

import android.util.Log;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.base.MediaChannelCtx;
import com.bjnet.airplaydemo.event.AirAudioEvent;
import com.bjnet.airplaydemo.event.AirSurfaceEvent;
import com.bjnet.airplaydemo.event.AirUrlEvent;
import com.bjnet.airplaydemo.event.AirplayEvent;
import com.bjnet.airplaydemo.event.AirplayUrlPlayEndEvent;
import com.bjnet.airplaydemo.event.CloseChannelEvent;
import com.bjnet.airplaydemo.event.CreateChannelFailedEvent;
import com.bjnet.airplaydemo.util.SharedPreferenceHelper;
import com.bjnet.cbox.module.AirplayModule;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaChannelInfo;
import com.bjnet.cbox.module.MediaConst;
import com.bjnet.cbox.module.ModuleImpItf;
import com.bjnet.cbox.module.UserInfo;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Properties;

public class AirplayModuleImp extends ModuleImpItf {

    public static final int MAX_BUFFERED_AUDIO_SIZE_FOR_MIRROR = 44100 * 2; //500ms
    public static final int MAX_BUFFERED_AUDIO_SIZE_FOR_MIRROR_AFTERDROP = 44100; //250ms
    public static final int MAX_GAP_STATE_ROUND_NUM = 5;
    public static final int MAX_GAP_STATE_ROUND_SIZE_LIMIT = 8820; //50ms
    public static final int MAX_AVSYNC_LIMIT_SIZE = 25280; //200ms

    public AirplayModuleImp(){
        DemoApplication.APP.getEventBus().register(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onAirplayUrlPlayEndEvent(AirplayUrlPlayEndEvent msg) {
        MediaChannel channel = CastManager.getMgr().getChannelById(msg.getChannelID());
        Log.i(DemoApplication.TAG, "onAirplayUrlPlayEndEvent channel:"+msg);
        if (channel != null) {
            CastManager.getMgr().getAirplayModule().kickOut(channel);
        }
    }

    boolean channelAvailable(){
        return CastManager.getMgr().isChannelAvailable();
    }

    protected boolean openAndStartChannel(MediaChannel channel){
        boolean ret = false;
        CastManager.getMgr().registerChannel(channel);
        if (channel.open()) {
            //channel.start(userInfo);
            Log.i(DemoApplication.TAG, " reqMediaChannel open success id:"+channel.getChannelId());
            ret = true;
        } else {
            channel.close();
            CastManager.getMgr().unregisterChannel(channel);
            Log.i(DemoApplication.TAG, " reqMediaChannel open failed");
        }
        return ret;
    }

    @Override
    public MediaChannel reqMediaChannel(MediaChannelInfo info, UserInfo userInfo) {
        Log.i(DemoApplication.TAG, "reqMediaChannel: userInfo ip:"+userInfo.ip+" name:"+userInfo.deviceName+" model:"+userInfo.model);
         MediaChannel channel = null;
        synchronized (this) {
            if (!channelAvailable()) {
                Log.e(DemoApplication.TAG, "reqMediaChannel channelAvailable false");
                CreateChannelFailedEvent event = new CreateChannelFailedEvent(userInfo,DemoApplication.getContext().getResources().getString(R.string.err_screen_full));
                DemoApplication.APP.getEventBus().post(event);
                return null;
            }

            android.app.Activity activity = com.blankj.utilcode.util.ActivityUtils.getTopActivity();
            if(!(activity instanceof com.bjnet.airplaydemo.activity.LoginActivity)){
                Log.e(DemoApplication.TAG, "reqMediaChannel failed. not in home activity :"+activity);
                CreateChannelFailedEvent event = new CreateChannelFailedEvent(userInfo,DemoApplication.getContext().getResources().getString(R.string.err_cannot_share_notin_home));
                DemoApplication.APP.getEventBus().post(event);
                return null;
            }

            /**
             * 根据不同的ChannelPlayType，创建不同的channel
             */
            switch (info.getChannelPlayType()) {
                case MediaConst.MEDIA_TYPE_AV:
                    AirplayMirrorChannel mirrorChannel = new AirplayMirrorChannel(info);
                    mirrorChannel.setUserInfo(userInfo);
                    channel = mirrorChannel;
                    break;
                case MediaConst.MEDIA_TYPE_AUDIO:
                    Log.d(DemoApplication.TAG, "AirplayModule reqMediaChannel AUDIO");
                    channel = new AirplayAudioChannel(info);
                    break;
                case MediaConst.MEDIA_TYPE_FILE:
                case MediaConst.MEDIA_TYPE_AUDIO_MP3:
                    Log.d(DemoApplication.TAG, "AirplayModule reqMediaChannel URL");
                    channel = new AirplayUrlPlayChannel(info);
                    break;
                case MediaConst.MEDIA_TYPE_PICTUPE_PLAYBACK:
                    Log.d(DemoApplication.TAG, "AirplayModule reqMediaChannel PIC");
                    channel = new AirplayPicViewChannel(info);
                    break;
                default: {
                    Log.e(DemoApplication.TAG, "reqMediaChannel failed,because info.getChannelPlayType() invalid:" + info.getChannelPlayType());
                    return null;
                }
            }
        }

        if (!openAndStartChannel(channel)) {
            Log.e(DemoApplication.TAG, "reqMediaChannel failed,because openAndStartChannel failed.");
            return null;
        }

        //根据不同的
        switch (info.getChannelPlayType()) {
            case MediaConst.MEDIA_TYPE_AV: {
//                if (userInfo.model.startsWith("iPad") || userInfo.model.startsWith("iPhone")){
//                    DemoApplication.APP.getEventBus().post(new AirplayEvent(channel));
//                }else {
                boolean isSurfaceView = SharedPreferenceHelper.getInstance().getIsSurfaceView();
                if (isSurfaceView){
                    DemoApplication.APP.getEventBus().post(new AirSurfaceEvent(channel));
                }else {
                    DemoApplication.APP.getEventBus().post(new AirplayEvent(channel));
                }

//                }
            }
            break;
            case MediaConst.MEDIA_TYPE_AUDIO: {
                DemoApplication.APP.getEventBus().post(new AirAudioEvent(channel));
            }
            break;
            case MediaConst.MEDIA_TYPE_FILE: {
                DemoApplication.APP.getEventBus().post(new AirUrlEvent(channel));
            }
            break;
            default:
                break;
        }
        return channel;
    }


    @Override
    public void relMediaChannel(MediaChannel channel) {
        channel.close();
        CloseChannelEvent event = new CloseChannelEvent(channel.getChannelId());
        DemoApplication.APP.getEventBus().post(event);

        CastManager.getMgr().updateChannelFlag(channel.getChannelId(), MediaChannelCtx.CHANNEL_CLOSED_MASK);
    }

    @Override
    public void probeRenderAbility(Properties props){
        //用户可定制改函数，实现Airplay投屏会话的最大帧率，参考如下代码，将帧率设置为60
//        int framerate = 60;
//        props.setProperty(AirplayModule.PARA_NAME_KEY_FRAMERATE,Integer.toString(framerate));
    }
}

