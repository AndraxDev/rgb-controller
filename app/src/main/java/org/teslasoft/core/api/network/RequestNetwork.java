package org.teslasoft.core.api.network;

import android.app.Activity;

import java.util.HashMap;

@SuppressWarnings("unused")
public class RequestNetwork {
    private HashMap<String, Object> params = new HashMap<>();
    private HashMap<String, Object> headers = new HashMap<>();

    private final Activity activity;

    private int requestType = 0;

    public RequestNetwork(Activity activity) {
        this.activity = activity;
    }

    public void setHeaders(HashMap<String, Object> headers) {
        this.headers = headers;
    }

    public void setParams(HashMap<String, Object> params, int requestType) {
        this.params = params;
        this.requestType = requestType;
    }

    public HashMap<String, Object> getParams() {
        return params;
    }

    public HashMap<String, Object> getHeaders() {
        return headers;
    }

    public Activity getActivity() {
        return activity;
    }

    public int getRequestType() {
        return requestType;
    }

    public void startRequestNetwork(String method, String url, String tag, RequestListener requestListener) {
        org.teslasoft.core.api.network.RequestNetworkController.getInstance().execute(this, method, url, tag, requestListener);
    }

    public interface RequestListener {
        void onResponse(String tag, String response);
        void onErrorResponse(String tag, String message);
    }
}
