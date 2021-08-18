package com.bjnet.airplaydemo.base;

import java.util.List;

public interface PermissionListener {
    void onGranted();

    void onDenied(List<String> deniedPermission);
}
