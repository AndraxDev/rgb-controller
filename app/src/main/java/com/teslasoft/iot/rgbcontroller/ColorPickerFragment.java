package com.teslasoft.iot.rgbcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.teslasoft.android.material.switchpreference.SwitchPreference;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import java.util.Objects;

import top.defaults.colorpicker.ColorPickerView;

public class ColorPickerFragment extends Fragment {
    private String DEVICE_ID;

    private String hostname;
    private String port;
    private String protocol;
    private String cmd;

    private TextView activity_title;

    private ConstraintLayout ui;

    public static ColorPickerFragment newInstance(String device_id) {
        Bundle args = new Bundle();
        args.putString("device_id", device_id);
        ColorPickerFragment f = new ColorPickerFragment();
        f.setArguments(args);
        return f;
    }

    private Context context;

    public TextInputEditText field_red;
    public TextInputEditText field_green;
    public TextInputEditText field_blue;

    public FloatingActionButton btn_power;

    public boolean error_red =          false;
    public boolean error_green =        false;
    public boolean error_blue =         false;
    public boolean form_error =         false;

    public LinearLayout loading_screen;
    public LinearLayout disabler;

    public ColorPickerView color_picker;

    public boolean is_loading = false;

    public boolean is_animating = false;

    public boolean animator = true;

    public boolean is_powered_on = true;

    public CheckBox animation;

    public RequestNetwork api;
    public RequestNetwork.RequestListener api_listener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(String tag, String response) {
            loading_screen.setVisibility(View.GONE);

            is_loading = false;
        }

