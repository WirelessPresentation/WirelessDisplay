package com.bjnet.airplaydemo.event;

import com.bjnet.cbox.module.MediaChannel;

public class AirUrlEvent {
    public MediaChannel mediaChannel;

    public AirUrlEvent(MediaChannel mediaChannel) {
        this.mediaChannel = mediaChannel;
    }
}
