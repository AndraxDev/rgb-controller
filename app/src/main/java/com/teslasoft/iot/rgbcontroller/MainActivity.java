package com.teslasoft.iot.rgbcontroller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.MultiTransformation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.teslasoft.core.api.auth.TeslasoftIDAuth;
import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import top.defaults.colorpicker.BuildConfig;

public class MainActivity extends FragmentActivity {

    private LinearLayout viewDevices;
    private LinearLayout viewController;
    private LinearLayout viewSettings;
    private BottomNavigationView navigator;
    private ImageView accountIcon;
    private TextView accountName;
    private TextView accountEmail;
    private LinearLayout noSelected;

    private String did = null;
    private DeviceListAdapter adapter;
    private final ArrayList<String> ids = new ArrayList<>();

    private int mode = 0;
    private int selectedTab = 1;
    private boolean isAnimating = false;

    private final RequestNetwork.RequestListener accountListener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(@NonNull String tag, @NonNull String response) {
            try {
                Gson gson = new Gson();
                Type r = TypeToken.getParameterized(UserModel.class).getType();
                UserModel sr = gson.fromJson(response, r);

                accountName.setText(sr.getUser_name());
                accountEmail.setText(sr.getUser_email());
            } catch (Exception e) {
                invalidateSession(Objects.requireNonNull(e.getMessage()).concat(" [load data]"));
            }
        }