        @Override
        public void onErrorResponse(String tag, String message) {
            loading_screen.setVisibility(View.GONE);

            Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show();
            is_loading = false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();
        DEVICE_ID = getArguments().getString("device_id");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        field_red = view.findViewById(R.id.field_red);
        field_green = view.findViewById(R.id.field_green);
        field_blue = view.findViewById(R.id.field_blue);
        btn_power = view.findViewById(R.id.btn_power);
        activity_title = view.findViewById(R.id.activity_title);

        ui = view.findViewById(R.id.ui);

        loading_screen = view.findViewById(R.id.loading_screen);
        disabler = view.findViewById(R.id.disabler);

        loading_screen.setVisibility(View.GONE);

        color_picker = view.findViewById(R.id.color_picker);
        animation = view.findViewById(R.id.animation);

        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
            hostname = settings.getString("hostname", null);
            port = settings.getString("port", null);
            protocol = settings.getString("protocol", null);
            cmd = settings.getString("cmd", null);
            field_red.setText(settings.getString("red", null));
            field_green.setText(settings.getString("green", null));
            field_blue.setText(settings.getString("blue", null));
            activity_title.setText(settings.getString("name", null));
            initialize();
        } catch (Exception ignored) {
            btn_power.setVisibility(View.GONE);
            ui.setVisibility(View.GONE);
        }
    }

    public void initialize() {
        if (hostname == null || port == null || protocol == null) {
            btn_power.setVisibility(View.GONE);
            ui.setVisibility(View.GONE);
        } else {
            post_initialize();
        }
    }

    public void post_initialize() {
        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);

            String xr = settings.getString("red", null);
            String xg = settings.getString("green", null);
            String xb = settings.getString("blue", null);

            String color = "FF".concat(String.format("%02X", Integer.parseInt(xr))).concat(String.format("%02X", Integer.parseInt(xg))).concat(String.format("%02X", Integer.parseInt(xb)));

            long c = Long.parseLong(color, 16);
            color_picker.setInitialColor((int) c);
        } catch (Exception e) {
            color_picker.setInitialColor(0xFF00FFFF);
        }

        field_red.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_red = true;
                    field_red.setError("Please field this blank!");
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            error_red = true;
                            field_red.setError("Invalid value!");
                        } else {
                            error_red = false;
                            field_red.setError(null);
                        }
                    } catch (Exception e) {
                        error_red = true;
                        field_red.setError("Invalid value!");
                    }
                }

                form_validator();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_green.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_green = true;
                    field_green.setError("Please field this blank!");
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            error_green = true;
                            field_green.setError("Invalid value!");
                        } else {
                            error_green = false;
                            field_green.setError(null);
                        }
                    } catch (Exception e) {
                        error_green = true;
                        field_green.setError("Invalid value!");
                    }
                }

                form_validator();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_blue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_blue = true;
                    field_blue.setError("Please field this blank!");
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            error_blue = true;
                            field_blue.setError("Invalid value!");
                        } else {
                            error_blue = false;
                            field_blue.setError(null);
                        }
                    } catch (Exception e) {
                        error_blue = true;
                        field_blue.setError("Invalid value!");
                    }
                }

                form_validator();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        color_picker.subscribe((color, fromUser, shouldPropagate) -> {
            if (!is_loading && !form_error && !is_animating && fromUser) {
                String r = Integer.toHexString(color).substring(2, 4);
                String g = Integer.toHexString(color).substring(4, 6);
                String b = Integer.toHexString(color).substring(6, 8);

                int rr = Integer.valueOf(r, 16);
                int gg = Integer.valueOf(g, 16);
                int bb = Integer.valueOf(b, 16);

                field_red.setText(Integer.toString(rr));
                field_green.setText(Integer.toString(gg));
                field_blue.setText(Integer.toString(bb));

                SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("red", Objects.requireNonNull(field_red.getText()).toString().trim());
                editor.putString("green", Objects.requireNonNull(field_green.getText()).toString().trim());
                editor.putString("blue", Objects.requireNonNull(field_blue.getText()).toString().trim());
                editor.apply();

                if (is_powered_on) {
                    api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", api_listener);
                    is_loading = true;
                }
            }
        });

        animation.setOnCheckedChangeListener((buttonView, isChecked) -> is_animating = isChecked);

        animate(0, 1024, 512);

        api = new RequestNetwork(this.getActivity());

        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
            if (settings.getString("enabled", null).equals("true")) {
                enable_leds();
            } else {
                disable_leds();
            }
        } catch (Exception e) {
            enable_leds();
        }

        btn_power.setOnClickListener(this::toggle);
    }

    public void form_validator() {
        if (!is_animating) {
            if (error_red || error_green || error_blue) {
                form_error = true;
            } else {
                try {
                    form_error = false;
                    String color = "FF".concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_red.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_green.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_blue.getText()).toString().trim())));

                    long c = Long.parseLong(color, 16);
                    color_picker.setInitialColor((int) c);
                    api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", api_listener);
                    is_loading = true;
                } catch (Exception ignored) {}
            }
        }
    }

    public void animate(int a, int b, int c) {
        if (a >= 1536) a = 0;
        if (b >= 1536) b = 0;
        if (c >= 1536) c = 0;

        int finalA = a;
        int finalB = b;
        int finalC = c;

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            animate(finalA + 4, finalB + 4, finalC + 4);
            String _r = Integer.toString(intToColor(finalA));
            String _g = Integer.toString(intToColor(finalB));
            String _b = Integer.toString(intToColor(finalC));
            if (is_animating && animator && is_powered_on) {
                field_red.setText(_r);
                field_green.setText(_g);
                field_blue.setText(_b);
                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port.concat(getCmd(cmd, null, null, null))), "A", api_listener);
                is_loading = true;
                String color = "FF".concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_red.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_green.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(field_blue.getText()).toString().trim())));
                long xc = Long.parseLong(color, 16);
                color_picker.setInitialColor((int) xc);
                SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("red", field_red.getText().toString().trim());
                editor.putString("green", field_green.getText().toString().trim());
                editor.putString("blue", field_blue.getText().toString().trim());
                editor.apply();
            }
        }, 50);
    }

    public String getCmd(String cmd, @Nullable String _r, @Nullable String _g, @Nullable String _b) {
        if (_r == null || _g == null || _b == null) {
            String r = Objects.requireNonNull(field_red.getText()).toString().trim();
            String g = Objects.requireNonNull(field_green.getText()).toString().trim();
            String b = Objects.requireNonNull(field_blue.getText()).toString().trim();

            String r_out = cmd.replace("{_r}", r);
            String g_out = r_out.replace("{_g}", g);

            return g_out.replace("{_b}", b);
        } else {
            String r_out = cmd.replace("{_r}", _r);
            String g_out = r_out.replace("{_g}", _g);

            return g_out.replace("{_b}", _b);
        }
    }

    public int intToColor(int i) {
        if (i >= 0 && i <= 255) return i;
        if (i >= 256 && i <= 767) return 255;
        if (i >= 768 && i <= 1023) return 768 - Math.abs(255 - i);
        else return 0;
    }

    public void toggle(View v) {
        if (is_powered_on) {
            disable_leds();

            if (!form_error) {
                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, "0", "0", "0")), "A", api_listener);
                is_loading = true;
            }

            SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "false");
            editor.apply();
        } else {
            enable_leds();

            if (!form_error) {
                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", api_listener);
                is_loading = true;
            }

            SharedPreferences settings = context.getSharedPreferences("settings_".concat(DEVICE_ID), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "true");
            editor.apply();
        }
    }

    public void enable_leds() {
        is_powered_on = true;
        btn_power.setBackgroundTintList(AppCompatResources.getColorStateList(context, R.color.light_green));

        field_red.setEnabled(true);
        field_green.setEnabled(true);
        field_blue.setEnabled(true);

        animation.setEnabled(true);

        color_picker.setEnabled(true);

        disabler.setVisibility(View.GONE);
    }

    public void disable_leds() {
        is_powered_on = false;
        btn_power.setBackgroundTintList(AppCompatResources.getColorStateList(context, R.color.light_red));

        field_red.setEnabled(false);
        field_green.setEnabled(false);
        field_blue.setEnabled(false);

        animation.setEnabled(false);

        color_picker.setEnabled(false);

        disabler.setVisibility(View.VISIBLE);
    }

    public void ignored(View v) {}
}
