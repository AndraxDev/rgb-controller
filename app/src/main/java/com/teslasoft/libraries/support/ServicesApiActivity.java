package com.teslasoft.libraries.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.teslasoft.iot.rgbcontroller.R;
import com.teslasoft.iot.rgbcontroller.UpdateRequiredActivity;

public class ServicesApiActivity extends Activity {

    public PackageManager packageManager;
    public Context context;
    public int REQUEST_STATE = 0;
    public Boolean has_response = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_shadow);

        context = this;
        packageManager = context.getPackageManager();

        performApiRequest();
    }

    @Override
    protected void onPause() {
        super.onPause();
        has_response = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finishAndRemoveTask();
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void performApiRequest() {
        if (isPackageInstalled("com.teslasoft.libraries.support", packageManager)) {
            try {
                Intent api_intent = new Intent();
                api_intent.setComponent(new ComponentName("com.teslasoft.libraries.support", "com.teslasoft.libraries.support.ApiCallbackActivity"));
                startActivityForResult(api_intent, 1);
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if (!has_response) {
                        Intent updater_intent = new Intent(this, UpdateRequiredActivity.class);
                        updater_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivityForResult(updater_intent, 3);
                    }
                }, 500);
            } catch (Exception e) {
                toast(e.toString(), this);
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Teslasoft Core")
                        .setMessage("This app requires one or more Teslasoft services that are not currently available on your device.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAndRemoveTask())
                        .show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Teslasoft Core")
                    .setMessage("This app requires one or more Teslasoft services that are not currently available on your device.")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAndRemoveTask())
                    .show();
        }
    }

    public void startTeslasoftCorePlayIntent() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.teslasoft.libraries.support"));
        startActivity(browserIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        has_response = true;
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            this.setResult(Activity.RESULT_OK);
            if (REQUEST_STATE == 0) {
                REQUEST_STATE = 1;
                try {
                    Intent license_intent = new Intent();
                    license_intent.setComponent(new ComponentName("com.teslasoft.libraries.support", "com.teslasoft.jarvis.licence.PiracyCheckActivity"));
                    startActivityForResult(license_intent, 1);
                } catch (Exception e) {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Teslasoft Core")
                            .setMessage("This app requires one or more Teslasoft services that are not currently available on your device.")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAndRemoveTask())
                            .show();
                }
            } else if (REQUEST_STATE == 1) {
                final Handler handler = new Handler();
                handler.postDelayed(this::finish, 500);
            }
        } else if (resultCode == 3) {
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            if (REQUEST_STATE == 0) {
                this.setResult(Activity.RESULT_CANCELED);
                final Handler handler = new Handler();
                handler.postDelayed(() -> new AlertDialog.Builder(this)
                        .setTitle("Teslasoft Core")
                        .setMessage("This app requires one or more Teslasoft services that are not currently available on your device.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAndRemoveTask())
                        .show(), 50);
            } else if (REQUEST_STATE == 1) {
                this.setResult(Activity.RESULT_CANCELED);
                final Handler handler = new Handler();
                handler.postDelayed(() -> new AlertDialog.Builder(this)
                        .setTitle("Teslasoft Core")
                        .setMessage("Failed to perform security check. Using of Teslasoft Core may be unsafe.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAndRemoveTask())
                        .show(), 50);
            }
        }
    }

    public Handler mHandler = new Handler();
    public void toast(final CharSequence text, final Context context) {
        mHandler.post(() -> {
            try {
                Toast.makeText(context,text,Toast.LENGTH_SHORT).show();
            } catch(Exception ignored) {}
        });
    }
}
