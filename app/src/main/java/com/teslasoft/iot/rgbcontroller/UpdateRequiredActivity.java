package com.teslasoft.iot.rgbcontroller;

import android.app.Activity;
import android.os.Bundle;

public class UpdateRequiredActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setResult(3);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Teslasoft Core Updater")
                .setMessage("Could not find Teslasoft Core patch files. Please install Teslasoft Core patch files and reboot your device.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }
}
