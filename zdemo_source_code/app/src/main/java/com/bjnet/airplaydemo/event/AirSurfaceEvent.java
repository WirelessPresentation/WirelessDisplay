package com.bjnet.airplaydemo.event;

import com.bjnet.cbox.module.MediaChannel;

public class AirSurfaceEvent {

    public MediaChannel mediaChannel;

    public AirSurfaceEvent(MediaChannel mediaChannel) {
        this.mediaChannel = mediaChannel;
    }
}
