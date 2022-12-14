package com.teslasoft.iot.rgbcontroller.nfc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.teslasoft.iot.rgbcontroller.R;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

public class PowerOn extends Activity {

    public RequestNetwork api;

    public RequestNetwork.RequestListener api_listener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(String tag, String response) {
            finishAndRemoveTask();
        }

        @Override
        public void onErrorResponse(String tag, String message) {
            Toast.makeText(PowerOn.this, "Error", Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    };

    public String r;
    public String g;
    public String b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_loading_screen);

        api = new RequestNetwork(this);

        try {
            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            String h = settings.getString("hostname", null);
            String p = settings.getString("port", null);
            String pp = settings.getString("protocol", null);
            String c = settings.getString("cmd", null);
            r = settings.getString("red", null);
            g = settings.getString("green", null);
            b = settings.getString("blue", null);
            api.startRequestNetwork(RequestNetworkController.GET, pp.concat("://").concat(h).concat(":").concat(p).concat(getCmd(c, null, null, null)), "A", api_listener);

            SharedPreferences xsettings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = xsettings.edit();
            editor.putString("enabled", "true");
            editor.apply();
        } catch (Exception ignored) {
            Toast.makeText(this, "Please setup this app first", Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    }

    public String getCmd(String cmd, @Nullable String _r, @Nullable String _g, @Nullable String _b) {
        if (_r == null || _g == null || _b == null) {
            String r_out = cmd.replace("{_r}", r);
            String g_out = r_out.replace("{_g}", g);

            return g_out.replace("{_b}", b);
        } else {
            String r_out = cmd.replace("{_r}", _r);
            String g_out = r_out.replace("{_g}", _g);

            return g_out.replace("{_b}", _b);
        }
    }
}
