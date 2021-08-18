package com.bjnet.airplaydemo.imp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.base.MediaConfHelper;
import com.bjnet.airplaydemo.event.CoverArtEvent;
import com.bjnet.airplaydemo.event.TrackInfoEvent;
import com.bjnet.cbox.module.ComBuffer;
import com.bjnet.cbox.module.MediaChannel;
import com.bjnet.cbox.module.MediaChannelInfo;
import com.bjnet.cbox.module.WorkHandler;
import com.bjnet.cbox.module.WorkThread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AirplayAudioChannel extends MediaChannel {
    private static final String TAG = "AIRPLAY";
    private AudioTrack audioPlayer = null;
    private ConcurrentLinkedQueue<ComBuffer> bufferedAudioQueue = new ConcurrentLinkedQueue<ComBuffer>();
    private ReentrantLock audioLock = new ReentrantLock();
    private Condition audioCond = audioLock.newCondition();
    private WorkHandler audioHandler = null;
    private WorkThread audioThread = null;    //音频播放线程+
    private AirplayAudioChannel.AudioRenderWorker audioWorker = null;
    private int[] gapSizeStateArray = new int[AirplayModuleImp.MAX_GAP_STATE_ROUND_NUM];
    private boolean canDropSomeAudio = false;
    private AirplayAudioChannel.AudioPlayerStatInfo audioStat = new AirplayAudioChannel.AudioPlayerStatInfo();
    public AirplayAudioChannel(MediaChannelInfo info) {
        super(info);
    }

    @Override
    public boolean open() {
        int minSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize * 2, AudioTrack.MODE_STREAM);
        audioPlayer.play();

        //另外起一个音频播放线程，这样不会阻塞JNI内部的线程
        audioThread = new WorkThread("mirror_audio_" + getChannelId());
        audioThread.start();
        this.audioHandler = new WorkHandler(audioThread.getLooper());
        audioWorker = new AirplayAudioChannel.AudioRenderWorker();
        audioHandler.post(audioWorker);
        setState(MCState.MC_OPENED);
        return true;
    }

    @Override
    public void close() {
        setState(MCState.MC_DEAD);
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
                Log.d(TAG, "AirplayAudioChannel audioPlayer release "+getChannelId());
            }
        }
//        Handler handler = CastManager.getMgr().getUiHandler();
//        if (handler != null){
//            Message msg = new Message();
//            msg.what = CastManager.MSG_CHANNEL_CLOSED;
//
//            handler.sendMessage(msg);
//        }

        Log.d(TAG, "AirplayAudioChannel close()");
    }

    @Override
    public void onFrame(int frameLen, int w, int h) {

    }

//    @Override
//    public void onAudioFrame(ComBuffer data) {
//        //data.ts = (long) (data.ts * 100.00 / 9.0);
//        bufferedAudioQueue.add(data);
//        ++audioStat.audioFramesInputThisRound;
//        audioStat.audioSizeInThisRound += data.getLen();
//        audioStat.audioSizeInBuffer.addAndGet(data.getLen());
//        ++audioStat.totalAudioFramesInput;
//        try {
//            audioLock.lock();
//            audioCond.signal();
//        } finally {
//            audioLock.unlock();
//        }
//    }

    @Override
    public  void  onAudioFrame(byte[] buffer,int len,long ts){
        //data.ts = (long) (data.ts * 100.00 / 9.0);
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

//    @Override
//    public ComBuffer reqBuffer(int size) {
//        return this.bufferPool.reqBuffer(size);
//    }

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

    @Override
    public void onRefreshTrackInfo(String album, String title, String artist) {
        DemoApplication.APP.getEventBus().post(new TrackInfoEvent(album, title, artist));
    }

    @Override
    public void onRefreshCoverArt(byte[] buffer, int len) {
        DemoApplication.APP.getEventBus().post(new CoverArtEvent(buffer, len));
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
}