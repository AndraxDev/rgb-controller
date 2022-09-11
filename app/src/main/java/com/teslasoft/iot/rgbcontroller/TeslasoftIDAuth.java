package com.teslasoft.iot.rgbcontroller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TeslasoftIDAuth extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_loading_screen);

        try {
            Intent api_intent = new Intent();
            api_intent.setComponent(new ComponentName("com.teslasoft.libraries.support", "org.teslasoft.core.api.account.AccountPickerActivity"));
            startActivityForResult(api_intent, 1);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            Intent intent = new Intent();
            intent.putExtra("account_id", data.getStringExtra("account_id"));
            intent.putExtra("signature", data.getStringExtra("signature"));
            this.setResult(resultCode, intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } catch (Exception e) {
            if (resultCode == 3 || resultCode == 4) {
                this.setResult(resultCode);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else {
                this.setResult(resultCode);

                new MaterialAlertDialogBuilder(this)
                        .setTitle("App sync")
                        .setMessage("To use your account with multiple apps you need Teslasoft core.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            this.setResult(RESULT_CANCELED);
                            finish();
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        })
                        .show();
            }
        }
    }
}
