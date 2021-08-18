package com.bjnet.airplaydemo.event;

public class TrackInfoEvent {

    private String album, title, artist;

    public TrackInfoEvent(String album, String title, String artist){
        this.album = album;
        this.title = title;
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}
