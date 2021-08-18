package com.bjnet.airplaydemo.imp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.base.GlSession;
import com.bjnet.airplaydemo.base.MediaConfHelper;
import com.bjnet.airplaydemo.event.VideoRotateEvent;
import com.bjnet.airplaydemo.event.VideoSizeEvent;
import com.bjnet.cbox.module.AirplayModule;
import com.bjnet.cbox.module.ComBuffer;
import com.bjnet.cbox.module.ComBufferPool;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaChannelInfo;
import com.bjnet.cbox.module.UserInfo;
import com.bjnet.cbox.module.Util;
import com.bjnet.cbox.module.WorkHandler;
import com.bjnet.cbox.module.WorkThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AirplayMirrorChannel extends MediaChannel {
    private static final String TAG = "AIRPLAY";
    private MediaCodec videoDecoder = null;
    private Handler videoDecodeHandler = null;
    private Runnable videoDecoderWorker = null;
    private MediaCodec.BufferInfo info = null;
    private HandlerThread videoDecodeThread = null;

    private AudioTrack audioPlayer = null;
    private Timer timer = null;
    private ReentrantLock audioLock = new ReentrantLock();
    //private ReentrantLock surfaceLock = new ReentrantLock();
    private Condition audioCond = audioLock.newCondition();
    private ConcurrentLinkedQueue<ComBuffer> bufferedAudioQueue = new ConcurrentLinkedQueue<ComBuffer>();
    private ConcurrentLinkedQueue<ComBuffer> bufferedVideoQueue = new ConcurrentLinkedQueue<ComBuffer>();
    private VideoStatInfo videoStat = new VideoStatInfo();
    private MediaFormat format = null;

    private int[] gapSizeStateArray = new int[AirplayModuleImp.MAX_GAP_STATE_ROUND_NUM];
    private boolean canDropSomeAudio = false;
    private AudioPlayerStatInfo audioStat = new AudioPlayerStatInfo();
    private GlSession glSession;
    private WorkHandler audioHandler = null;
    private WorkThread audioThread = null;    //音频播放线程
    private AudioRenderWorker audioWorker = null;
    private Surface surface = null;
    private Random random = new Random(System.currentTimeMillis());

    private int videoWidth = -1;
    private int videoHeight = -1;
    private int video_rotate = 0;

    private UserInfo userInfo;

    public AirplayMirrorChannel(MediaChannelInfo info) {
        super(info);
    }

    @Override
    public boolean open() {
        setState(MCState.MC_OPENED);
        int minSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize * 2, AudioTrack.MODE_STREAM);
        audioPlayer.play();

        //另外起一个音频播放线程，这样不会阻塞JNI内部的线程
        audioThread = new WorkThread("mirror_audio_" + getChannelId());
        audioThread.start();
        this.audioHandler = new WorkHandler(audioThread.getLooper());
        audioWorker = new AudioRenderWorker();
        audioHandler.post(audioWorker);

        if (MediaConfHelper.getInstance().isEnableChannelStat()){
            this.timer = new Timer();
            this.timer.schedule(new AirplayMirrorChannel.ChannelStatTask(), 1000, 1000);
        }
        return openMediaCodecDecoder();
    }

    public boolean openMediaCodecDecoder(){
        if ( Util.getApiLevel() < 19){
            return true;
        }

        try {
            videoDecoder = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            Log.e(TAG, "createDecoder failed" + e.getMessage());
            return false;
        }
        if (videoDecoder == null) {
            Log.e(TAG, "Can't find video info!");
            return false;
        }
        format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 446859);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        videoDecoder.configure(format, this.surface, null, 0);
        Log.d(DemoApplication.TAG, "videoDecoder.configure");
        if(null != surface) {
            videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            Log.d(DemoApplication.TAG, "videoDecoder.setVideoScalingMode");
        }
        videoDecoder.start();
        this.info = new MediaCodec.BufferInfo();

        this.videoDecodeThread = new HandlerThread("ScreenRender_"+getChannelId());
        this.videoDecodeThread.start();
        this.videoDecodeHandler = new ScreenRenderChannelHandler(this.videoDecodeThread.getLooper());
        this.videoDecoderWorker = new VideoDecoderWorker();
        this.videoDecodeHandler.post(videoDecoderWorker);
        return true;
    }

    void  closeMediaCodecDecoder(){
        if ( Util.getApiLevel() < 19){
            return;
        }
        if (this.videoDecodeThread !=  null){
            this.videoDecodeHandler.post(videoDecoderWorker);
            this.videoDecodeThread.quit();
            try {
                this.videoDecodeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.videoDecodeThread = null;
            this.videoDecodeHandler = null;
        }
        Log.i(DemoApplication.TAG, "channel videoDecodeThread: stop now");
        if (videoDecoder != null){
            videoDecoder.flush();
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
    }

    @Override
    public void close() {
        setState(MCState.MC_DEAD);
        closeMediaCodecDecoder();

        if(null != timer) {
            this.timer.cancel();
        }
        if (audioThread != null){
            try {
                audioLock.lock();
                audioCond.signal();
            }  finally {
                audioLock.unlock();
            }
            audioThread.quit();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (audioPlayer != null) {
                audioPlayer.stop();
                audioPlayer.release();
                audioPlayer = null;
                Log.d(TAG, "AirplayMirrorChannel audioPlayer release "+getChannelId());
            }
        }
        Log.d(TAG, "AirplayMirrorChannel close()");

//        Handler handler = CastManager.getMgr().getUiHandler();
//        if (handler != null){
//            Message msg = new Message();
//            msg.what = CastManager.MSG_CHANNEL_CLOSED;
//            handler.sendMessage(msg);
//        }

        Log.i(DemoApplication.TAG, "MirrorChannel close: ");
        this.glSession = null;
    }

    @Override
    public void onFrame(int frameLen, int w, int h) {
        if (glSession != null){
            glSession.onFrame(frameLen,w,h); //通知GLSufraceView进行渲染
        }
    }

    @Override
    public void onVideoFrame(ComBuffer buffer){
        bufferedVideoQueue.offer(buffer);
        ++videoStat.videoFramesInputThisRound;
        ++videoStat.totalVideoFramesInput;
    }

    @Override
    public void rotate(int angle){
        if (video_rotate != angle){
            Log.i(TAG, "set rotate: angle:"+angle);
            video_rotate = angle;

            VideoRotateEvent event = new VideoRotateEvent(channelId,angle);
            DemoApplication.APP.getEventBus().post(event);
        }
    }

    @Override
    public  void  onAudioFrame(byte[] buffer,int len,long ts){
        ComBuffer data = new ComBuffer(buffer,len,ts);
        bufferedAudioQueue.add(data);
        ++audioStat.audioFramesInputThisRound;
        audioStat.audioSizeInThisRound += data.getLen();
        audioStat.audioSizeInBuffer.addAndGet(data.getLen());
        ++audioStat.totalAudioFramesInput;
        try {
            audioLock.lock();
            audioCond.signal();
        } finally {
            audioLock.unlock();
        }
    }

    public GlSession getGlSession() {
        return glSession;
    }

    /**
    * GLView建立好后，此时再设置Video Render buffer到JNI层
     */
    public void setGlSession(GlSession glSession) {
        this.glSession = glSession;
        AirplayModule castModule = CastManager.getMgr().getAirplayModule();
        castModule.setRenderBuffer(this,glSession.getBuffer(),glSession.getBufferSize());
    }

    /**
    * 缓存的音频太多，会做一些丢弃。一般在性能不足或者传输抖动比较大时会出现。
     */
    private void dropTooOldAudio() {
        int dropNum = 0;
        while (!bufferedAudioQueue.isEmpty()) {
            if (audioStat.audioSizeInBuffer.get() > AirplayModuleImp.MAX_BUFFERED_AUDIO_SIZE_FOR_MIRROR_AFTERDROP) {
                ComBuffer buf = bufferedAudioQueue.poll();
                ++audioStat.totalAudioFramesOut;
                ++audioStat.audioFramesDropThisRound;
                audioStat.dropAudioSizeThisRound += buf.getLen();
                audioStat.audioSizeInBuffer.addAndGet(-1 * buf.getLen());
                ++dropNum;
            } else {
                break;
            }
        }
        Log.w("AIRPLAY_STAT", "dropTooOldAudio: dropAudio num:" + dropNum + " channel:" + getChannelId());
    }

    private void dropSomeTooOldAudio() {
        int dropSize = 0;
        int dropNum = 0;
        while (!bufferedAudioQueue.isEmpty()) {
            if (dropSize < AirplayModuleImp.MAX_GAP_STATE_ROUND_SIZE_LIMIT) {
                ComBuffer buf = bufferedAudioQueue.poll();
                ++audioStat.totalAudioFramesOut;
                ++audioStat.audioFramesDropThisRound;
                audioStat.dropAudioSizeThisRound += buf.getLen();
                audioStat.audioSizeInBuffer.addAndGet(-1 * buf.getLen());
                dropSize += buf.getLen();
                ++dropNum;
            } else {
                break;
            }
        }
        Log.w("AIRPLAY_STAT", "dropSomeTooOldAudio: dropAudio num:" + dropNum + " channel:" + getChannelId());
    }

    public int getVideoRotate() {
        return video_rotate;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    private class AudioPlayerStatInfo {
        public int audioFramesInputThisRound;
        public int totalAudioFramesInput;
        public int totalAudioFramesOut;
        public int audioFramesOutThisRound;
        public int audioFramesDropThisRound;
        public int audioSizeInThisRound;
        public int audioSizeOutThisRound;
        public int audioFramesInBuffer;
        public AtomicInteger audioSizeInBuffer = new AtomicInteger(0);
        public int dropAudioSizeThisRound;
        public int lastRoundAudioFramesInBuffer;

        public AudioPlayerStatInfo() {
            audioFramesInputThisRound = 0;
            totalAudioFramesInput = 0;
            totalAudioFramesOut = 0;
            audioFramesOutThisRound = 0;
            audioSizeInThisRound = 0;
            audioSizeOutThisRound = 0;
            audioFramesInBuffer = 0;
            audioSizeInBuffer.set(0);
            dropAudioSizeThisRound = 0;
            lastRoundAudioFramesInBuffer = 0;
        }

        void reset() {
            audioFramesInputThisRound = 0;
            audioFramesOutThisRound = 0;
            audioSizeInThisRound = 0;
            audioSizeOutThisRound = 0;
            dropAudioSizeThisRound = 0;
            audioFramesDropThisRound = 0;
        }


        @Override
        public String toString() {
            return "AudioPlayerStatInfo{" +
                    "totalAudioFramesInput=" + totalAudioFramesInput +
                    ", totalAudioFramesOut=" + totalAudioFramesOut +
                    ", audioFramesInputThisRound=" + audioFramesInputThisRound +
                    ", audioFramesOutThisRound=" + audioFramesOutThisRound +
                    ", audioFramesDropThisRound=" + audioFramesDropThisRound +
                    ", audioSizeInThisRound=" + audioSizeInThisRound +
                    ", audioSizeOutThisRound=" + audioSizeOutThisRound +
                    ", audioFramesInBuffer=" + audioFramesInBuffer +
                    ", audioSizeInBuffer=" + audioSizeInBuffer.get() +
                    ", dropAudioSizeThisRound=" + dropAudioSizeThisRound +
                    '}';
        }
    }

//    @Override
//    public ComBuffer reqBuffer(int size) {
//        return this.bufferPool.reqBuffer(size);
//    }

    private class AudioRenderWorker implements Runnable {

        @Override
        public void run() {
            if (state == MCState.MC_DEAD) {
                return;
            }

            try {
                audioLock.lock();
                audioCond.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                audioLock.unlock();
            }

            while (!bufferedAudioQueue.isEmpty() && state != MCState.MC_DEAD) {
                if (audioStat.audioSizeInBuffer.get() >= AirplayModuleImp.MAX_BUFFERED_AUDIO_SIZE_FOR_MIRROR && MediaConfHelper.getInstance().isEnableChannelDropAudio()) {
                    dropTooOldAudio();
                    canDropSomeAudio = false;
                    for (int i = 0; i < AirplayModuleImp.MAX_GAP_STATE_ROUND_NUM; ++i) {
                        gapSizeStateArray[i] = AirplayModuleImp.MAX_GAP_STATE_ROUND_SIZE_LIMIT + 1;
                    }
                } else if (audioStat.audioSizeInBuffer.get() > AirplayModuleImp.MAX_AVSYNC_LIMIT_SIZE && canDropSomeAudio && MediaConfHelper.getInstance().isEnableChannelDropAudio()) {
                    dropSomeTooOldAudio();
                    canDropSomeAudio = false;
                    for (int i = 0; i < AirplayModuleImp.MAX_GAP_STATE_ROUND_NUM; ++i) {
                        gapSizeStateArray[i] = AirplayModuleImp.MAX_GAP_STATE_ROUND_SIZE_LIMIT + 1;
                    }
                } else {
                    ComBuffer buf = bufferedAudioQueue.poll();
                    audioPlayer.write(buf.buffer, 0, buf.getLen());
                    ++audioStat.totalAudioFramesOut;
                    ++audioStat.audioFramesOutThisRound;
                    audioStat.audioSizeOutThisRound += buf.getLen();
                    audioStat.audioSizeInBuffer.addAndGet(-1 * buf.getLen());
                }
            }

            if (state != MCState.MC_DEAD) {
                if (audioHandler != null) {
                    audioHandler.post(this);
                }
            }

//            audioPlayer.write(data.buffer, 0, data.getLen());
//            this.bufferPool.relBuffer(data);
        }
    }

    private class VideoStatInfo{
        public float videobitInputThisRound;
        public int videoFramesInputThisRound;
        public int totalVideoFramesInput;
        public int videoFramesOutThisRound;
        public int totalVideoFramesOut;
        public int videoFramesInBuffer;
        public int videoFramesQueneToDecodeThisRound;
        public int videoFramesQueneToDecodeThisRoundFailed;
        public int videoFramesDecodeOutThisRound;
        public int videoLoopCountThisRound;
        public int videoFramesRenderThisRound;
        public int videoFramesRenderFailedThisRound;
        public  int videoFramesNoQueneToDecodeRounds;

        public VideoStatInfo() {
            videobitInputThisRound = 0;
            videoFramesInputThisRound = 0;
            totalVideoFramesInput = 0;
            videoFramesOutThisRound = 0;
            totalVideoFramesOut = 0;
            videoFramesInBuffer = 0;
            videoFramesQueneToDecodeThisRound = 0;
            videoFramesDecodeOutThisRound = 0;
            videoLoopCountThisRound = 0;
            videoFramesRenderThisRound = 0;
            videoFramesRenderFailedThisRound = 0;
            videoFramesNoQueneToDecodeRounds = 0;
        }

        void reset(){
            videobitInputThisRound = 0;
            videoFramesInputThisRound = 0;
            videoFramesOutThisRound = 0;
            videoFramesQueneToDecodeThisRoundFailed = 0;
            videoFramesQueneToDecodeThisRound = 0;
            videoLoopCountThisRound = 0;
            videoFramesRenderThisRound = 0;
            videoFramesRenderFailedThisRound = 0;
        }

        @Override
        public String toString() {
            return "VideoStatInfo{" +
                    "videobitBpsThisRound=" + (videobitInputThisRound/1000) + "kbps" +
                    ",videoFramesInputThisRound=" + videoFramesInputThisRound +
                    ", totalVideoFramesInput=" + totalVideoFramesInput +
                    ", videoFramesOutThisRound=" + videoFramesOutThisRound +
                    ", totalVideoFramesOut=" + totalVideoFramesOut +
                    ", videoFramesInBuffer=" + videoFramesInBuffer +
                    ", videoFramesQueneToDecodeThisRound=" + videoFramesQueneToDecodeThisRound +
                    ", videoFramesQueneToDecodeThisRoundFailed=" + videoFramesQueneToDecodeThisRoundFailed +
                    ", videoFramesDecodeOutThisRound=" + videoFramesDecodeOutThisRound +
                    ", videoLoopCountThisRound=" + videoLoopCountThisRound +
                    ", videoFramesRenderThisRound=" + videoFramesRenderThisRound +
                    ", videoFramesRenderFailedThisRound=" + videoFramesRenderFailedThisRound +
                    ", videoFramesNoQueneToDecodeRounds=" + videoFramesNoQueneToDecodeRounds +
                    '}';
        }
    }

    private void decodeVideoAndRender(long timeoutUs) {

        if (videoDecoder == null){
            return;
        }
        ByteBuffer[] outputBuffers = videoDecoder.getOutputBuffers();
        int outIndex = videoDecoder.dequeueOutputBuffer(info, timeoutUs);
        switch (outIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:

                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                outputBuffers = videoDecoder.getOutputBuffers();

                break;

            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:

                Log.d(TAG, "New format " + videoDecoder.getOutputFormat());
                {

                    MediaFormat fmt = videoDecoder.getOutputFormat();

                    videoWidth = getWidthFromMediaFormat(fmt);
                    videoHeight = getHeightFromMediaFormat(fmt);

                    VideoSizeEvent e = new VideoSizeEvent(channelId,videoWidth,videoHeight);
                    DemoApplication.APP.getEventBus().post(e);

//                if (fmt.containsKey(MediaFormat.KEY_WIDTH)) {
//
//                    //msg.arg1 = fmt.getInteger(MediaFormat.KEY_WIDTH);
//
//                    Log.d(MainActivity.TAG, "format width = " + msg.arg1);
//                    videoWidth = msg.arg1;
//                }
//
//                if (fmt.containsKey(MediaFormat.KEY_HEIGHT)) {
//
//                    msg.arg2 = fmt.getInteger(MediaFormat.KEY_HEIGHT);
//
//                    Log.d(MainActivity.TAG, "format height = " + msg.arg2);
//                    videoHeight = msg.arg2;
//
//                }


                }

                break;

            case MediaCodec.INFO_TRY_AGAIN_LATER:

                // Log.d("CM", "dequeueOutputBuffer timed out!");

                break;

            default:

                ByteBuffer buffer = outputBuffers[outIndex];

                int bufferVedioSize = bufferedVideoQueue.size();
                if (bufferVedioSize > 30) {
                    if ((random.nextInt()) % 4 == 1) {
                        ++videoStat.videoFramesRenderThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, MCState.MC_RUN_FRONT == state);
                    } else {
                        ++videoStat.videoFramesRenderFailedThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, false);
                    }
                }
                else if (bufferVedioSize >= 25) {
                    if ((random.nextInt()) % 2 == 1) {
                        ++videoStat.videoFramesRenderFailedThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, false);
                    } else {
                        ++videoStat.videoFramesRenderThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, MCState.MC_RUN_FRONT == state);
                    }
                }
                else if (bufferVedioSize >= 12){
                    if((random.nextInt()) % 3 == 1){
                        ++videoStat.videoFramesRenderFailedThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, false);
                    }
                    else{
                        ++videoStat.videoFramesRenderThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, MCState.MC_RUN_FRONT == state);
                    }
                }
                else if (bufferVedioSize >= 6){
                    if((random.nextInt()) % 4 == 1){
                        ++videoStat.videoFramesRenderFailedThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, false);
                    }else{
                        ++videoStat.videoFramesRenderThisRound;
                        videoDecoder.releaseOutputBuffer(outIndex, MCState.MC_RUN_FRONT == state);
                    }
                }
                else{
                    ++videoStat.videoFramesRenderThisRound;
                    videoDecoder.releaseOutputBuffer(outIndex, MCState.MC_RUN_FRONT == state);
                }

                videoStat.videoFramesOutThisRound++;
                ++videoStat.totalVideoFramesOut;
                break;
        }
    }

    @Override
    public void setVolume(int volume) {
        super.setVolume(volume);
        float v = volume/100.0f;
        if(audioPlayer != null){
            audioPlayer.setStereoVolume(v,v);
        }
    }

    public void setSurface(Surface surface) {
        Log.i(TAG,"setSurface surface:"+surface);
        this.surface = surface;

        if (MCState.MC_DEAD == this.state) {
            return;
        }

        Message msg = new Message();
        msg.what = (this.surface == null)?CastManager.MSG_UI_SURFACE_DESTROYED:CastManager.MSG_UI_SURFACE_CREATED;
        if (this.videoDecodeHandler != null){
            this.videoDecodeHandler.sendMessage(msg);
        }
    }

    private int getWidthFromMediaFormat(MediaFormat format) {
        int width = 0;
        if (format.containsKey("crop-right") && format.containsKey("crop-left")) {
            int crop_right = format.getInteger("crop-right");
            int crop_left = format.getInteger("crop-left");
            width = crop_right + 1 - crop_left;
        } else if (format.containsKey(MediaFormat.KEY_WIDTH)) {
            width = format.getInteger(MediaFormat.KEY_WIDTH);
            Log.d(TAG, "onOutputFormatChanged format width = " + width + " format:" + format.toString());

        }
        return width;
    }

    private int getHeightFromMediaFormat(MediaFormat format) {
        int height = 0;
        if (format.containsKey("crop-top") && format.containsKey("crop-bottom")) {
            height = format.getInteger("crop-bottom") + 1 - format.getInteger("crop-top");
        } else if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
            height = format.getInteger(MediaFormat.KEY_HEIGHT);
            Log.d(TAG, "onOutputFormatChanged format width = " + height + " format:" + format.toString());
        }
        return height;
    }


    private class VideoDecoderWorker implements Runnable {
        private boolean isWaitIFrame = true;
        private byte end_buffer[] = {0x00, 0x00, 0x01, 0x1e, 0x48, 0x53, 0x50, 0x49, 0x43, 0x45,
                0x4e, 0x44, 0x00, 0x00, 0x01, 0x1e, 0x00, 0x00, 0x00, 0x00};
        private ComBuffer lastBuf = null;
        private int lastBufInputTimes = 0;

        @Override
        public void run() {
            videoStat.videoLoopCountThisRound++;
            if (MCState.MC_DEAD == state) {
                Log.d(DemoApplication.TAG, "ScreenRenderChannel.run() state = MC_DEAD id:" + getChannelId());
//                if (videoDecoder != null){
//                    videoDecoder.stop();
//                    videoDecoder.release();
//                    videoDecoder = null;
//                }
                return;
            }

//            if (videoDecoder == null){
//                try {
//                    videoDecoder = MediaCodec.createDecoderByType("video/avc");
//                    //videoDecoder.setCallback();
//                } catch (IOException e) {
//                    Log.e(LOGTAG, "createDecoder failed" + e.getMessage());
//                    return;
//                }
//                if (videoDecoder == null) {
//                    Log.e(LOGTAG, "Can't find video info!");
//                    return ;
//                }
//                format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720);
//                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 446859);
//                format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
//                videoDecoder.configure(format, surface, null, 0);
//                Log.d(MainActivity.TAG, "videoDecoder.configure");
//                if(null != surface) {
//                    videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
//                    Log.d(MainActivity.TAG, "videoDecoder.setVideoScalingMode");
//                }
//                videoDecoder.start();
//                info = new MediaCodec.BufferInfo();
//            }

            if (state != MCState.MC_RUN_FRONT){
                if(bufferedVideoQueue.size() > 120){
                    Log.i(TAG, "run: buffered too many video exit this session");
                    MediaChannel channel = CastManager.getMgr().getChannelById(channelId);
                    if (channel != null){
                        CastManager.getMgr().getAirplayModule().kickOut(channel);
                    }

                    return;
                }
                videoDecodeHandler.postDelayed(this, 10);
                return;
            }

            while (true) {
                if (state == MCState.MC_DEAD){
                    break;
                }

                if (bufferedVideoQueue.isEmpty() ) {

                    boolean needInputLast = false;
                    if(userInfo.model.startsWith("iPad")){
                        needInputLast = false;
                    }else if(userInfo.model.startsWith("iPhone")){
                        if (videoWidth > videoHeight){
                            needInputLast = true;
                        }
                    }

                    if (lastBuf != null && videoStat.videoFramesNoQueneToDecodeRounds >= 2 && lastBufInputTimes < 3){
                        videoStat.videoFramesNoQueneToDecodeRounds = 0;
                        ByteBuffer[] inputBuffers = videoDecoder.getInputBuffers();
                        int inIndex = videoDecoder.dequeueInputBuffer(0);
                        if (inIndex >= 0) {

                            ByteBuffer buffer = inputBuffers[inIndex];

                            buffer.clear();
                            long ts = 0;

                            ComBuffer b = lastBuf;
                            ++lastBufInputTimes;

                            buffer.put(b.buffer, 0, b.getLen());
                            //buffer.put(end_buffer,0,end_buffer.length);

                            ts = b.getTs();
                            buffer.flip();
                            ++videoStat.videoFramesQueneToDecodeThisRound;
                            videoDecoder.queueInputBuffer(inIndex, 0, buffer.limit(), ts, 0);
                            Log.d(TAG, "run: bufferedVideoQueue lastbuf again");
                            decodeVideoAndRender(0);
                        }
                        else
                        {
                            ++videoStat.videoFramesQueneToDecodeThisRoundFailed;
                            decodeVideoAndRender(0);
                            if (MCState.MC_DEAD != state) {
                                videoDecodeHandler.postDelayed(this, 10);
                            }
                            //handler.post(this);
                            return;
                        }
                    }
                    break;
                }
                lastBufInputTimes = 0;
                if (isWaitIFrame){
                    ComBuffer b = bufferedVideoQueue.peek();
                    if(!Util.isAvcKeyFrame(b)){
                        //bufferPool.relBuffer(bufferedVideoQueue.poll());
                        Log.i(DemoApplication.TAG, "run: drop a video when wait IFrame");
                        break;
                    }
                    else {
                        isWaitIFrame = false;
                        Log.i(DemoApplication.TAG, "run: get IFrame now");
                    }
                }

                ByteBuffer[] inputBuffers = videoDecoder.getInputBuffers();
                int inIndex = videoDecoder.dequeueInputBuffer(0);
                if (inIndex >= 0) {

                    ByteBuffer buffer = inputBuffers[inIndex];
                    //ByteBuffer buffer = videoDecoder.getInputBuffer(inIndex);
                    buffer.clear();
                    long ts = 0;

                    ComBuffer b = bufferedVideoQueue.poll();

                    buffer.put(b.buffer, 0, b.getLen());
                    ts = b.getTs();
                    ///bufferPool.relBuffer(b);
                    buffer.flip();
                    ++videoStat.videoFramesQueneToDecodeThisRound;
                    videoStat.videoFramesNoQueneToDecodeRounds = 0;
                    videoDecoder.queueInputBuffer(inIndex, 0, buffer.limit(), ts, 0);
                    decodeVideoAndRender(0);
                }

                else

                {
                    ++videoStat.videoFramesQueneToDecodeThisRoundFailed;
                    decodeVideoAndRender(0);
                    if (MCState.MC_DEAD != state) {
                        videoDecodeHandler.postDelayed(this, 10);
                    }
                    //handler.post(this);
                    return;
                }
            }


            decodeVideoAndRender(0);
            if (MCState.MC_DEAD != state) {
                videoDecodeHandler.postDelayed(this, 10);
            }
        }
    }

    private class ChannelStatTask extends TimerTask
    {
        int count = 0;
        @Override
        public void run() {
            videoStat.videoFramesInBuffer = bufferedVideoQueue.size();

            Log.i("CHANNEL_STAT", "channel id:" + getChannelId() + " " + videoStat.toString()+ " " + audioStat.toString() );

            if(videoStat.videoFramesQueneToDecodeThisRound == 0){
                videoStat.videoFramesNoQueneToDecodeRounds += 1;
            }else{
                videoStat.videoFramesNoQueneToDecodeRounds = 0;
            }

            videoStat.reset();
            audioStat.reset();

        }
    }

    private void resetMediaCodecWrapper(){
        if (videoDecoder != null){
            //Log.i(MainActivity.TAG, "handleMessage MSG_UI_SURFACE_CREATED: start");
            videoDecoder.flush();
            videoDecoder.stop();

            videoDecoder.release();
            videoDecoder = null;

            try {
                videoDecoder = MediaCodec.createDecoderByType("video/avc");
                //videoDecoder.setCallback();
            } catch (IOException e) {
                Log.e(TAG, "createDecoder failed" + e.getMessage());
                return;
            }

            videoDecoder.configure(format, surface, null, 0);
            videoDecoder.start();
            Log.i(DemoApplication.TAG, "reset videoDecoder");
        }
    }

    private class ScreenRenderChannelHandler extends Handler {

        public ScreenRenderChannelHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == CastManager.MSG_UI_SURFACE_CREATED){
                if (videoDecoder != null){
                    Log.i(DemoApplication.TAG, "handleMessage MSG_UI_SURFACE_CREATED: start");
                    videoDecoder.flush();
                    videoDecoder.stop();
                    videoDecoder.release();
                    videoDecoder = null;

                    try {
                        videoDecoder = MediaCodec.createDecoderByType("video/avc");
                        //videoDecoder.setCallback();
                    } catch (IOException e) {
                        Log.e(TAG, "createDecoder failed" + e.getMessage());
                        return;
                    }
                    videoDecoder.configure(format, surface, null, 0);
                    videoDecoder.start();
                    Log.i(DemoApplication.TAG, "handleMessage MSG_UI_SURFACE_CREATED: over");
                }

                setState(MCState.MC_RUN_FRONT);
            }else if (msg.what == CastManager.MSG_UI_SURFACE_DESTROYED){

                if (videoDecoder != null){
                    videoDecoder.flush();
                    videoDecoder.stop();
                    videoDecoder.release();
                    videoDecoder = null;

                    try {
                        videoDecoder = MediaCodec.createDecoderByType("video/avc");
                        //videoDecoder.setCallback();
                    } catch (IOException e) {
                        Log.e(TAG, "createDecoder failed" + e.getMessage());
                        return;
                    }
                    videoDecoder.configure(format, surface, null, 0);
                    videoDecoder.start();
                }

                setState(MCState.MC_RUN_BACK);
            }
        }
    }
    @Override
    public  void  onVideoFrame(byte[] buffer,int len,long ts){
        ComBuffer data = new ComBuffer(buffer,len,ts);
        bufferedVideoQueue.offer(data);
        videoStat.videobitInputThisRound += (buffer.length * 8);
        ++videoStat.videoFramesInputThisRound;
        ++videoStat.totalVideoFramesInput;
    }
}

