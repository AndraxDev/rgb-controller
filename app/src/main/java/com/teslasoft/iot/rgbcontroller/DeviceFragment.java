package com.teslasoft.iot.rgbcontroller;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.teslasoft.android.material.switchpreference.SwitchPreference;

import java.util.Objects;

public class DeviceFragment extends DialogFragment {

    public DeviceFragment.DeviceChangedListener listener;
    public DeviceFragment.DeviceAddedListener alistener;

    public DeviceFragment() {
        this.listener = null;
        this.alistener = null;
    }

    public static DeviceFragment newInstance(String device_id, String protocol, String hostname, String port, String command, String name) {
        DeviceFragment deviceFragment = new DeviceFragment();

        Bundle args = new Bundle();
        args.putString("device_id", device_id);
        args.putString("protocol", protocol);
        args.putString("hostname", hostname);
        args.putString("port", port);
        args.putString("command", command);
        args.putString("name", name);
        deviceFragment.setArguments(args);

        return deviceFragment;
    }

    private Context context;

    public TextInputEditText field_protocol;
    public TextInputEditText field_hostname;
    public TextInputEditText field_port;
    public TextInputEditText field_cmd;
    public TextInputEditText field_name;

    public boolean error_protocol =     true;
    public boolean error_hostname =     true;
    public boolean error_port =         true;
    public boolean error_cmd =          true;
    public boolean error_name =         true;

    public static String TAG = "DeviceDialog";

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

        field_protocol = view.findViewById(R.id.field_protocol);
        field_hostname = view.findViewById(R.id.field_hostname);
        field_port = view.findViewById(R.id.field_port);
        field_cmd = view.findViewById(R.id.field_cmd);
        field_name = view.findViewById(R.id.field_name);

        field_protocol.setText(getArguments().getString("protocol"));
        field_hostname.setText(getArguments().getString("hostname"));
        field_port.setText(getArguments().getString("port"));
        field_cmd.setText(getArguments().getString("command"));
        field_name.setText(getArguments().getString("name"));

        error_protocol = field_protocol.getText().toString().trim().equals("");
        error_hostname = field_hostname.getText().toString().trim().equals("");
        error_port = field_port.getText().toString().trim().equals("");
        error_cmd = field_cmd.getText().toString().trim().equals("");
        error_name = field_name.getText().toString().trim().equals("");

        field_protocol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_protocol = true;
                    field_protocol.setError("Please field this blank!");
                } else {
                    error_protocol = false;
                    field_protocol.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_hostname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_hostname = true;
                    field_hostname.setError("Please field this blank!");
                } else {
                    error_hostname = false;
                    field_hostname.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_port = true;
                    field_port.setError("Please field this blank!");
                } else {
                    try {
                        final int i = Integer.parseInt(s.toString().trim());

                        if (i > 65536 || i < 0) {
                            error_port = true;
                            field_port.setError("Invalid value!");
                        } else {
                            error_port = false;
                            field_port.setError(null);
                        }
                    } catch (Exception e) {
                        error_port = true;
                        field_port.setError("Invalid value!");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_cmd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_cmd = true;
                    field_cmd.setError("Please field this blank!");
                } else {
                    if (s.toString().trim().contains("{_r}")  && s.toString().trim().contains("{_g}") && s.toString().trim().contains("{_b}")) {
                        error_cmd = false;
                        field_cmd.setError(null);
                    } else {
                        error_cmd = true;
                        field_cmd.setError("CMD does not contain required params");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    error_name = true;
                    field_name.setError("Please field this blank!");
                } else {
                    error_name = false;
                    field_name.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        field_cmd.setOnTouchListener((v, event) -> {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(event.getRawX() >= (field_cmd.getRight() - field_cmd.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle(getResources().getString(R.string.title_help))
                            .setMessage(Html.fromHtml(getResources().getString(R.string.text_help)))
                            .setPositiveButton(getResources().getString(R.string.btn_close), (dialog, which) -> {})
                            .show();

                    return true;
                }
            }
            return false;
        });

        builder.setView(view)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> form_validator())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    try {
                        DeviceFragment.this.alistener.onFailed();
                    } catch (Exception ignored) {}
                });

        return builder.create();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void form_validator() {
        if (error_protocol || error_hostname || error_port || error_cmd || error_name) {
            Toast.makeText(context, "Form error", Toast.LENGTH_SHORT).show();
            DeviceFragment deviceFragment = DeviceFragment.newInstance(getArguments().getString("device_id"), Objects.requireNonNull(field_protocol.getText()).toString(), Objects.requireNonNull(field_hostname.getText()).toString(), Objects.requireNonNull(field_port.getText()).toString(), Objects.requireNonNull(field_cmd.getText()).toString(), Objects.requireNonNull(field_name.getText()).toString());
            deviceFragment.show(requireActivity().getSupportFragmentManager().beginTransaction(), "DeviceDialog");
        } else {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            SharedPreferences debug_settings = context.getSharedPreferences("settings_".concat(getArguments().getString("device_id")), Context.MODE_PRIVATE);
            SharedPreferences.Editor debug_editor = debug_settings.edit();
            debug_editor.putString("hostname", field_hostname.getText().toString());
            debug_editor.putString("port", field_port.getText().toString());
            debug_editor.putString("protocol", field_protocol.getText().toString());
            debug_editor.putString("cmd", field_cmd.getText().toString());
            debug_editor.putString("red", "0");
            debug_editor.putString("green", "0");
            debug_editor.putString("blue", "0");
            debug_editor.putString("enabled", "false");
            debug_editor.putString("name", field_name.getText().toString());
            debug_editor.apply();

            try {
                DeviceFragment.this.listener.deviceInfoChanged(field_name.getText().toString());
            } catch (Exception ignored) {}

            try {
                DeviceFragment.this.alistener.onSuccess();
            } catch (Exception ignored) {}
        }
    }

    public interface DeviceChangedListener {
        void deviceInfoChanged(String dev_name);
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
