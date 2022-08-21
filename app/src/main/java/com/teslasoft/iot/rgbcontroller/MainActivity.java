package com.teslasoft.iot.rgbcontroller;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity {

    public LinearLayout view_devices;
    public LinearLayout view_controller;
    public LinearLayout view_settings;

    public BottomNavigationView navigator;

    public ListView list_dev;

    public String did = "0";

    public DevicesAdapter adapter;

    public ArrayList<String> ids = new ArrayList<>();

    public LinearLayout no_selected;
    public ImageView app_i;

    public ImageView app_icon;

    public Button btn_used_libs;
    public Button btn_privacy;
    public Button btn_tos;

    public TextView app_version;

    public ColorPickerFragment colorPickerFragment;

    public ImageView account_icon;
    public TextView account_name;
    public TextView account_email;
    public int MODE = 0;
    public int SELECTED_TAB = 1;

    RequestNetwork.RequestListener account_listener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(String tag, String response) {
            try {
                Gson gson = new Gson();
                Type r = new TypeToken<UserModel>() {
                }.getType();
                UserModel sr = gson.fromJson(response, r);

                account_name.setText(sr.getUser_name());
                account_email.setText(sr.getUser_email());
            } catch (Exception e) {
                SharedPreferences account_settings = getSharedPreferences("account", MODE_PRIVATE);
                SharedPreferences.Editor edit = account_settings.edit();
                edit.remove("account_id");
                edit.remove("signature");
                edit.apply();

                account_icon.setImageResource(R.drawable.ic_account_circle);
                account_name.setText("Turn on sync");
                account_email.setText("Sign in with Teslasoft ID");
            }
        }

        @Override
        public void onErrorResponse(String tag, String message) {
            Toast.makeText(MainActivity.this, "Failed to get account info", Toast.LENGTH_SHORT).show();
        }
    };

    public class DevicesAdapter extends BaseAdapter {
        ArrayList<String> _data;
        Activity context;

        public FragmentManager fragmentManager;

        public DevicesAdapter(Activity context, FragmentManager fragmentManager, ArrayList<String> _arr) {
            _data = _arr;
            this.context = context;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public String getItem(int _index) {
            return _data.get(_index);
        }

        @Override
        public long getItemId(int _index) {
            return _index;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(final int _position, View _view, ViewGroup _viewGroup) {
            LayoutInflater _inflater = (LayoutInflater)context.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View _v = _view;

            if (_v == null) {
                _v = _inflater.inflate(R.layout.item_device, null);
            }

            final String protocol;
            final String hostname;
            final String port;
            final String cmd;
            final String dev_name;
            final String device_id;

            final TextView name = _v.findViewById(R.id.name);
            final TextView status = _v.findViewById(R.id.status);
            final Button edit = _v.findViewById(R.id.edit);
            final Button remove = _v.findViewById(R.id.remove);
            final ImageView icon = _v.findViewById(R.id.icon);
            final ConstraintLayout ui = _v.findViewById(R.id.ui);

            icon.setImageResource(R.drawable.app_icon_round);

            device_id = getItem(_position);

            try {
                SharedPreferences settings = context.getSharedPreferences("settings_".concat(device_id), Context.MODE_PRIVATE);

                protocol = settings.getString("protocol", null);
                hostname = settings.getString("hostname", null);
                port = settings.getString("port", null);
                cmd = settings.getString("cmd", null);
                dev_name = settings.getString("name", null);

                name.setText(dev_name);

                initialize(_position, edit, remove, protocol, hostname, port, cmd, dev_name, device_id, name, status, ui);
            } catch (Exception e) {
                ui.setVisibility(View.GONE);
            }

            return _v;
        }

        public void check(String protocol, String hostname, String port, RequestNetwork availability_api, RequestNetwork.RequestListener availability_api_listener) {

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                availability_api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port), "A", availability_api_listener);
                check(protocol, hostname, port, availability_api, availability_api_listener);
            }, 3000);
        }

        public void initialize(int pos, Button edit, Button remove, String protocol, String hostname, String port, String cmd, String dev_name, String device_id, TextView name, TextView status, ConstraintLayout ui) {
            edit.setOnClickListener(v -> {
                DeviceFragment deviceFragment = DeviceFragment.newInstance(device_id, protocol, hostname, port, cmd, dev_name);

                deviceFragment.setDeviceChangedListener(name::setText);

                deviceFragment.show(fragmentManager.beginTransaction(), "DeviceDialog");
            });

            remove.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Confirm")
                        .setMessage("Do you really want to remove this device?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            SharedPreferences settings = context.getSharedPreferences("settings_".concat(device_id), Context.MODE_PRIVATE);
                            SharedPreferences.Editor debug_editor = settings.edit();
                            debug_editor.remove("hostname");
                            debug_editor.remove("port");
                            debug_editor.remove("protocol");
                            debug_editor.remove("cmd");

                            try {
                                SharedPreferences s = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                                if (s.getString("selected_device", null).equals(device_id)) {
                                    SharedPreferences.Editor ed = s.edit();
                                    ed.remove("selected_device");
                                    ed.apply();
                                    did = null;
                                }
                            } catch (Exception ignored) {}

                            debug_editor.apply();
                            ids.remove(pos);
                            notifyDataSetChanged();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {

                        })
                        .show();
            });

            ui.setOnClickListener(v -> {
                colorPickerFragment = ColorPickerFragment.newInstance(Integer.toString(pos + 1));

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.view_controller, colorPickerFragment , "ColorPicker");
                fragmentTransaction.commit();

                navigator.setSelectedItemId(R.id.page_controller);

                did = Integer.toString(pos + 1);

                SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor debug_editor = settings.edit();
                debug_editor.putString("selected_device", did);
                debug_editor.apply();
            });

            final RequestNetwork availability_api = new RequestNetwork(context);

            final RequestNetwork.RequestListener availability_api_listener = new RequestNetwork.RequestListener() {
                @Override
                public void onResponse(String tag, String response) {
                    status.setText("Online");
                    status.setTextColor(getResources().getColor(R.color.success));
                }

                @Override
                public void onErrorResponse(String tag, String message) {
                    status.setText("Offline");
                    status.setTextColor(getResources().getColor(R.color.error));
                }
            };

            check(protocol, hostname, port, availability_api, availability_api_listener);
        }
    }

    public Bundle savedInstanceState;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        app_icon = findViewById(R.id.app_icon);
        app_icon.setImageResource(R.mipmap.ic_launcher_round);

        btn_used_libs = findViewById(R.id.btn_used_libs);
        btn_privacy = findViewById(R.id.btn_privacy);
        btn_tos = findViewById(R.id.btn_tos);
        app_version = findViewById(R.id.app_version);

        account_icon = findViewById(R.id.account_icon);
        account_name = findViewById(R.id.account_name);
        account_email = findViewById(R.id.account_email);
        account_icon.setImageResource(R.drawable.ic_account_circle);

        SharedPreferences account_settings = getSharedPreferences("account", MODE_PRIVATE);

        try {
            String auid = account_settings.getString("account_id", null);
            String sig = account_settings.getString("signature", null);
            if (!auid.equals("")) {
                account_name.setText("Loading...");
                account_email.setText("");

                RequestNetwork ar = new RequestNetwork(this);
                ar.startRequestNetwork(RequestNetworkController.GET, "https://id.teslasoft.org/xauth/GetAccountInfo.php?sig=".concat(sig).concat("&uid=").concat(auid), "A", account_listener);

                RequestOptions requestOptions = new RequestOptions();
                requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners((int) convertDpToPixel(22, MainActivity.this)));
                Glide.with(getApplicationContext()).load(Uri.parse("https://id.teslasoft.org/xauth/users/".concat(auid).concat(".png"))).apply(requestOptions).into(account_icon);
            }
        } catch (Exception ignored) {}

        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            app_version.setText(getResources().getString(R.string.text_version).concat(" ").concat(info.versionName));
        } catch (PackageManager.NameNotFoundException ignored) {
            app_version.setText(getResources().getString(R.string.text_version).concat(" ").concat("unknown"));
        }

        try {
            SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
            did = settings.getString("selected_device", null);
        } catch (Exception e) {
            did = "0";
        }

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

            if (did != null) {
                colorPickerFragment = ColorPickerFragment.newInstance(did);
                fragmentTransaction.replace(R.id.view_controller, colorPickerFragment , "ColorPicker");
            }

            fragmentTransaction.commit();
        }

        int e = 1;

        while (true) {
            try {
                SharedPreferences settings = getSharedPreferences("settings_".concat(Integer.toString(e)), MODE_PRIVATE);
                if (settings.getString("name", null).equals("")) {
                    break;
                }
                if (settings.getString("hostname", null) != null) {
                    ids.add(Integer.toString(e));
                }
                e++;
            } catch (Exception exception) {
                break;
            }
        }

        FragmentManager fragmentManager =  getSupportFragmentManager();

        adapter = new DevicesAdapter(this, fragmentManager, ids);
        list_dev = findViewById(R.id.list_dev);
        list_dev.setDividerHeight(0);
        list_dev.setAdapter(adapter);

        no_selected = findViewById(R.id.no_selected);
        no_selected.setVisibility(View.GONE);

        app_i = findViewById(R.id.app_i);
        app_i.setImageResource(R.mipmap.ic_launcher_round);

        view_devices = findViewById(R.id.view_devices);
        view_controller = findViewById(R.id.view_controller);
        view_settings = findViewById(R.id.view_settings);

        view_devices.setVisibility(View.VISIBLE);
        view_controller.setVisibility(View.GONE);
        view_settings.setVisibility(View.GONE);

        navigator = findViewById(R.id.bottom_navigation);

        navigator.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.page_devices) {
                SELECTED_TAB = 1;
                view_devices.setVisibility(View.VISIBLE);
                view_controller.setVisibility(View.GONE);
                view_settings.setVisibility(View.GONE);
                view_devices.animate().alpha(1.0f).setDuration(200);
                view_controller.animate().alpha(0.0f).setDuration(200);
                view_settings.animate().alpha(0.0f).setDuration(200);
                return true;
            } else if (item.getItemId() == R.id.page_controller) {
                SELECTED_TAB = 2;
                view_devices.setVisibility(View.GONE);
                view_controller.setVisibility(View.VISIBLE);
                view_settings.setVisibility(View.GONE);
                view_devices.animate().alpha(0.0f).setDuration(200);
                view_controller.animate().alpha(1.0f).setDuration(200);
                view_settings.animate().alpha(0.0f).setDuration(200);

                if (did == null) {
                    no_selected.setVisibility(View.VISIBLE);
                } else {
                    no_selected.setVisibility(View.GONE);
                }
                return true;
            } else if (item.getItemId() == R.id.page_settings) {
                SELECTED_TAB = 3;
                view_devices.setVisibility(View.GONE);
                view_controller.setVisibility(View.GONE);
                view_settings.setVisibility(View.VISIBLE);
                view_devices.animate().alpha(0.0f).setDuration(200);
                view_controller.animate().alpha(0.0f).setDuration(200);
                view_settings.animate().alpha(1.0f).setDuration(200);
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.getInt("tab") == 1) {
                navigator.setSelectedItemId(R.id.page_devices);
            } else if (savedInstanceState.getInt("tab") == 2) {
                navigator.setSelectedItemId(R.id.page_controller);
            } else if (savedInstanceState.getInt("tab") == 3) {
                navigator.setSelectedItemId(R.id.page_settings);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("tab", SELECTED_TAB);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        SELECTED_TAB = savedInstanceState.getInt("tab");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MODE == 1) {
            if (resultCode >= 20) {
                try {
                    String sig = data.getStringExtra("signature");
                    String uid = data.getStringExtra("account_id");

                    SharedPreferences account_settings = getSharedPreferences("account", MODE_PRIVATE);
                    SharedPreferences.Editor edit = account_settings.edit();
                    edit.putString("account_id", uid);
                    edit.putString("signature", sig);
                    edit.apply();

                    account_name.setText("Loading...");
                    account_email.setText("");

                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners((int) convertDpToPixel(22, MainActivity.this)));
                    Glide.with(getApplicationContext()).load(Uri.parse("https://id.teslasoft.org/xauth/users/".concat(uid).concat(".png"))).apply(requestOptions).into(account_icon);

                    RequestNetwork ar = new RequestNetwork(this);
                    ar.startRequestNetwork(RequestNetworkController.GET, "https://id.teslasoft.org/xauth/GetAccountInfo.php?sig=".concat(sig).concat("&uid=").concat(uid), "A", account_listener);
                } catch (Exception e) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Account manager")
                            .setMessage("Failed to sign in")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                }
            } else if (resultCode == 3) {
                try {
                    SharedPreferences account_settings = getSharedPreferences("account", MODE_PRIVATE);
                    SharedPreferences.Editor edit = account_settings.edit();
                    edit.remove("account_id");
                    edit.remove("signature");
                    edit.apply();
                } catch (Exception ignored) {
                }

                account_icon.setImageResource(R.drawable.ic_account_circle);
                account_name.setText("Turn on sync");
                account_email.setText("Sign in with Teslasoft ID");
            }
            MODE = 0;
        }
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void add_device(View v) {
        DeviceFragment deviceFragment = DeviceFragment.newInstance(Integer.toString(getAvailableDeviceId()), "", "", "", "", "");
        deviceFragment.show(getSupportFragmentManager().beginTransaction(), "DeviceDialog");

        deviceFragment.setDeviceAddedListener(new DeviceFragment.DeviceAddedListener() {
            @Override
            public void onSuccess() {
                ids.add(Integer.toString(getAvailableDeviceId() - 1));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed() {

            }
        });
    }

    public int getAvailableDeviceId() {
        int e = 1;

        while (true) {
            try {
                SharedPreferences settings = getSharedPreferences("settings_".concat(Integer.toString(e)), MODE_PRIVATE);
                if (settings.getString("hostname", null).equals("")) {
                    break;
                }
                e++;
            } catch (Exception exception) {
                break;
            }
        }

        return e;
    }

    public void open_used_libs(View v) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("Used libs")
                .setMessage(Html.fromHtml("- Android SDK<br>- AndroidX<br>- com.github.duanhong169.colorpicker<br>- OKHTTPP3<br>- Android Material<br>- AndroixX constraintlayout"))
                .setPositiveButton(getResources().getString(R.string.btn_close), (dialog, which) -> {})
                .show();
    }

    public void open_privacy(View v) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://teslasoft.org/rgb-controller/privacy.html"));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    public void open_tos(View v) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://teslasoft.org/rgb-controller/tos.html"));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    public void ignored(View v) {}

    public void sync(View v) {
        try {
            MODE = 1;
            Intent api_intent = new Intent(this, TeslasoftIDAuth.class);
            startActivityForResult(api_intent, 1);
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Teslasoft Core")
                    .setMessage("This feature requires one or more Teslasoft services that are not currently available on your device.")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();
        }
    }
}