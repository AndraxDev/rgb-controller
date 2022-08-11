package com.teslasoft.android.material.switchpreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.teslasoft.iot.rgbcontroller.R;

public class SwitchPreference extends Fragment {

    private TextView mtrl_switch_title;
    private TextView mtrl_switch_description;
    private MaterialSwitch mtrl_switch;
    private ConstraintLayout mtrl_switch_clickable;

    private String switch_title;
    private String switch_description;
    private String switch_key;
    private boolean enabled;
    private boolean is_checked;
    private boolean restore_state;
    public CheckChangedListener listener;

    public SwitchPreference(){
        this.listener = null;
    }

    public static SwitchPreference newInstance(String title, String description, String key, boolean enabled, boolean is_checked, boolean restore_state) {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("description", description);
        args.putString("key", key);
        args.putBoolean("enabled", enabled);
        args.putBoolean("is_checked", is_checked);
        args.putBoolean("restore_state", restore_state);
        SwitchPreference f = new SwitchPreference();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch_title = getArguments().getString("title");
        switch_description = getArguments().getString("description");
        switch_key = getArguments().getString("key");
        this.enabled = getArguments().getBoolean("enabled");
        this.is_checked = getArguments().getBoolean("is_checked");
        this.restore_state = getArguments().getBoolean("restore_state");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.switch_material, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences app_settings = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        mtrl_switch = view.findViewById(R.id.fragment_mtrl_switch);
        mtrl_switch_clickable = view.findViewById(R.id.fragment_mtrl_switch_clickable);
        mtrl_switch_title = view.findViewById(R.id.fragment_mtrl_switch_title);
        mtrl_switch_description = view.findViewById(R.id.fragment_mtrl_switch_description);

        try {
            mtrl_switch.setChecked(app_settings.getBoolean(switch_key, false));

            try {
                SwitchPreference.this.listener.onChecked(app_settings.getBoolean(switch_key, false));
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        mtrl_switch_clickable.setOnClickListener(v -> {
            if (mtrl_switch.isEnabled()) {
                mtrl_switch.setChecked(!mtrl_switch.isChecked());
            }
        });

        mtrl_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences app_settings_editor = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = app_settings_editor.edit();
            editor.putBoolean(switch_key, mtrl_switch.isChecked());
            editor.apply();

            try {
                SwitchPreference.this.listener.onChecked(isChecked);
            } catch (Exception ignored) {}
        });

        if (!restore_state) mtrl_switch.setChecked(is_checked);

        setTitle(switch_title);
        setDescription(switch_description);

        if (!enabled) {
            mtrl_switch.setEnabled(false);
            mtrl_switch_clickable.setClickable(false);
            mtrl_switch_clickable.setEnabled(false);
            mtrl_switch_title.setAlpha((float) 0.5);
            mtrl_switch_description.setAlpha((float) 0.5);
        }
    }

    public void setEnabled(boolean is_enabled) {
        this.enabled = is_enabled;

        try {
            if (is_enabled) {
                mtrl_switch.setEnabled(true);
                mtrl_switch_clickable.setClickable(true);
                mtrl_switch_clickable.setEnabled(true);
                mtrl_switch_title.setAlpha((float) 1);
                mtrl_switch_description.setAlpha((float) 1);
            } else {
                mtrl_switch.setEnabled(false);
                mtrl_switch_clickable.setClickable(false);
                mtrl_switch_clickable.setEnabled(false);
                mtrl_switch_title.setAlpha((float) 0.5);
                mtrl_switch_description.setAlpha((float) 0.5);
            }
        } catch (Exception ignored) {}
    }

    public void setChecked(boolean is_checked) {
        this.is_checked = is_checked;

        mtrl_switch.setChecked(is_checked);
    }

    public boolean isEnabled() {
        return mtrl_switch.isEnabled();
    }

    public boolean isChecked() {
        return mtrl_switch.isChecked();
    }

    private void setTitle(String title) {
        if (title == null) {
            mtrl_switch_title.setVisibility(View.GONE);
        } else {
            if (title.equals("")) {
                mtrl_switch_title.setVisibility(View.GONE);
            } else {
                mtrl_switch_title.setText(title);
            }
        }
    }

    private void setDescription(String description) {
        if (description == null) {
            mtrl_switch_description.setVisibility(View.GONE);
        } else {
            if (description.equals("")) {
                mtrl_switch_description.setVisibility(View.GONE);
            } else {
                mtrl_switch_description.setText(description);
            }
        }
    }

    public boolean getSwitchState() {
        return mtrl_switch.isChecked();
    }

    public interface CheckChangedListener {
        void onChecked(boolean is_checked);
    }

    public void setOnCheckedListener(CheckChangedListener listener) {
        this.listener = listener;
    }
}