        @Override
        public void onErrorResponse(@NonNull String tag, @NonNull String message) {
            invalidateSession(message);
        }
    };

    private final ActivityResultLauncher<Intent> authDataListener = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (mode == 1) {
            if (result.getResultCode() >= 20) {
                try {
                    if (result.getData() != null) {
                        String sig = result.getData().getStringExtra("signature");
                        String uid = result.getData().getStringExtra("account_id");

                        SharedPreferences accountSettings = getSharedPreferences("account", MODE_PRIVATE);
                        SharedPreferences.Editor edit = accountSettings.edit();
                        edit.putString("account_id", uid);
                        edit.putString("signature", sig);
                        edit.apply();

                        loadAccountData();
                    } else {
                        invalidateSession("Data is null");
                    }
                } catch (Exception e) {
                    invalidateSession(Objects.requireNonNull(e.getMessage()).concat(" [return response]"));
                }
            } else if (result.getResultCode() == 3) {
                removeAccountData();
            }
            mode = 0;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        initUI();
        loadAccountData();
        initSettings();
        initNavigator();
        restoreActivity(savedInstanceState);
    }

    private void restoreActivity(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt("tab") == 1) {
                navigator.setSelectedItemId(R.id.page_devices);
                viewDevices.setVisibility(View.VISIBLE);
                viewController.setVisibility(View.GONE);
                viewSettings.setVisibility(View.GONE);
            } else if (savedInstanceState.getInt("tab") == 2) {
                navigator.setSelectedItemId(R.id.page_controller);
                viewDevices.setVisibility(View.GONE);
                viewController.setVisibility(View.VISIBLE);
                viewSettings.setVisibility(View.GONE);
            } else if (savedInstanceState.getInt("tab") == 3) {
                navigator.setSelectedItemId(R.id.page_settings);
                viewDevices.setVisibility(View.GONE);
                viewController.setVisibility(View.GONE);
                viewSettings.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initUI() {
        ImageView appIcon = findViewById(R.id.app_icon);
        TextView appVersion = findViewById(R.id.app_version);
        ImageView appI = findViewById(R.id.app_i);
        ListView listDev = findViewById(R.id.list_dev);

        accountIcon = findViewById(R.id.account_icon);
        accountName = findViewById(R.id.account_name);
        accountEmail = findViewById(R.id.account_email);
        viewDevices = findViewById(R.id.view_devices);
        viewController = findViewById(R.id.view_controller);
        viewSettings = findViewById(R.id.view_settings);
        navigator = findViewById(R.id.bottom_navigation);
        noSelected = findViewById(R.id.no_selected);

        appIcon.setImageResource(R.mipmap.ic_launcher_round);
        accountIcon.setImageResource(R.drawable.ic_account_circle);
        appI.setImageResource(R.mipmap.ic_launcher_round);

        noSelected.setVisibility(View.VISIBLE);
        viewDevices.setVisibility(View.VISIBLE);
        viewController.setVisibility(View.GONE);
        viewSettings.setVisibility(View.GONE);

        appVersion.setText(getResources().getString(R.string.text_version).concat(" ").concat(BuildConfig.VERSION_NAME));

        FragmentManager fragmentManager = getSupportFragmentManager();

        adapter = new DeviceListAdapter(this, fragmentManager, ids);

        listDev.setDividerHeight(0);
        listDev.setAdapter(adapter);
    }

    private void loadAccountData() {
        SharedPreferences accountSettings = getSharedPreferences("account", MODE_PRIVATE);

        String uid = accountSettings.getString("account_id", "");
        String sig = accountSettings.getString("signature", "");

        if (!uid.equals("")) {
            accountName.setText(R.string.text_loading);
            accountEmail.setText("");

            RequestOptions requestOptions = new RequestOptions();
            requestOptions = requestOptions.transform(new MultiTransformation<>(Arrays.asList(new CenterCrop(), new RoundedCorners((int) convertDpToPixel(22, MainActivity.this)))));
            Glide.with(getApplicationContext()).load(Uri.parse("https://id.teslasoft.org/xauth/users/".concat(uid).concat(".png"))).apply(requestOptions).into(accountIcon);

            RequestNetwork ar = new RequestNetwork(this);
            ar.startRequestNetwork(RequestNetworkController.GET, "https://id.teslasoft.org/xauth/GetAccountInfo.php?sig=".concat(sig).concat("&uid=").concat(uid), "A", accountListener);
        }
    }

    private void removeAccountData() {
        try {
            SharedPreferences accountSettings = getSharedPreferences("account", MODE_PRIVATE);
            SharedPreferences.Editor edit = accountSettings.edit();
            edit.remove("account_id");
            edit.remove("signature");
            edit.apply();
        } catch (Exception ignored) { /* unused */ }

        accountIcon.setImageResource(R.drawable.ic_account_circle);
        accountName.setText(R.string.widget_sync_title);
        accountEmail.setText(R.string.widget_sync_description);
    }

    private void invalidateSession(String e) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_manager)
                .setMessage(getString(R.string.auth_failed).concat(" ").concat(e))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();

        removeAccountData();
    }

    private void initSettings() {
        try {
            SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
            did = settings.getString("selected_device", null);
        } catch (Exception e) {
            did = "0";
        }

        int i = 1;

        while (true) {
            SharedPreferences deviceSettings = getSharedPreferences("settings_".concat(Integer.toString(i)), MODE_PRIVATE);

            if (deviceSettings.getString("name", "").equals("")) {
                break;
            }

            if (deviceSettings.getString("hostname", null) != null) {
                ids.add(Integer.toString(i));
            }

            i++;
        }
    }

    private void initNavigator() {
        navigator.setOnItemSelectedListener(item -> {
            if (!isAnimating) {
                if (item.getItemId() == R.id.page_devices) {
                    setSelectedTab(selectedTab, 1);
                    return true;
                } else if (item.getItemId() == R.id.page_controller) {
                    setSelectedTab(selectedTab, 2);
                    /*if (did == null) {
                        noSelected.setVisibility(View.VISIBLE);
                    } else {
                        noSelected.setVisibility(View.GONE);
                    }*/
                    return true;
                } else if (item.getItemId() == R.id.page_settings) {
                    setSelectedTab(selectedTab, 3);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        });
    }

    public void setSelectedDevice(String device) {
        this.did = device;
    }

    public String getSelectedDevice() {
        if (did != null) {
            noSelected.setVisibility(View.GONE);
        } else {
            noSelected.setVisibility(View.VISIBLE);
        }
        return did;
    }

    public void setSelectedNavigatorItem(int item) {
        navigator.setSelectedItemId(item);
    }

    public void removeDevice(int pos) {
        ids.remove(pos);

        if (ids.isEmpty()) {
            did = null;
            noSelected.setVisibility(View.VISIBLE);
        }

        recreate();
    }

    private void setSelectedTab(int from, int to) {
        if (from == 1) {
            if (to == 2) {
                animate(viewDevices, viewSettings, viewDevices, viewController, to);
            } else if (to == 3) {
                animate(viewDevices, viewController, viewDevices, viewSettings, to);
            } else isAnimating = false;
        } else if (from == 2) {
            if (to == 1) {
                animate(viewController, viewSettings, viewController, viewDevices, to);
            } else if (to == 3) {
                animate(viewController, viewDevices, viewController, viewSettings, to);
            } else isAnimating = false;
        } else if (from == 3) {
            if (to == 1) {
                animate(viewSettings, viewController, viewSettings, viewDevices, to);
            } else if (to == 2) {
                animate(viewSettings, viewDevices, viewSettings, viewController, to);
            } else isAnimating = false;
        } else isAnimating = false;
    }

    private void animate(LinearLayout l1, LinearLayout l2, LinearLayout fromTab, LinearLayout toTab, int to) {
        isAnimating = true;
        fromTab.setVisibility(View.VISIBLE);
        fromTab.setAlpha(1);
        fromTab.animate()
                .setDuration(150)
                .alpha(0)
                .setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                transition(l1, l2, toTab, to);
                            }
                        }
                ).start();
    }

    private void transition(LinearLayout l1, LinearLayout l2, LinearLayout toTab, int to) {
        toTab.setVisibility(View.VISIBLE);
        toTab.setAlpha(0);
        toTab.animate()
                .setDuration(150)
                .alpha(1)
                .setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                l1.setVisibility(View.GONE);
                                l2.setVisibility(View.GONE);
                                toTab.setVisibility(View.VISIBLE);
                                isAnimating = false;
                                selectedTab = to;
                            }
                        }
                ).start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("tab", selectedTab);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedTab = savedInstanceState.getInt("tab");
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void addDevice(View v) {
        DeviceFragment deviceFragment = DeviceFragment.newInstance(Integer.toString(getAvailableDeviceId()), "", "", "", "", "", "");
        deviceFragment.show(getSupportFragmentManager().beginTransaction(), "DeviceDialog");

        deviceFragment.setDeviceAddedListener(new DeviceFragment.DeviceAddedListener() {
            @Override
            public void onSuccess() {
                ids.add(Integer.toString(getAvailableDeviceId() - 1));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, R.string.device_add_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getAvailableDeviceId() {
        int e = 1;

        while (true) {
            SharedPreferences settings = getSharedPreferences("settings_".concat(Integer.toString(e)), MODE_PRIVATE);
            if (settings.getString("hostname", "").equals("")) {
                break;
            }

            e++;
        }

        return e;
    }

    public void openUsedLibs(View v) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.title_used_libs)
                .setMessage(HtmlCompat.fromHtml("- Android SDK<br>- AndroidX<br>- com.github.duanhong169.colorpicker<br>- OKHTTPP3<br>- Android Material<br>- AndroidX constraint layout", HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(getResources().getString(R.string.btn_close), (dialog, which) -> {})
                .show();
    }

    public void openPrivacy(View v) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://teslasoft.org/rgb-controller/privacy.html"));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    public void openTos(View v) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://teslasoft.org/rgb-controller/tos.html"));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    public void sync(View v) {
        try {
            mode = 1;
            Intent apiIntent = new Intent(this, TeslasoftIDAuth.class);
            authDataListener.launch(apiIntent);
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.teslasoft_core)
                    .setMessage(R.string.teslasoft_core_error)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();
        }
    }
}