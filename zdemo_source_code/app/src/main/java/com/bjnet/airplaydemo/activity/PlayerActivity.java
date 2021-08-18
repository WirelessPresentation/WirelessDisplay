package com.bjnet.airplaydemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.R;

public class PlayerActivity extends Activity {

    private Spinner spinner;
    private Resources resources;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        spinner = findViewById(R.id.widi_settings);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.player, R.layout.lan_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.device), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        resources = getResources();
        int config = sharedPreferences.getInt("player", 1);
        switch (config){
            case 0:
                spinner.setSelection(0);
                break;
            case 1:
                spinner.setSelection(1);
                break;
        }
    }

    public void change(View view) {
        int selection = spinner.getSelectedItemPosition();
        if (selection == 0) {
            editor.putInt("player",0);
            CastManager.getMgr().setPlayer(0);
        } else {
            editor.putInt("player",1);
            CastManager.getMgr().setPlayer(1);
        }
        editor.commit();
        onBackPressed();
    }

    public void backPressed(View v) {
        onBackPressed();
    }

}
