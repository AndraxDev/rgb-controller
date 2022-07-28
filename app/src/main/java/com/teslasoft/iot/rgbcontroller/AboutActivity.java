package com.teslasoft.iot.rgbcontroller;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class AboutActivity extends FragmentActivity {

    public ImageView app_icon;

    public Button btn_used_libs;
    public Button btn_privacy;
    public Button btn_tos;

    public ImageButton activity_back_about;

    public TextView app_version;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        app_icon = findViewById(R.id.app_icon);
        app_icon.setImageResource(R.drawable.app_icon_round);

        btn_used_libs = findViewById(R.id.btn_used_libs);
        btn_privacy = findViewById(R.id.btn_privacy);
        btn_tos = findViewById(R.id.btn_tos);

        activity_back_about = findViewById(R.id.activity_back_about);
        activity_back_about.setImageResource(R.drawable.ic_back);

        app_version = findViewById(R.id.app_version);

        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            app_version.setText(getResources().getString(R.string.text_version).concat(" ").concat(info.versionName));
        } catch (PackageManager.NameNotFoundException ignored) {
            app_version.setText(getResources().getString(R.string.text_version).concat(" ").concat("unknown"));
        }

    }

    public void open_used_libs(View v) {

    }

    public void open_privacy(View v) {

    }

    public void open_tos(View v) {

    }

    public void back(View v) {
        finish();
    }
}
