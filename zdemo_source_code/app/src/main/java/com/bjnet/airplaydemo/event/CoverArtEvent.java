package com.bjnet.airplaydemo.event;

public class CoverArtEvent {

    private byte[] buffer;
    private int len;

    public CoverArtEvent(byte[] buffer, int len){
        this.buffer = buffer;
        this.len = len;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getLen() {
        return len;
    }
}
