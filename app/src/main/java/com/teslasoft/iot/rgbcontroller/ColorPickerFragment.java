package com.teslasoft.iot.rgbcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Objects;

import top.defaults.colorpicker.ColorPickerView;

public class ColorPickerFragment extends Fragment {

    public static ColorPickerFragment newInstance(String deviceId) {
        Bundle args = new Bundle();

        args.putString("device_id", deviceId);

        ColorPickerFragment colorPickerFragment = new ColorPickerFragment();

        colorPickerFragment.setArguments(args);

        return colorPickerFragment;
    }

    private TextInputEditText fieldRed;
    private TextInputEditText fieldGreen;
    private TextInputEditText fieldBlue;
    private FloatingActionButton btnPower;
    private ConstraintLayout ui;
    private LinearLayout loadingScreen;
    private LinearLayout disabler;
    private ColorPickerView colorPicker;
    private CheckBox animation;

    private String deviceId;
    private String hostname;
    private String port;
    private String protocol;
    private String cmd;
    private String getter;

    private Context context;

    private boolean errorRed =          false;
    private boolean errorGreen =        false;
    private boolean errorBlue =         false;
    private boolean formError =         false;

    private boolean isLoading = false;
    private boolean isAnimating = false;
    private boolean isPoweredOn = true;

    private static final boolean ANIMATOR = true;

    private RequestNetwork api;
    private RequestNetwork syncProvider;

    private final RequestNetwork.RequestListener syncListener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(@NonNull String tag, @NonNull String response) {
            Gson gson = new Gson();

            try {
                Type r = TypeToken.getParameterized(Color.class).getType();
                Color sr = gson.fromJson(response, r);

                String red = sr.getRed();
                String green = sr.getGreen();
                String blue = sr.getBlue();

                fieldRed.setText(red);
                fieldGreen.setText(green);
                fieldBlue.setText(blue);

                SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString("red", red);
                editor.putString("green", green);
                editor.putString("blue", blue);
                editor.apply();
            } catch (Exception e) { /* unused */ }
        }

