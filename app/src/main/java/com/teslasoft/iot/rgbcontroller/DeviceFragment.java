package com.teslasoft.iot.rgbcontroller;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class DeviceFragment extends DialogFragment {

    private DeviceFragment.DeviceChangedListener listener;
    private DeviceFragment.DeviceAddedListener alistener;

    public DeviceFragment() {
        this.listener = null;
        this.alistener = null;
    }

    public static DeviceFragment newInstance(String deviceId, String protocol, String hostname, String port, String command, String name, String getter) {
        DeviceFragment deviceFragment = new DeviceFragment();

        Bundle args = new Bundle();
        args.putString("device_id", deviceId);
        args.putString("protocol", protocol);
        args.putString("hostname", hostname);
        args.putString("port", port);
        args.putString("command", command);
        args.putString("name", name);
        args.putString("getter", getter);
        deviceFragment.setArguments(args);

        return deviceFragment;
    }

    private Context context;

    private TextInputEditText fieldProtocol;
    private TextInputEditText fieldHostname;
    private TextInputEditText fieldPort;
    private TextInputEditText fieldCmd;
    private TextInputEditText fieldName;
    private TextInputEditText fieldGetter;

    private boolean errorProtocol =     true;
    private boolean errorHostname =     true;
    private boolean errorPort =         true;
    private boolean errorCmd =          true;
    private boolean errorName =         true;

    AlertDialog.Builder builder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_info, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        builder = new MaterialAlertDialogBuilder(context);

        View view = this.getLayoutInflater().inflate(R.layout.fragment_device_info, null);

        initUI(view);

        fieldProtocol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorProtocol = true;
                    fieldProtocol.setError(getString(R.string.required_field));
                } else {
                    errorProtocol = false;
                    fieldProtocol.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldHostname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorHostname = true;
                    fieldHostname.setError(getString(R.string.required_field));
                } else {
                    errorHostname = false;
                    fieldHostname.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorPort = true;
                    fieldPort.setError(getString(R.string.required_field));
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 65536 || i < 0) {
                            errorPort = true;
                            fieldPort.setError(getString(R.string.invalid_value));
                        } else {
                            errorPort = false;
                            fieldPort.setError(null);
                        }
                    } catch (Exception e) {
                        errorPort = true;
                        fieldPort.setError(getString(R.string.invalid_value));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldCmd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorCmd = true;
                    fieldCmd.setError(getString(R.string.required_field));
                } else {
                    if (s.toString().trim().contains("{_r}")  && s.toString().trim().contains("{_g}") && s.toString().trim().contains("{_b}")) {
                        errorCmd = false;
                        fieldCmd.setError(null);
                    } else {
                        errorCmd = true;
                        fieldCmd.setError(getString(R.string.cmd_missing_params));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    errorName = true;
                    fieldName.setError(getString(R.string.required_field));
                } else {
                    errorName = false;
                    fieldName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        });

        fieldCmd.setOnTouchListener((v, event) -> {
            v.performClick();

            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= (fieldCmd.getRight() - fieldCmd.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(getResources().getString(R.string.title_help))
                        .setMessage(HtmlCompat.fromHtml(getResources().getString(R.string.text_help), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(getResources().getString(R.string.btn_close), (dialog, which) -> {})
                        .show();

                return true;
            }

            return false;
        });

        builder.setView(view)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> validateForm())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    try {
                        DeviceFragment.this.alistener.onFailed();
                    } catch (Exception ignored) { /* unused */ }
                });

        return builder.create();
    }

    private void initUI(View view) {
        fieldProtocol = view.findViewById(R.id.field_protocol);
        fieldHostname = view.findViewById(R.id.field_hostname);
        fieldPort = view.findViewById(R.id.field_port);
        fieldCmd = view.findViewById(R.id.field_cmd);
        fieldName = view.findViewById(R.id.field_name);
        fieldGetter = view.findViewById(R.id.field_getter);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);

        if (getArguments() != null) {
            if (getArguments().getString("hostname").equals("")) {
                dialogTitle.setText(R.string.text_add_device);
            } else {
                dialogTitle.setText(R.string.text_edit_device_info);
            }
        } else {
            dialogTitle.setText(R.string.text_add_device);
        }

        SharedPreferences settings = context.getSharedPreferences("settings_" + getArguments().getString("device_id"), Context.MODE_PRIVATE);

        fieldProtocol.setText(settings.getString("protocol", ""));
        fieldHostname.setText(settings.getString("hostname", ""));
        fieldPort.setText(settings.getString("port", ""));
        fieldCmd.setText(settings.getString("cmd", ""));
        fieldName.setText(settings.getString("name", ""));
        fieldGetter.setText(settings.getString("getter", ""));

        errorProtocol = Objects.requireNonNull(fieldProtocol.getText()).toString().trim().equals("");
        errorHostname = Objects.requireNonNull(fieldHostname.getText()).toString().trim().equals("");
        errorPort = Objects.requireNonNull(fieldPort.getText()).toString().trim().equals("");
        errorCmd = Objects.requireNonNull(fieldCmd.getText()).toString().trim().equals("");
        errorName = Objects.requireNonNull(fieldName.getText()).toString().trim().equals("");
    }

    public void validateForm() {
        if (getArguments() != null) {
            if (errorProtocol || errorHostname || errorPort || errorCmd || errorName) {
                Toast.makeText(context, "Form error", Toast.LENGTH_SHORT).show();
                DeviceFragment deviceFragment = DeviceFragment.newInstance(getArguments().getString("device_id"), Objects.requireNonNull(fieldProtocol.getText()).toString(), Objects.requireNonNull(fieldHostname.getText()).toString(), Objects.requireNonNull(fieldPort.getText()).toString(), Objects.requireNonNull(fieldCmd.getText()).toString(), Objects.requireNonNull(fieldName.getText()).toString(), Objects.requireNonNull(fieldGetter.getText()).toString());
                deviceFragment.show(requireActivity().getSupportFragmentManager().beginTransaction(), "DeviceDialog");
            } else {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();

                SharedPreferences settings = context.getSharedPreferences("settings_".concat(Objects.requireNonNull(requireArguments().getString("device_id"))), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString("hostname", Objects.requireNonNull(fieldHostname.getText()).toString());
                editor.putString("port", Objects.requireNonNull(fieldPort.getText()).toString());
                editor.putString("protocol", Objects.requireNonNull(fieldProtocol.getText()).toString());
                editor.putString("cmd", Objects.requireNonNull(fieldCmd.getText()).toString());
                editor.putString("getter", Objects.requireNonNull(fieldGetter.getText()).toString());
                editor.putString("red", "0");
                editor.putString("green", "0");
                editor.putString("blue", "0");
                editor.putString("enabled", "false");
                editor.putString("name", Objects.requireNonNull(fieldName.getText()).toString());
                editor.apply();

                try {
                    DeviceFragment.this.listener.deviceInfoChanged(fieldName.getText().toString());
                } catch (Exception ignored) { /* unused */ }

                try {
                    DeviceFragment.this.alistener.onSuccess();
                } catch (Exception ignored) { /* unused */ }
            }
        } else {
            try {
                DeviceFragment.this.alistener.onFailed();
            } catch (Exception ignored) { /* unused */ }
        }
    }

    public interface DeviceChangedListener {
        void deviceInfoChanged(String devName);
    }

    public interface DeviceAddedListener {
        void onSuccess();
        void onFailed();
    }

    public void setDeviceAddedListener(DeviceFragment.DeviceAddedListener listener) {
        this.alistener = listener;
    }

    public void setDeviceChangedListener(DeviceFragment.DeviceChangedListener listener) {
        this.listener = listener;
    }
}
