package com.bjnet.airplaydemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.base.DeviceName;
import com.bjnet.airplaydemo.event.RenameEvent;

public class CustomAct extends Activity {

    EditText device;
    DeviceName deviceName;
    String oldName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_device);

        device = findViewById(R.id.customDevice);
        deviceName = new DeviceName(getApplicationContext());
        int deviceNumber = deviceName.getNumber();
        if (deviceName.getDeviceName() != null) {
            oldName = deviceName.getDeviceName();
            device.setText(deviceName.getDeviceName());
        } else {

            device.setText("BJ62_" + String.valueOf(deviceNumber));
        }
    }

    public void modifyName(View view) {
        final String customName = device.getText().toString();
        if (device.getText().toString().length() != 0) {
            if (!oldName.equals(customName)){
                DemoApplication.APP.getEventBus().post(new RenameEvent(customName));
            }
            deviceName.setDeviceName(customName);
            Toast.makeText(this, getString(R.string.d_name_modified), Toast.LENGTH_SHORT).show();

            onBackPressed();
        }
    }

}
