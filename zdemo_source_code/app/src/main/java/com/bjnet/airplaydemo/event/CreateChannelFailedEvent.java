package com.bjnet.airplaydemo.event;

import com.bjnet.cbox.module.UserInfo;

public class CreateChannelFailedEvent {
    private UserInfo userInfo;
    private String reason;

    public CreateChannelFailedEvent(UserInfo info, String reason){
        this.setUserInfo(info);
        this.setReason(reason);
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
