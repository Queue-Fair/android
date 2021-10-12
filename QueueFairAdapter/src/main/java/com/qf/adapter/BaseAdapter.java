package com.qf.adapter;

import com.qf.adapter.android.JsonUrlToMap;
import com.qf.adapter.android.LoggerWrapper;

public class BaseAdapter {

    static UrlToMap urlToMap = new JsonUrlToMap();

    public static LoggerWrapper log = new LoggerWrapper();

    static boolean sendIPAddressToAdapter =  false;

    static boolean usesSecrets = false;

    public String getSettingsURL() {
        return QueueFairConfig.protocol + "://" + QueueFairConfig.filesServer + "/"
                + QueueFairConfig.account + "/queue-fair-settings.js";
    }
}