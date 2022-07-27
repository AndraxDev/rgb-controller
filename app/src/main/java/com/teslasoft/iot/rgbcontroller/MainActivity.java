package com.teslasoft.iot.rgbcontroller;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.teslasoft.core.api.network.RequestNetwork;
import org.teslasoft.core.api.network.RequestNetworkController;

import top.defaults.colorpicker.ColorPickerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends FragmentActivity {

    public TextInputEditText field_protocol;
    public TextInputEditText field_hostname;
    public TextInputEditText field_port;
    public TextInputEditText field_cmd;
    public TextInputEditText field_red;
    public TextInputEditText field_green;
    public TextInputEditText field_blue;

    public boolean error_protocol =     false;
    public boolean error_hostname =     false;
    public boolean error_port =         false;
    public boolean error_cmd =          false;
    public boolean error_red =          false;
    public boolean error_green =        false;
    public boolean error_blue =         false;
    public boolean form_error =         false;

    public Button btn_pick_color;
    public Button btn_set;

    public FloatingActionButton btn_power;

    public TextView activity_title;

    public LinearLayout loading_screen;
    public LinearLayout disabler;

    public ColorPickerView color_picker;

    public boolean is_loading = false;

    public boolean is_animating = false;

    public boolean animator = false;

    public boolean is_powered_on = true;

    public CheckBox animation;

    public TextView debug;

    public RequestNetwork api;
    public RequestNetwork.RequestListener api_listener = new RequestNetwork.RequestListener() {
        @Override
        public void onResponse(String tag, String response) {
            loading_screen.setVisibility(View.GONE);
            if (!is_animating && is_powered_on) btn_set.setEnabled(true);

            is_loading = false;
        }

        @Override
        public void onErrorResponse(String tag, String message) {
            loading_screen.setVisibility(View.GONE);
            if (!is_animating && is_powered_on) btn_set.setEnabled(true);

            Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
            is_loading = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        field_protocol = findViewById(R.id.field_protocol);
        field_hostname = findViewById(R.id.field_hostname);
        field_port = findViewById(R.id.field_port);
        field_cmd = findViewById(R.id.field_cmd);
        field_red = findViewById(R.id.field_red);
        field_green = findViewById(R.id.field_green);
        field_blue = findViewById(R.id.field_blue);

        try {
            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            field_hostname.setText(settings.getString("hostname", null));
            field_port.setText(settings.getString("port", null));
            field_protocol.setText(settings.getString("protocol", null));
            field_cmd.setText(settings.getString("cmd", null));
            field_red.setText(settings.getString("red", null));
            field_green.setText(settings.getString("green", null));
            field_blue.setText(settings.getString("blue", null));
        } catch (Exception ignored) {}

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

                form_validator();
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

                form_validator();
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

                form_validator();
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

                form_validator();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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

        field_cmd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (field_cmd.getRight() - field_cmd.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(getResources().getString(R.string.title_help))
                                .setMessage(Html.fromHtml(getResources().getString(R.string.text_help)))
                                .setPositiveButton(getResources().getString(R.string.btn_close), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();

                        return true;
                    }
                }
                return false;
            }
        });

        btn_pick_color = findViewById(R.id.btn_pick_color);
        btn_set = findViewById(R.id.btn_set);
        btn_power = findViewById(R.id.btn_power);

        activity_title = findViewById(R.id.activity_title);
        debug = findViewById(R.id.debug);

        loading_screen = findViewById(R.id.loading_screen);
        disabler = findViewById(R.id.disabler);

        loading_screen.setVisibility(View.GONE);

        color_picker = findViewById(R.id.color_picker);

        try {
            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);

            String xr = settings.getString("red", null);
            String xg = settings.getString("green", null);
            String xb = settings.getString("blue", null);

            String color = "FF".concat(String.format("%02X", Integer.parseInt(xr))).concat(String.format("%02X", Integer.parseInt(xg))).concat(String.format("%02X", Integer.parseInt(xb)));

            long c = Long.parseLong(color, 16);
            color_picker.setInitialColor((int) c);
        } catch (Exception e) {
            color_picker.setInitialColor(0xFF00FFFF);
        }

        color_picker.subscribe((color, fromUser) -> {
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

                btn_set.setEnabled(false);

                SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("hostname", field_hostname.getText().toString().trim());
                editor.putString("port", field_port.getText().toString().trim());
                editor.putString("protocol", field_protocol.getText().toString().trim());
                editor.putString("cmd", field_cmd.getText().toString().trim());
                editor.putString("red", field_red.getText().toString().trim());
                editor.putString("green", field_green.getText().toString().trim());
                editor.putString("blue", field_blue.getText().toString().trim());
                editor.apply();

                if (is_powered_on) {
                    api.startRequestNetwork(RequestNetworkController.GET, field_protocol.getText().toString().trim().concat("://").concat(field_hostname.getText().toString().trim()).concat(":").concat(field_port.getText().toString().trim()).concat(getCmd(field_cmd.getText().toString().trim(), null, null, null)), "A", api_listener);
                    is_loading = true;
                }
            }
        });

        animation = findViewById(R.id.animation);

        animation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            is_animating = isChecked;
            if (isChecked) {
                btn_set.setEnabled(false);
                btn_set.setBackgroundResource(R.drawable.btn_disabled);
            } else {
                btn_set.setEnabled(true);
                btn_set.setBackgroundResource(R.drawable.btn_accent);
            }
        });

        animate(0, 1024, 512);

        api = new RequestNetwork(this);

        try {
            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            if (settings.getString("enabled", null).equals("true")) {
                enable_leds();
            } else {
                disable_leds();
            }
        } catch (Exception e) {
            enable_leds();
        }
    }

    public void animate(int a, int b, int c) {
        if (a >= 1536) a = 0;
        if (b >= 1536) b = 0;
        if (c >= 1536) c = 0;
        final Handler handler = new Handler();
        int finalA = a;
        int finalB = b;
        int finalC = c;

        handler.postDelayed(() -> {
            animate(finalA + 4, finalB + 4, finalC + 4);
            String _r = Integer.toString(intToColor(finalA));
            String _g = Integer.toString(intToColor(finalB));
            String _b = Integer.toString(intToColor(finalC));
            debug.setText(_r.concat(" ").concat(_g).concat(" ").concat(_b).concat(" | ").concat(Integer.toString(finalA)));
            if (is_animating && animator && is_powered_on) {
                field_red.setText(_r);
                field_green.setText(_g);
                field_blue.setText(_b);
                api.startRequestNetwork(RequestNetworkController.GET, field_protocol.getText().toString().trim().concat("://").concat(field_hostname.getText().toString().trim()).concat(":").concat(field_port.getText().toString().trim()).concat(getCmd(field_cmd.getText().toString().trim(), null, null, null)), "A", api_listener);
                is_loading = true;
                String color = "FF".concat(String.format("%02X", Integer.parseInt(field_red.getText().toString().trim()))).concat(String.format("%02X", Integer.parseInt(field_green.getText().toString().trim()))).concat(String.format("%02X", Integer.parseInt(field_blue.getText().toString().trim())));
                long xc = Long.parseLong(color, 16);
                color_picker.setInitialColor((int) xc);
                SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("hostname", field_hostname.getText().toString().trim());
                editor.putString("port", field_port.getText().toString().trim());
                editor.putString("protocol", field_protocol.getText().toString().trim());
                editor.putString("cmd", field_cmd.getText().toString().trim());
                editor.putString("red", field_red.getText().toString().trim());
                editor.putString("green", field_green.getText().toString().trim());
                editor.putString("blue", field_blue.getText().toString().trim());
                editor.apply();
            }
        }, 50);
    }

    public int intToColor(int i) {
        if (i >= 0 && i <= 255) return i;
        if (i >= 256 && i <= 767) return 255;
        if (i >= 768 && i <= 1023) return 768 - Math.abs(255 - i);
        else return 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        animator = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        animator = true;
    }

    public void form_validator() {
        if (!is_animating) {
            if (error_protocol || error_hostname || error_port || error_cmd || error_red || error_green || error_blue) {
                btn_set.setBackgroundResource(R.drawable.btn_disabled);
                btn_set.setEnabled(false);
                form_error = true;
            } else {
                btn_set.setBackgroundResource(R.drawable.btn_accent);
                btn_set.setEnabled(true);
                form_error = false;
                String color = "FF".concat(String.format("%02X", Integer.parseInt(field_red.getText().toString().trim()))).concat(String.format("%02X", Integer.parseInt(field_green.getText().toString().trim()))).concat(String.format("%02X", Integer.parseInt(field_blue.getText().toString().trim())));

                long c = Long.parseLong(color, 16);
                color_picker.setInitialColor((int) c);
            }
        }
    }

    public void run(View v) {
        if (!form_error && is_powered_on) {
            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("hostname", field_hostname.getText().toString().trim());
            editor.putString("port", field_port.getText().toString().trim());
            editor.putString("protocol", field_protocol.getText().toString().trim());
            editor.putString("cmd", field_cmd.getText().toString().trim());
            editor.putString("red", field_red.getText().toString().trim());
            editor.putString("green", field_green.getText().toString().trim());
            editor.putString("blue", field_blue.getText().toString().trim());
            editor.apply();

            loading_screen.setVisibility(View.VISIBLE);
            btn_set.setEnabled(false);
            is_loading = true;

            api.startRequestNetwork(RequestNetworkController.GET, field_protocol.getText().toString().trim().concat("://").concat(field_hostname.getText().toString().trim()).concat(":").concat(field_port.getText().toString().trim()).concat(getCmd(field_cmd.getText().toString().trim(), null, null, null)), "A", api_listener);
            is_loading = true;
        }
    }

    public String getCmd(String cmd, @Nullable String _r, @Nullable String _g, @Nullable String _b) {
        if (_r == null || _g == null || _b == null) {
            String r = field_red.getText().toString().trim();
            String g = field_green.getText().toString().trim();
            String b = field_blue.getText().toString().trim();

            String r_out = cmd.replace("{_r}", r);
            String g_out = r_out.replace("{_g}", g);
            String b_out = g_out.replace("{_b}", b);

            return b_out;
        } else {
            String r_out = cmd.replace("{_r}", _r);
            String g_out = r_out.replace("{_g}", _g);
            String b_out = g_out.replace("{_b}", _b);

            return b_out;
        }
    }

    public void reset(View v) {
        unfocus(null);
        field_red.setText("0");
        field_green.setText("255");
        field_blue.setText("255");
        color_picker.setInitialColor(0xFF00FFFF);
    }

    public void toggle(View v) {
        if (is_powered_on) {
            disable_leds();

            if (!form_error) {
                api.startRequestNetwork(RequestNetworkController.GET, field_protocol.getText().toString().trim().concat("://").concat(field_hostname.getText().toString().trim()).concat(":").concat(field_port.getText().toString().trim()).concat(getCmd(field_cmd.getText().toString().trim(), "0", "0", "0")), "A", api_listener);
                is_loading = true;
            }

            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "false");
            editor.apply();
        } else {
            enable_leds();

            if (!form_error) {
                api.startRequestNetwork(RequestNetworkController.GET, field_protocol.getText().toString().trim().concat("://").concat(field_hostname.getText().toString().trim()).concat(":").concat(field_port.getText().toString().trim()).concat(getCmd(field_cmd.getText().toString().trim(), null, null, null)), "A", api_listener);
                is_loading = true;
            }

            SharedPreferences settings = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("enabled", "true");
            editor.apply();
        }
    }

    public void enable_leds() {
        is_powered_on = true;
        btn_power.setBackgroundTintList(AppCompatResources.getColorStateList(this, R.color.light_green));

        field_protocol.setEnabled(true);
        field_hostname.setEnabled(true);
        field_port.setEnabled(true);
        field_cmd.setEnabled(true);
        field_red.setEnabled(true);
        field_green.setEnabled(true);
        field_blue.setEnabled(true);

        btn_pick_color.setEnabled(true);
        btn_set.setEnabled(true);

        animation.setEnabled(true);

        color_picker.setEnabled(true);

        disabler.setVisibility(View.GONE);
    }

    public void disable_leds() {
        is_powered_on = false;
        btn_power.setBackgroundTintList(AppCompatResources.getColorStateList(this, R.color.light_red));

        field_protocol.setEnabled(false);
        field_hostname.setEnabled(false);
        field_port.setEnabled(false);
        field_cmd.setEnabled(false);
        field_red.setEnabled(false);
        field_green.setEnabled(false);
        field_blue.setEnabled(false);

        btn_pick_color.setEnabled(false);
        btn_set.setEnabled(false);

        animation.setEnabled(false);

        color_picker.setEnabled(false);

        disabler.setVisibility(View.VISIBLE);
    }

    public void unfocus(View v) {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void about(View v) {
        startActivity(new Intent(this, AboutActivity.class));
    }
}