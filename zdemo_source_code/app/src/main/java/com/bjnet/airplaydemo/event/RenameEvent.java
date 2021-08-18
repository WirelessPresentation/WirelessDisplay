package com.bjnet.airplaydemo.event;

public class RenameEvent {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RenameEvent(String name) {
        this.name = name;
    }
}