        @Override
        public void onErrorResponse(@NonNull String tag, @NonNull String message) { /* unused */ }
    };

    private final RequestNetwork.RequestListener apiListener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(@NonNull String tag, @NonNull String response) {
            loadingScreen.setVisibility(View.GONE);

            isLoading = false;
        }

        @Override
        public void onErrorResponse(@NonNull String tag, @NonNull String message) {
            loadingScreen.setVisibility(View.GONE);
            isLoading = false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();

        if (getArguments() != null) {
            deviceId = getArguments().getString("device_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fieldRed = view.findViewById(R.id.field_red);
        fieldGreen = view.findViewById(R.id.field_green);
        fieldBlue = view.findViewById(R.id.field_blue);
        btnPower = view.findViewById(R.id.btn_power);
        TextView activityTitle = view.findViewById(R.id.activity_title);
        colorPicker = view.findViewById(R.id.color_picker);
        animation = view.findViewById(R.id.animation);
        ui = view.findViewById(R.id.ui);
        loadingScreen = view.findViewById(R.id.loading_screen);
        disabler = view.findViewById(R.id.disabler);

        disabler.setOnClickListener(v -> {});

        loadingScreen.setVisibility(View.GONE);

        syncProvider = new RequestNetwork(requireActivity());

        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);

            hostname = settings.getString("hostname", null);
            port = settings.getString("port", null);
            protocol = settings.getString("protocol", null);
            cmd = settings.getString("cmd", null);
            getter = settings.getString("getter", null);
            fieldRed.setText(settings.getString("red", null));
            fieldGreen.setText(settings.getString("green", null));
            fieldBlue.setText(settings.getString("blue", null));
            activityTitle.setText(settings.getString("name", null));

            initialize();
        } catch (Exception ignored) {
            if (ui != null && btnPower != null) {
                btnPower.setVisibility(View.GONE);
                ui.setVisibility(View.GONE);
            }
        }
    }

    public void initialize() {
        if (hostname == null || port == null || protocol == null) {
            btnPower.setVisibility(View.GONE);
            ui.setVisibility(View.GONE);
        } else {
            postInit();
        }
    }

    private void initSettings() {
        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);

            String xr = settings.getString("red", null);
            String xg = settings.getString("green", null);
            String xb = settings.getString("blue", null);

            String color = "FF".concat(String.format("%02X", Integer.parseInt(xr))).concat(String.format("%02X", Integer.parseInt(xg))).concat(String.format("%02X", Integer.parseInt(xb)));

            long c = Long.parseLong(color, 16);
            colorPicker.setInitialColor((int) c);

            syncProvider.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getter), "A", syncListener);
        } catch (Exception e) {
            colorPicker.setInitialColor(0xFF00FFFF);
        }
    }

    public void postInit() {
        initSettings();

        fieldRed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorRed = true;
                    fieldRed.setError(getString(R.string.required_field));
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            errorRed = true;
                            fieldRed.setError(getString(R.string.invalid_value));
                        } else {
                            errorRed = false;
                            fieldRed.setError(null);
                        }
                    } catch (Exception e) {
                        errorRed = true;
                        fieldRed.setError(getString(R.string.invalid_value));
                    }
                }

                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldGreen.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorGreen = true;
                    fieldGreen.setError(getString(R.string.required_field));
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            errorGreen = true;
                            fieldGreen.setError(getString(R.string.invalid_value));
                        } else {
                            errorGreen = false;
                            fieldGreen.setError(null);
                        }
                    } catch (Exception e) {
                        errorGreen = true;
                        fieldGreen.setError(getString(R.string.invalid_value));
                    }
                }

                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldBlue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorBlue = true;
                    fieldBlue.setError(getString(R.string.required_field));
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 255 || i < 0) {
                            errorBlue = true;
                            fieldBlue.setError(getString(R.string.invalid_value));
                        } else {
                            errorBlue = false;
                            fieldBlue.setError(null);
                        }
                    } catch (Exception e) {
                        errorBlue = true;
                        fieldBlue.setError(getString(R.string.invalid_value));
                    }
                }

                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        initLogic();
    }

    private void initLogic() {
        colorPicker.subscribe((color, fromUser, shouldPropagate) -> {
            if (!isLoading && !formError && !isAnimating && fromUser) {
                String r = Integer.toHexString(color).substring(2, 4);
                String g = Integer.toHexString(color).substring(4, 6);
                String b = Integer.toHexString(color).substring(6, 8);

                int rr = Integer.valueOf(r, 16);
                int gg = Integer.valueOf(g, 16);
                int bb = Integer.valueOf(b, 16);

                fieldRed.setText(String.format(Locale.US,"%d", rr));
                fieldGreen.setText(String.format(Locale.US,"%d", gg));
                fieldBlue.setText(String.format(Locale.US,"%d", bb));

                SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("red", Objects.requireNonNull(fieldRed.getText()).toString().trim());
                editor.putString("green", Objects.requireNonNull(fieldGreen.getText()).toString().trim());
                editor.putString("blue", Objects.requireNonNull(fieldBlue.getText()).toString().trim());
                editor.apply();

                if (isPoweredOn) {
                    api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", apiListener);
                    isLoading = true;
                }
            }
        });

        animation.setOnCheckedChangeListener((buttonView, isChecked) -> isAnimating = isChecked);

        animate(0, 1024, 512);

        api = new RequestNetwork(this.requireActivity());

        try {
            SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
            if (settings.getString("enabled", null).equals("true")) {
                enableLeds();
            } else {
                disableLeds();
            }
        } catch (Exception e) {
            enableLeds();
        }

        btnPower.setOnClickListener(this::toggle);
    }

    public void validateForm() {
        if (!isAnimating) {
            if (errorRed || errorGreen || errorBlue) {
                formError = true;
            } else {
                try {
                    formError = false;
                    String color = "FF".concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldRed.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldGreen.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldBlue.getText()).toString().trim())));

                    long c = Long.parseLong(color, 16);
                    colorPicker.setInitialColor((int) c);
                    api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", apiListener);
                    isLoading = true;
                } catch (Exception ignored) {}
            }
        }
    }

    public void animate(int r, int g, int b) {
        if (r >= 1536) r = 0;
        if (g >= 1536) g = 0;
        if (b >= 1536) b = 0;

        int tR = r;
        int tG = g;
        int tB = b;

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            animate(tR + 4, tG + 4, tB + 4);
            String rR = Integer.toString(intToColor(tR));
            String gG = Integer.toString(intToColor(tG));
            String bB = Integer.toString(intToColor(tB));

            if (isAnimating && ANIMATOR && isPoweredOn) {
                fieldRed.setText(rR);
                fieldGreen.setText(gG);
                fieldBlue.setText(bB);

                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port.concat(getCmd(cmd, null, null, null))), "A", apiListener);
                isLoading = true;
                String color = "FF".concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldRed.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldGreen.getText()).toString().trim()))).concat(String.format("%02X", Integer.parseInt(Objects.requireNonNull(fieldBlue.getText()).toString().trim())));
                long xc = Long.parseLong(color, 16);
                colorPicker.setInitialColor((int) xc);

                SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString("red", fieldRed.getText().toString().trim());
                editor.putString("green", fieldGreen.getText().toString().trim());
                editor.putString("blue", fieldBlue.getText().toString().trim());
                editor.apply();
            }
        }, 50);
    }

    public String getCmd(String cmd, @Nullable String r, @Nullable String g, @Nullable String b) {
        if (r == null || g == null || b == null) {
            String rR = Objects.requireNonNull(fieldRed.getText()).toString().trim();
            String gG = Objects.requireNonNull(fieldGreen.getText()).toString().trim();
            String bB = Objects.requireNonNull(fieldBlue.getText()).toString().trim();

            String rOut = cmd.replace("{_r}", rR);
            String gOut = rOut.replace("{_g}", gG);

            return gOut.replace("{_b}", bB);
        } else {
            String rOut = cmd.replace("{_r}", r);
            String gOut = rOut.replace("{_g}", g);

            return gOut.replace("{_b}", b);
        }
    }

    public int intToColor(int i) {
        if (i >= 0 && i <= 255) return i;
        if (i >= 256 && i <= 767) return 255;
        if (i >= 768 && i <= 1023) return 768 - Math.abs(255 - i);
        else return 0;
    }

    public void toggle(View v) {
        if (isPoweredOn) {
            disableLeds();

            if (!formError) {
                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, "0", "0", "0")), "A", apiListener);
                isLoading = true;
            }

            SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "false");
            editor.apply();
        } else {
            enableLeds();

            if (!formError) {
                api.startRequestNetwork(RequestNetworkController.GET, protocol.concat("://").concat(hostname).concat(":").concat(port).concat(getCmd(cmd, null, null, null)), "A", apiListener);
                isLoading = true;
            }

            SharedPreferences settings = context.getSharedPreferences("settings_".concat(deviceId), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "true");
            editor.apply();
        }
    }

    public void enableLeds() {
        isPoweredOn = true;
        btnPower.setBackgroundTintList(AppCompatResources.getColorStateList(context, R.color.light_green));

        fieldRed.setEnabled(true);
        fieldGreen.setEnabled(true);
        fieldBlue.setEnabled(true);

        animation.setEnabled(true);

        colorPicker.setEnabled(true);

        disabler.setVisibility(View.GONE);
    }

    public void disableLeds() {
        isPoweredOn = false;
        btnPower.setBackgroundTintList(AppCompatResources.getColorStateList(context, R.color.light_red));

        fieldRed.setEnabled(false);
        fieldGreen.setEnabled(false);
        fieldBlue.setEnabled(false);

        animation.setEnabled(false);

        colorPicker.setEnabled(false);

        disabler.setVisibility(View.VISIBLE);
    }
}
