package com.bjnet.airplaydemo.event;

import com.bjnet.cbox.module.MediaChannel;

public class AirplayEvent {
    public MediaChannel mediaChannel;

    public AirplayEvent(MediaChannel mediaChannel) {
        this.mediaChannel = mediaChannel;
    }
}
