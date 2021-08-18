package com.bjnet.airplaydemo.base;

import android.util.Log;

import com.bjnet.airplaydemo.DemoApplication;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/9/5.
 */

public class GlSession {

    private ByteBuffer buffer;
    private int width;
    private int height;
    private JetGlSurfaceView view;

    private int bufferSize = 0;
    private boolean first = true;

    public GlSession(int w, int h, JetGlSurfaceView v){
        this.width = w;
        this.height = h;
        this.bufferSize = this.width*this.height*3/2;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
        this.view = v;
        this.view.setBuffer(buffer,this.width,this.height);
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void onFrame(int bufferSize,int videoW,int videoH){
        if (view == null || !view.isReady()){
            Log.i(DemoApplication.TAG, "onFrame:view is notReady ");
            return;
        }

        if (videoW != this.width || this.height != videoH){
            int oldW = this.width;
            int oldh = this.height;
            this.width = videoW;
            this.height = videoH;
            Log.i(DemoApplication.TAG, "onFrame: new w:"+this.width+" new h:"+this.height);
            synchronized (view){
                if ((videoH*videoW*3/2) > this.bufferSize){
                    this.bufferSize = (width*height*3/2);
                    this.buffer = ByteBuffer.allocateDirect(this.bufferSize);
                    //setYuvBuffer(sessionHandle,this.buffer,width*height*3/2);
                    //TODO:impossible to here
                    Log.e(DemoApplication.TAG, "onFrame: should reset buffer");
                }
                this.view.setBuffer(buffer,this.width,this.height);
                return;
            }
        }

        if (first){
            first = false;
            Log.i(DemoApplication.TAG, "onFrame:first render ");
        }
        view.requestRender();
    }
}
