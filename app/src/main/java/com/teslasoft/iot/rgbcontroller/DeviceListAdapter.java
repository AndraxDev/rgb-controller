package com.teslasoft.iot.rgbcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import java.util.List;

public class DeviceListAdapter extends BaseAdapter {
    private final List<String> data;
    private final FragmentActivity context;

    private final FragmentManager fragmentManager;
    private ColorPickerFragment colorPickerFragment;

    private String protocol;
    private String hostname;
    private String port;
    private String cmd;
    private String devName;
    private String getter;

    public DeviceListAdapter(FragmentActivity context, FragmentManager fragmentManager, List<String> data) {
        this.data = data;
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String getItem(int index) {
        return data.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        if (context instanceof MainActivity) {
            return getView(view, position, viewGroup);
        } else {
            throw new IllegalStateException("Could not initialize adapter: context must be instance of MainActivity");
        }
    }

    private void check(String protocol, String hostname, String port, RequestNetwork availabilityApi, RequestNetwork.RequestListener availabilityApiListener) {
        final Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {
            availabilityApi.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port), "A", availabilityApiListener);

            check(protocol, hostname, port, availabilityApi, availabilityApiListener);
        }, 3000);
    }

    private View getView(View view, int position, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = view;
        if (v == null) {
            v = inflater.inflate(R.layout.item_device, viewGroup, false);
        }

        final TextView name = v.findViewById(R.id.name);
        final TextView status = v.findViewById(R.id.status);
        final Button edit = v.findViewById(R.id.edit);
        final Button remove = v.findViewById(R.id.remove);
        final ImageView icon = v.findViewById(R.id.icon);
        final ConstraintLayout ui = v.findViewById(R.id.ui);

        icon.setImageResource(R.drawable.app_icon_round);

        String deviceId = getItem(position);

        try {
            SharedPreferences deviceSettings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);

            protocol = deviceSettings.getString("protocol", null);
            hostname = deviceSettings.getString("hostname", null);
            port = deviceSettings.getString("port", null);
            cmd = deviceSettings.getString("cmd", null);
            devName = deviceSettings.getString("name", null);
            getter = deviceSettings.getString("getter", null);

            name.setText(devName);

            initLogic(position, edit, remove, name, status, ui, deviceId);
        } catch (Exception e) {
            ui.setVisibility(View.GONE);
        }

        return v;
    }

    private void initLogic(int pos, Button edit, Button remove, TextView name, TextView status, ConstraintLayout ui, String deviceId) {
        SharedPreferences appSettings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences deviceSettings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);


        edit.setOnClickListener(v -> {
            DeviceFragment deviceFragment = DeviceFragment.newInstance(deviceId, protocol, hostname, port, cmd, devName, getter);

            deviceFragment.setDeviceChangedListener(name::setText);
            deviceFragment.show(fragmentManager.beginTransaction(), "DeviceDialog");
        });

        remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm")
                .setMessage("Do you really want to remove this device?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    SharedPreferences.Editor editor = deviceSettings.edit();

                    editor.remove("hostname");
                    editor.remove("port");
                    editor.remove("protocol");
                    editor.remove("cmd");
                    editor.remove("getter");

                    try {
                        if (appSettings.getString("selected_device", null).equals(deviceId)) {
                            SharedPreferences.Editor ed = appSettings.edit();
                            ed.remove("selected_device");
                            ed.apply();
                            ((MainActivity) context).setSelectedDevice(null);
                        }
                    } catch (Exception ignored) { /* unused */ }

                    editor.apply();

                    ((MainActivity) context).removeDevice(pos);

                    notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> { /* unused */ })
                .show());

        ui.setOnClickListener(v -> {
            colorPickerFragment = ColorPickerFragment.newInstance(deviceId);

            FragmentManager fragmentManager = context.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            fragmentTransaction.replace(R.id.view_controller, colorPickerFragment , "ColorPicker");
            fragmentTransaction.commit();

            ((MainActivity) context).setSelectedDevice(Integer.toString(pos + 1));
            ((MainActivity) context).setSelectedNavigatorItem(R.id.page_controller);

            SharedPreferences.Editor editor = appSettings.edit();
            editor.putString("selected_device", ((MainActivity) context).getSelectedDevice());
            editor.apply();
        });

        final RequestNetwork availabilityApi = new RequestNetwork(context);

        final RequestNetwork.RequestListener availabilityApiListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(@NonNull String tag, @NonNull String response) {
                status.setText("Online");
                status.setTextColor(context.getColor(R.color.success));
            }

            @Override
            public void onErrorResponse(@NonNull String tag, @NonNull String message) {
                status.setText("Offline");
                status.setTextColor(context.getColor(R.color.error));
            }
        };

        check(protocol, hostname, port, availabilityApi, availabilityApiListener);
    }
}