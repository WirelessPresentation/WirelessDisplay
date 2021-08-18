package com.bjnet.airplaydemo.event;

import com.bjnet.cbox.module.MediaChannel;

public class AirAudioEvent {
    public MediaChannel mediaChannel;

    public AirAudioEvent(MediaChannel mediaChannel) {
        this.mediaChannel = mediaChannel;
    }
}
